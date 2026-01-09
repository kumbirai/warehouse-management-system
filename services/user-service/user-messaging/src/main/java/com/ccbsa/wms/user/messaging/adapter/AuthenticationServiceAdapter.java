package com.ccbsa.wms.user.messaging.adapter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.common.keycloak.config.KeycloakConfig;
import com.ccbsa.common.keycloak.port.KeycloakClientPort;
import com.ccbsa.wms.user.application.service.command.dto.AuthenticationResult;
import com.ccbsa.wms.user.application.service.command.dto.LoginCommand;
import com.ccbsa.wms.user.application.service.command.dto.RefreshTokenCommand;
import com.ccbsa.wms.user.application.service.exception.AuthenticationException;
import com.ccbsa.wms.user.application.service.exception.KeycloakServiceException;
import com.ccbsa.wms.user.application.service.port.auth.AuthenticationServicePort;
import com.ccbsa.wms.user.application.service.query.dto.UserContextQuery;
import com.ccbsa.wms.user.application.service.query.dto.UserContextView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter: AuthenticationServiceAdapter
 * <p>
 * Implements AuthenticationServicePort for BFF authentication operations. Handles Keycloak token operations, masking IAM complexity.
 * <p>
 * Uses externalRestTemplate (non-load-balanced) for Keycloak calls since Keycloak is an external service
 * not registered with Eureka.
 */
@Component
@Slf4j
public class AuthenticationServiceAdapter implements AuthenticationServicePort {
    // Keycloak client configuration - should match Keycloak client setup
    private static final String CLIENT_ID = "wms-api";
    private static final String GRANT_TYPE_PASSWORD = "password";
    private static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    private final KeycloakConfig keycloakConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String clientSecret;
    private final KeycloakClientPort keycloakClientPort;

    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Configuration validation is intentionally performed before publishing the bean")
    public AuthenticationServiceAdapter(KeycloakConfig keycloakConfig, @Qualifier("externalRestTemplate") RestTemplate restTemplate, KeycloakClientPort keycloakClientPort) {
        KeycloakConfig validatedConfig = validateConfig(keycloakConfig);
        this.keycloakConfig = validatedConfig;
        this.clientSecret = validatedConfig.getClientSecret();
        this.restTemplate = Objects.requireNonNull(restTemplate, "RestTemplate must not be null");
        this.objectMapper = new ObjectMapper();
        this.keycloakClientPort = Objects.requireNonNull(keycloakClientPort, "KeycloakClientPort must not be null");
    }

    private static KeycloakConfig validateConfig(KeycloakConfig config) {
        KeycloakConfig validatedConfig = Objects.requireNonNull(config, "KeycloakConfig must not be null");
        if (validatedConfig.getServerUrl() == null || validatedConfig.getServerUrl().isEmpty()) {
            throw new IllegalStateException("Keycloak server URL is not configured");
        }
        if (validatedConfig.getDefaultRealm() == null || validatedConfig.getDefaultRealm().isEmpty()) {
            throw new IllegalStateException("Keycloak default realm is not configured");
        }
        return validatedConfig;
    }

    @Override
    @Retryable(retryFor = {RestClientException.class, KeycloakServiceException.class}, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2.0), noRetryFor = {
            AuthenticationException.class})
    public AuthenticationResult login(LoginCommand command) {
        log.debug("Attempting login for user: {}", command.getUsername());

        try {
            // Build token endpoint URL
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakConfig.getServerUrl(), keycloakConfig.getDefaultRealm());

            log.debug("Keycloak token URL: {}", tokenUrl);
            log.debug("Client ID: {}, Client Secret configured: {}", CLIENT_ID, (clientSecret != null && !clientSecret.isEmpty()));

            // Prepare request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", GRANT_TYPE_PASSWORD);
            body.add("client_id", CLIENT_ID);
            if (clientSecret != null && !clientSecret.isEmpty()) {
                body.add("client_secret", clientSecret);
            } else {
                log.warn("Client secret is not configured - this may cause authentication failures for confidential clients");
            }
            body.add("username", command.getUsername());
            body.add("password", command.getPassword());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            // Call Keycloak token endpoint
            ResponseEntity<String> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, entity, String.class);

            // Parse response
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            String accessToken = jsonResponse.get("access_token").asText();
            String refreshToken = jsonResponse.get("refresh_token").asText();
            String tokenType = jsonResponse.get("token_type").asText();
            int expiresIn = jsonResponse.get("expires_in").asInt();

            // Extract user context from access token
            AuthenticationResult.UserContext userContext = extractUserContext(accessToken);

            log.info("Login successful for user: {}", command.getUsername());

            return new AuthenticationResult(accessToken, refreshToken, tokenType, expiresIn, userContext);
        } catch (HttpClientErrorException e) {
            // Extract and log Keycloak error response
            String errorResponseBody = e.getResponseBodyAsString();
            String keycloakError = extractKeycloakError(errorResponseBody);

            if (e.getStatusCode().value() == 401) {
                log.warn("Login failed for user: {} - HTTP 401 Unauthorized. Keycloak error: {}", command.getUsername(), keycloakError);
                log.debug("Full Keycloak error response: {}", errorResponseBody);

                // Provide more specific error message based on Keycloak error
                String errorMessage = "Invalid username or password";
                if (keycloakError.contains("invalid_client")) {
                    errorMessage = "Authentication service configuration error. Please contact support.";
                    log.error("Keycloak client authentication failed - client secret may be misconfigured");
                } else if (keycloakError.contains("invalid_grant")) {
                    errorMessage = "Invalid username or password";
                }

                throw new AuthenticationException(errorMessage, e);
            } else if (e.getStatusCode().value() == 400) {
                log.warn("Login failed for user: {} - HTTP 400 Bad Request. Keycloak error: {}", command.getUsername(), keycloakError);
                log.debug("Full Keycloak error response: {}", errorResponseBody);

                // Provide user-friendly error messages for specific Keycloak 400 errors
                String errorMessage = "Invalid request parameters";

                // Check for account setup issues (email not verified, temporary password, etc.)
                // Use Locale.ROOT for locale-independent case conversion (production-grade)
                String lowerCaseError = keycloakError.toLowerCase(Locale.ROOT);
                if (lowerCaseError.contains("account is not fully set up") || lowerCaseError.contains("account is not fully setup")) {
                    errorMessage = "Your account is not fully set up. Please verify your email address or contact your administrator to complete account setup.";
                    log.info("User account not fully set up: {} - likely email not verified or temporary password", command.getUsername());
                } else if (keycloakError.contains("invalid_grant")) {
                    // Generic invalid_grant error (credentials incorrect)
                    errorMessage = "Invalid username or password";
                } else if (keycloakError.contains("User is disabled")) {
                    errorMessage = "Your account has been disabled. Please contact your administrator.";
                } else if (keycloakError.contains("Account temporarily disabled")) {
                    errorMessage = "Your account has been temporarily disabled due to too many failed login attempts. Please try again later or contact your administrator.";
                }

                throw new AuthenticationException(errorMessage, e);
            } else {
                log.error("Login failed for user: {} - HTTP {}: {}. Keycloak error: {}", command.getUsername(), e.getStatusCode().value(), e.getMessage(), keycloakError);
                log.debug("Full Keycloak error response: {}", errorResponseBody);
                throw new KeycloakServiceException(String.format("Authentication service error: %s", keycloakError), e);
            }
        } catch (RestClientException e) {
            log.error("Network error during login for user: {}", command.getUsername(), e);
            throw new KeycloakServiceException("Unable to connect to authentication service", e);
        } catch (JsonProcessingException e) {
            log.error("Unexpected error during login for user: {}", command.getUsername(), e);
            throw new KeycloakServiceException("Authentication failed", e);
        }
    }

    /**
     * Extracts user context from JWT token by decoding it.
     *
     * @param accessToken JWT access token
     * @return User context
     * @throws IllegalArgumentException if token is invalid or malformed
     */
    private AuthenticationResult.UserContext extractUserContext(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalArgumentException("Access token cannot be null or empty");
        }

        try {
            // Decode JWT token (base64 decode payload)
            String[] parts = accessToken.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException(String.format("Invalid JWT token format: expected 3 parts, got %d", parts.length));
            }

            // Decode payload (second part) with proper padding
            String payload = parts[1];
            // Add padding if needed for Base64 decoding
            int padding = 4 - (payload.length() % 4);
            if (padding != 4) {
                payload = String.format("%s%s", payload, "=".repeat(padding));
            }

            String decodedPayload = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            JsonNode claims = objectMapper.readTree(decodedPayload);

            // Extract user information
            String userIdString = claims.has("sub") ? claims.get("sub").asText() : null;
            String username = claims.has("preferred_username") ? claims.get("preferred_username").asText() : (claims.has("username") ? claims.get("username").asText() : null);
            String email = claims.has("email") ? claims.get("email").asText() : null;
            String firstName = claims.has("given_name") ? claims.get("given_name").asText() : null;
            String lastName = claims.has("family_name") ? claims.get("family_name").asText() : null;
            String tenantIdString = claims.has("tenant_id") ? claims.get("tenant_id").asText() : null;

            // Validate required fields
            if (userIdString == null || userIdString.isEmpty()) {
                throw new IllegalArgumentException("JWT token missing required 'sub' claim (user ID)");
            }

            // Extract roles first to check for SYSTEM_ADMIN
            List<String> roles = new ArrayList<>();
            if (claims.has("realm_access")) {
                JsonNode realmAccess = claims.get("realm_access");
                if (realmAccess.has("roles")) {
                    realmAccess.get("roles").forEach(role -> roles.add(role.asText()));
                }
            }

            // SYSTEM_ADMIN users don't require tenant_id
            boolean isSystemAdmin = roles.contains("SYSTEM_ADMIN");
            if (!isSystemAdmin && (tenantIdString == null || tenantIdString.isEmpty())) {
                throw new IllegalArgumentException("JWT token missing required 'tenant_id' claim");
            }

            // Create value objects from JWT claims
            UserId userId = UserId.of(userIdString);
            TenantId tenantId = (tenantIdString != null && !tenantIdString.isEmpty()) ? TenantId.of(tenantIdString) : null;

            return new AuthenticationResult.UserContext(userId, username, tenantId, roles, email, firstName, lastName);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to extract user context: {}", e.getMessage());
            throw e;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse user context from token", e);
            throw new IllegalArgumentException("Failed to extract user context", e);
        }
    }

    /**
     * Extracts error message from Keycloak error response.
     *
     * @param errorResponseBody Error response body from Keycloak
     * @return Error message or default message if parsing fails
     */
    private String extractKeycloakError(String errorResponseBody) {
        if (errorResponseBody == null || errorResponseBody.isEmpty()) {
            return "No error details available";
        }

        try {
            JsonNode errorNode = objectMapper.readTree(errorResponseBody);
            StringBuilder errorMsg = new StringBuilder();

            if (errorNode.has("error")) {
                errorMsg.append("error: ").append(errorNode.get("error").asText());
            }
            if (errorNode.has("error_description")) {
                if (errorMsg.length() > 0) {
                    errorMsg.append(", ");
                }
                errorMsg.append("description: ").append(errorNode.get("error_description").asText());
            }

            return errorMsg.length() > 0 ? errorMsg.toString() : errorResponseBody;
        } catch (JsonProcessingException e) {
            log.debug("Failed to parse Keycloak error response: {}", e.getMessage());
            return errorResponseBody;
        }
    }

    @Override
    @Retryable(retryFor = {RestClientException.class, KeycloakServiceException.class}, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2.0), noRetryFor = {
            AuthenticationException.class})
    public AuthenticationResult refreshToken(RefreshTokenCommand command) {
        log.debug("Refreshing token");

        try {
            // Build token endpoint URL
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakConfig.getServerUrl(), keycloakConfig.getDefaultRealm());

            // Prepare request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", GRANT_TYPE_REFRESH_TOKEN);
            body.add("client_id", CLIENT_ID);
            if (clientSecret != null && !clientSecret.isEmpty()) {
                body.add("client_secret", clientSecret);
            }
            body.add("refresh_token", command.getRefreshToken());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            // Call Keycloak token endpoint
            ResponseEntity<String> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, entity, String.class);

            // Parse response
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            String accessToken = jsonResponse.get("access_token").asText();
            String refreshToken =
                    jsonResponse.has("refresh_token") ? jsonResponse.get("refresh_token").asText() : command.getRefreshToken(); // Keycloak may not return new refresh token
            String tokenType = jsonResponse.get("token_type").asText();
            int expiresIn = jsonResponse.get("expires_in").asInt();

            // Extract user context from access token
            AuthenticationResult.UserContext userContext = extractUserContext(accessToken);

            log.debug("Token refresh successful");

            return new AuthenticationResult(accessToken, refreshToken, tokenType, expiresIn, userContext);
        } catch (HttpClientErrorException e) {
            // Extract and log Keycloak error response
            String errorResponseBody = e.getResponseBodyAsString();
            String keycloakError = extractKeycloakError(errorResponseBody);

            if (e.getStatusCode().value() == 401) {
                log.warn("Token refresh failed - HTTP 401 Unauthorized. Keycloak error: {}", keycloakError);
                log.debug("Full Keycloak error response: {}", errorResponseBody);
                throw new AuthenticationException("Invalid or expired refresh token", e);
            } else if (e.getStatusCode().value() == 400) {
                log.warn("Token refresh failed - HTTP 400 Bad Request. Keycloak error: {}", keycloakError);
                log.debug("Full Keycloak error response: {}", errorResponseBody);
                throw new AuthenticationException(String.format("Invalid request parameters: %s", keycloakError), e);
            } else {
                log.error("Token refresh failed - HTTP {}: {}. Keycloak error: {}", e.getStatusCode().value(), e.getMessage(), keycloakError);
                log.debug("Full Keycloak error response: {}", errorResponseBody);
                throw new KeycloakServiceException(String.format("Token refresh service error: %s", keycloakError), e);
            }
        } catch (RestClientException e) {
            log.error("Network error during token refresh", e);
            throw new KeycloakServiceException("Unable to connect to authentication service", e);
        } catch (JsonProcessingException e) {
            log.error("Unexpected error during token refresh", e);
            throw new KeycloakServiceException("Token refresh failed", e);
        }
    }

    @Override
    public UserContextView getUserContext(UserContextQuery query) {
        AuthenticationResult.UserContext context = extractUserContext(query.getAccessToken());
        return new UserContextView(context.getUserId(), context.getUsername(), context.getTenantId(), context.getRoles(), context.getEmail(), context.getFirstName(),
                context.getLastName());
    }

    @Override
    public com.ccbsa.wms.user.domain.core.valueobject.KeycloakUserId createUser(String tenantId, String username, String email, String password, String firstName,
                                                                                String lastName) {
        log.info("Creating user in Keycloak: username={}, tenantId={}", username, tenantId);

        try {
            log.debug("Getting Keycloak Admin Client and realm: {}", keycloakConfig.getDefaultRealm());
            RealmResource realm = keycloakClientPort.getAdminClient().realm(keycloakConfig.getDefaultRealm());
            log.debug("Realm resource obtained successfully");

            // Create user representation
            UserRepresentation user = new UserRepresentation();
            user.setUsername(username);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEnabled(true);
            user.setEmailVerified(false);

            // Set tenant_id attribute (multi-tenancy enforcement)
            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("tenant_id", Collections.singletonList(tenantId));
            user.setAttributes(attributes);

            // Set initial password (temporary, requires change on first login)
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(true);
            user.setCredentials(Collections.singletonList(credential));

            // Create user
            log.debug("Creating user representation in Keycloak");
            UsersResource usersResource = realm.users();
            log.debug("Calling Keycloak API to create user");
            try (Response response = usersResource.create(user)) {
                log.debug("Keycloak user creation response received: status={}", response.getStatus());
                int statusCode = response.getStatus();
                if (statusCode == Response.Status.CONFLICT.getStatusCode()) {
                    // HTTP 409 Conflict - duplicate username or email
                    String errorMsg = String.format("User already exists with username '%s' or email '%s'", username, email);
                    log.warn(errorMsg);
                    throw new KeycloakServiceException(errorMsg);
                }
                if (statusCode != Response.Status.CREATED.getStatusCode()) {
                    String errorMsg = String.format("Failed to create user in Keycloak: HTTP %d", statusCode);
                    log.error(errorMsg);
                    throw new KeycloakServiceException(errorMsg);
                }

                // Extract user ID from location header
                String locationHeader = response.getLocation().toString();
                String keycloakUserId = locationHeader.substring(locationHeader.lastIndexOf('/') + 1);

                // Assign user to tenant group (non-blocking - group assignment is optional)
                // If group doesn't exist or assignment fails, log warning but don't fail user creation
                log.debug("Assigning user to tenant group: userId={}, tenantId={}", keycloakUserId, tenantId);
                try {
                    assignUserToTenantGroup(realm, keycloakUserId, tenantId);
                    log.debug("User assigned to tenant group successfully");
                } catch (KeycloakServiceException e) {
                    // Group assignment failed - log warning but don't fail user creation
                    // Groups are used for organization and don't affect core user functionality
                    log.warn("Failed to assign user to tenant group (user creation will continue): userId={}, tenantId={}, error={}", keycloakUserId, tenantId, e.getMessage());
                    log.debug("Group assignment failure details", e);
                }

                log.info("User created successfully in Keycloak: userId={}, username={}", keycloakUserId, username);
                return com.ccbsa.wms.user.domain.core.valueobject.KeycloakUserId.of(keycloakUserId);
            }
        } catch (KeycloakServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create user in Keycloak: {}", e.getMessage(), e);
            throw new KeycloakServiceException(String.format("Failed to create user in Keycloak: %s", e.getMessage()), e);
        }
    }

    /**
     * Assigns a user to their tenant group in Keycloak.
     * <p>
     * Uses efficient group lookup via getGroupByPath first, falling back to groups().groups() only if needed.
     * This avoids fetching all groups unnecessarily, which can cause timeouts.
     *
     * @param realm          Realm resource
     * @param keycloakUserId Keycloak user ID
     * @param tenantId       Tenant ID
     * @throws KeycloakServiceException if group not found or assignment fails
     */
    private void assignUserToTenantGroup(RealmResource realm, String keycloakUserId, String tenantId) {
        String groupName = String.format("tenant-%s", tenantId);
        log.debug("Assigning user to tenant group: userId={}, group={}", keycloakUserId, groupName);

        try {
            // Find tenant group using efficient path lookup first
            GroupRepresentation tenantGroup = null;
            try {
                // Try efficient path-based lookup first (doesn't fetch all groups)
                tenantGroup = realm.getGroupByPath(String.format("/%s", groupName));
                if (tenantGroup != null) {
                    log.debug("Found tenant group via path lookup: {}", groupName);
                }
            } catch (NotFoundException e) {
                log.debug("Group {} not found via path lookup, falling back to groups list", groupName);
            } catch (jakarta.ws.rs.ProcessingException e) {
                // Handle timeout or connection errors
                if (e.getCause() instanceof java.net.SocketTimeoutException) {
                    String errorMsg = String.format("Timeout while fetching group by path from Keycloak: %s", e.getMessage());
                    log.error(errorMsg);
                    throw new KeycloakServiceException(errorMsg, e);
                }
                String errorMsg = String.format("Connection error while fetching group by path from Keycloak: %s", e.getMessage());
                log.error(errorMsg);
                throw new KeycloakServiceException(errorMsg, e);
            } catch (Exception e) {
                log.debug("Path lookup failed for group {}, will try groups list: {}", groupName, e.getMessage());
            }

            // Fallback to fetching all groups if path lookup didn't work
            if (tenantGroup == null) {
                try {
                    List<GroupRepresentation> groups = realm.groups().groups();
                    tenantGroup = groups.stream().filter(group -> groupName.equalsIgnoreCase(group.getName())).findFirst().orElse(null);
                } catch (jakarta.ws.rs.ProcessingException e) {
                    // Handle timeout or connection errors
                    if (e.getCause() instanceof java.net.SocketTimeoutException) {
                        String errorMsg = String.format("Timeout while fetching groups from Keycloak: %s", e.getMessage());
                        log.error(errorMsg);
                        throw new KeycloakServiceException(errorMsg, e);
                    }
                    String errorMsg = String.format("Connection error while fetching groups from Keycloak: %s", e.getMessage());
                    log.error(errorMsg);
                    throw new KeycloakServiceException(errorMsg, e);
                } catch (Exception e) {
                    log.error("Failed to fetch groups from Keycloak: {}", e.getMessage(), e);
                    throw new KeycloakServiceException(String.format("Failed to fetch groups from Keycloak: %s", e.getMessage()), e);
                }
            }

            if (tenantGroup == null) {
                String errorMsg = String.format("Tenant group not found: %s", groupName);
                log.error(errorMsg);
                throw new KeycloakServiceException(errorMsg);
            }

            // Assign user to group with timeout handling
            try {
                UserResource userResource = realm.users().get(keycloakUserId);
                userResource.joinGroup(tenantGroup.getId());
            } catch (jakarta.ws.rs.ProcessingException e) {
                // Handle timeout or connection errors
                if (e.getCause() instanceof java.net.SocketTimeoutException) {
                    String errorMsg = String.format("Timeout while assigning user to group in Keycloak: %s", e.getMessage());
                    log.error(errorMsg);
                    throw new KeycloakServiceException(errorMsg, e);
                }
                String errorMsg = String.format("Connection error while assigning user to group in Keycloak: %s", e.getMessage());
                log.error(errorMsg);
                throw new KeycloakServiceException(errorMsg, e);
            }

            log.debug("User assigned to tenant group successfully: userId={}, group={}", keycloakUserId, groupName);
        } catch (KeycloakServiceException e) {
            throw e;
        }
    }

    @Override
    public void updateUser(com.ccbsa.wms.user.domain.core.valueobject.KeycloakUserId keycloakUserId, String email, String firstName, String lastName) {
        log.debug("Updating user in Keycloak: userId={}", keycloakUserId.getValue());

        try {
            RealmResource realm = keycloakClientPort.getAdminClient().realm(keycloakConfig.getDefaultRealm());

            UserResource userResource = realm.users().get(keycloakUserId.getValue());
            UserRepresentation user = userResource.toRepresentation();

            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);

            userResource.update(user);
            log.debug("User updated successfully in Keycloak: userId={}", keycloakUserId.getValue());
        } catch (Exception e) {
            log.error("Failed to update user in Keycloak: {}", e.getMessage(), e);
            throw new KeycloakServiceException(String.format("Failed to update user in Keycloak: %s", e.getMessage()), e);
        }
    }

    @Override
    public void enableUser(com.ccbsa.wms.user.domain.core.valueobject.KeycloakUserId keycloakUserId) {
        log.debug("Enabling user in Keycloak: userId={}", keycloakUserId.getValue());

        try {
            RealmResource realm = keycloakClientPort.getAdminClient().realm(keycloakConfig.getDefaultRealm());

            UserResource userResource = realm.users().get(keycloakUserId.getValue());
            UserRepresentation user = userResource.toRepresentation();
            user.setEnabled(true);
            userResource.update(user);

            log.debug("User enabled successfully in Keycloak: userId={}", keycloakUserId.getValue());
        } catch (Exception e) {
            log.error("Failed to enable user in Keycloak: {}", e.getMessage(), e);
            throw new KeycloakServiceException(String.format("Failed to enable user in Keycloak: %s", e.getMessage()), e);
        }
    }

    @Override
    public void disableUser(com.ccbsa.wms.user.domain.core.valueobject.KeycloakUserId keycloakUserId) {
        log.debug("Disabling user in Keycloak: userId={}", keycloakUserId.getValue());

        try {
            RealmResource realm = keycloakClientPort.getAdminClient().realm(keycloakConfig.getDefaultRealm());

            UserResource userResource = realm.users().get(keycloakUserId.getValue());
            UserRepresentation user = userResource.toRepresentation();
            user.setEnabled(false);
            userResource.update(user);

            log.debug("User disabled successfully in Keycloak: userId={}", keycloakUserId.getValue());
        } catch (Exception e) {
            log.error("Failed to disable user in Keycloak: {}", e.getMessage(), e);
            throw new KeycloakServiceException(String.format("Failed to disable user in Keycloak: %s", e.getMessage()), e);
        }
    }

    @Override
    public void assignRole(com.ccbsa.wms.user.domain.core.valueobject.KeycloakUserId keycloakUserId, String roleName) {
        log.debug("Assigning role to user in Keycloak: userId={}, role={}", keycloakUserId.getValue(), roleName);

        try {
            RealmResource realm = keycloakClientPort.getAdminClient().realm(keycloakConfig.getDefaultRealm());

            // Get realm role
            RolesResource rolesResource = realm.roles();
            RoleResource roleResource = rolesResource.get(roleName);
            RoleRepresentation role = roleResource.toRepresentation();

            // Assign role to user
            UserResource userResource = realm.users().get(keycloakUserId.getValue());
            userResource.roles().realmLevel().add(Collections.singletonList(role));

            log.debug("Role assigned successfully: userId={}, role={}", keycloakUserId.getValue(), roleName);
        } catch (Exception e) {
            log.error("Failed to assign role in Keycloak: {}", e.getMessage(), e);
            throw new KeycloakServiceException(String.format("Failed to assign role in Keycloak: %s", e.getMessage()), e);
        }
    }

    @Override
    public void removeRole(com.ccbsa.wms.user.domain.core.valueobject.KeycloakUserId keycloakUserId, String roleName) {
        log.debug("Removing role from user in Keycloak: userId={}, role={}", keycloakUserId.getValue(), roleName);

        try {
            RealmResource realm = keycloakClientPort.getAdminClient().realm(keycloakConfig.getDefaultRealm());

            // Get realm role
            RolesResource rolesResource = realm.roles();
            RoleResource roleResource = rolesResource.get(roleName);
            RoleRepresentation role = roleResource.toRepresentation();

            // Remove role from user
            UserResource userResource = realm.users().get(keycloakUserId.getValue());
            userResource.roles().realmLevel().remove(Collections.singletonList(role));

            log.debug("Role removed successfully: userId={}, role={}", keycloakUserId.getValue(), roleName);
        } catch (Exception e) {
            log.error("Failed to remove role in Keycloak: {}", e.getMessage(), e);
            throw new KeycloakServiceException(String.format("Failed to remove role in Keycloak: %s", e.getMessage()), e);
        }
    }

    @Override
    public List<String> getUserRoles(com.ccbsa.wms.user.domain.core.valueobject.KeycloakUserId keycloakUserId) {
        log.debug("Getting user roles from Keycloak: userId={}", keycloakUserId.getValue());

        try {
            RealmResource realm = keycloakClientPort.getAdminClient().realm(keycloakConfig.getDefaultRealm());

            UserResource userResource = realm.users().get(keycloakUserId.getValue());
            List<RoleRepresentation> roles = userResource.roles().realmLevel().listEffective();

            return roles.stream().map(RoleRepresentation::getName).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get user roles from Keycloak: {}", e.getMessage(), e);
            throw new KeycloakServiceException(String.format("Failed to get user roles from Keycloak: %s", e.getMessage()), e);
        }
    }

    @Override
    public void sendEmailVerificationAndPasswordReset(com.ccbsa.wms.user.domain.core.valueobject.KeycloakUserId keycloakUserId, String redirectUri) {
        log.debug("Sending email verification and password reset email: userId={}, redirectUri={}", keycloakUserId.getValue(), redirectUri);

        try {
            RealmResource realm = keycloakClientPort.getAdminClient().realm(keycloakConfig.getDefaultRealm());

            UserResource userResource = realm.users().get(keycloakUserId.getValue());

            // Prepare actions: VERIFY_EMAIL and UPDATE_PASSWORD
            List<String> actions = new ArrayList<>();
            actions.add("VERIFY_EMAIL");
            actions.add("UPDATE_PASSWORD");

            // Execute actions email with client ID and redirect URI
            // Keycloak will send an email with links for both actions
            // Method signature: executeActionsEmail(String clientId, String redirectUri, List<String> actions)
            // If redirectUri is provided, it will be used for the action links
            if (redirectUri != null && !redirectUri.trim().isEmpty()) {
                userResource.executeActionsEmail(CLIENT_ID, redirectUri, actions);
            } else {
                // Use default Keycloak redirect (login page)
                // Method signature: executeActionsEmail(List<String> actions)
                userResource.executeActionsEmail(actions);
            }

            log.info("Email verification and password reset email sent successfully: userId={}", keycloakUserId.getValue());
        } catch (Exception e) {
            log.error("Failed to send email verification and password reset email: userId={}, error={}", keycloakUserId.getValue(), e.getMessage(), e);
            throw new KeycloakServiceException(String.format("Failed to send email verification and password reset email: %s", e.getMessage()), e);
        }
    }
}

