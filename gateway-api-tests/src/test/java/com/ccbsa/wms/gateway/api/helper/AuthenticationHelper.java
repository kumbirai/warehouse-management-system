package com.ccbsa.wms.gateway.api.helper;

import java.util.Base64;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.LoginRequest;
import com.ccbsa.wms.gateway.api.dto.LoginResponse;
import com.ccbsa.wms.gateway.api.dto.UserContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Helper class for authentication operations in tests.
 * Simulates the frontend app's authentication flow.
 */
@Slf4j
public final class AuthenticationHelper {

    private static final String LOGIN_ENDPOINT = "/bff/auth/login";
    private static final String REFRESH_ENDPOINT = "/bff/auth/refresh";
    private static final String ME_ENDPOINT = "/bff/auth/me";
    private static final String LOGOUT_ENDPOINT = "/bff/auth/logout";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final WebTestClient webTestClient;
    private final ObjectMapper objectMapper;

    public AuthenticationHelper(WebTestClient webTestClient, ObjectMapper objectMapper) {
        this.webTestClient = webTestClient;
        // Configure ObjectMapper to ignore unknown properties (like "success" field in API responses)
        // Create a new ObjectMapper instance to avoid modifying the original
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Performs login and extracts access token and refresh token cookie.
     * Simulates the frontend app's login flow.
     *
     * @param username Username
     * @param password Password
     * @return AuthenticationResult containing tokens and user context
     */
    public AuthenticationResult login(String username, String password) {
        log.debug("Attempting login for user: {}", username);

        LoginRequest loginRequest = LoginRequest.builder()
                .username(username)
                .password(password)
                .build();

        EntityExchangeResult<byte[]> result = webTestClient
                .post()
                .uri(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(loginRequest))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .returnResult();

        // Extract response body
        ApiResponse<LoginResponse> apiResponse;
        try {
            apiResponse = objectMapper.readValue(
                    result.getResponseBody(),
                    objectMapper.getTypeFactory().constructParametricType(
                            ApiResponse.class,
                            LoginResponse.class
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse login response", e);
        }

        if (apiResponse.isError() || apiResponse.getData() == null) {
            String errorMessage = apiResponse.getError() != null
                    ? apiResponse.getError().getMessage()
                    : "No data in response";
            throw new RuntimeException(String.format("Login failed: %s", errorMessage));
        }

        LoginResponse loginResponse = apiResponse.getData();
        String accessToken = loginResponse.getAccessToken();
        UserContext userContext = loginResponse.getUserContext();

        // Extract refresh token from Set-Cookie header
        String refreshTokenCookie = extractRefreshTokenCookie(result.getResponseHeaders());

        log.debug("Login successful for user: {}, userId: {}", username, userContext != null ? userContext.getUserId() : "unknown");

        return AuthenticationResult.builder()
                .accessToken(accessToken)
                .refreshTokenCookie(refreshTokenCookie)
                .userContext(userContext)
                .build();
    }

    /**
     * Extracts the refresh token cookie value from response headers.
     *
     * <p>The cookie format from ResponseCookie.toString() is:
     * "refreshToken=value; Path=/api/v1/bff/auth; HttpOnly; Secure; SameSite=Strict; Max-Age=86400"
     *
     * @param headers Response headers
     * @return Refresh token cookie value, or null if not found
     */
    private String extractRefreshTokenCookie(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }

        // WebTestClient may store cookies in Set-Cookie header or in a cookies map
        // Try Set-Cookie header first (standard HTTP header)
        String setCookieHeader = headers.getFirst(HttpHeaders.SET_COOKIE);
        if (setCookieHeader == null) {
            // Try getting all Set-Cookie headers (there might be multiple)
            var allCookies = headers.get(HttpHeaders.SET_COOKIE);
            if (allCookies != null && !allCookies.isEmpty()) {
                // Find the refreshToken cookie
                for (String cookie : allCookies) {
                    if (cookie.startsWith(String.format("%s=", REFRESH_TOKEN_COOKIE_NAME))) {
                        setCookieHeader = cookie;
                        break;
                    }
                }
            }
        }

        if (setCookieHeader == null) {
            log.warn("No Set-Cookie header found in response");
            return null;
        }

        // Extract cookie value (format: "refreshToken=value; Path=...; HttpOnly; ...")
        String cookiePrefix = String.format("%s=", REFRESH_TOKEN_COOKIE_NAME);
        if (setCookieHeader.startsWith(cookiePrefix)) {
            String cookieValue = setCookieHeader.substring(REFRESH_TOKEN_COOKIE_NAME.length() + 1);
            // Remove any attributes after semicolon (Path, HttpOnly, Secure, SameSite, Max-Age)
            int semicolonIndex = cookieValue.indexOf(';');
            if (semicolonIndex > 0) {
                cookieValue = cookieValue.substring(0, semicolonIndex);
            }
            return cookieValue.trim();
        }

        log.warn("Refresh token cookie not found in Set-Cookie header: {}", setCookieHeader);
        return null;
    }

    /**
     * Refreshes the access token using the refresh token cookie.
     *
     * @param refreshTokenCookie The refresh token cookie value
     * @return New AuthenticationResult with refreshed tokens
     */
    public AuthenticationResult refreshToken(String refreshTokenCookie) {
        log.debug("Attempting token refresh");

        EntityExchangeResult<byte[]> result = webTestClient
                .post()
                .uri(REFRESH_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(REFRESH_TOKEN_COOKIE_NAME, refreshTokenCookie)
                .body(BodyInserters.fromValue("{}"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .returnResult();

        // Extract response body
        ApiResponse<LoginResponse> apiResponse;
        try {
            apiResponse = objectMapper.readValue(
                    result.getResponseBody(),
                    objectMapper.getTypeFactory().constructParametricType(
                            ApiResponse.class,
                            LoginResponse.class
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse refresh response", e);
        }

        if (apiResponse.isError() || apiResponse.getData() == null) {
            String errorMessage = apiResponse.getError() != null
                    ? apiResponse.getError().getMessage()
                    : "No data in response";
            throw new RuntimeException(String.format("Token refresh failed: %s", errorMessage));
        }

        LoginResponse loginResponse = apiResponse.getData();
        String accessToken = loginResponse.getAccessToken();
        UserContext userContext = loginResponse.getUserContext();

        // Extract new refresh token from Set-Cookie header
        String newRefreshTokenCookie = extractRefreshTokenCookie(result.getResponseHeaders());

        log.debug("Token refresh successful");

        return AuthenticationResult.builder()
                .accessToken(accessToken)
                .refreshTokenCookie(newRefreshTokenCookie != null ? newRefreshTokenCookie : refreshTokenCookie)
                .userContext(userContext)
                .build();
    }

    /**
     * Gets the current user context using the access token.
     *
     * @param accessToken The access token
     * @return UserContext of the authenticated user
     */
    public UserContext getCurrentUser(String accessToken) {
        log.debug("Getting current user context");

        EntityExchangeResult<byte[]> result = webTestClient
                .get()
                .uri(ME_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .returnResult();

        ApiResponse<UserContext> apiResponse;
        try {
            apiResponse = objectMapper.readValue(
                    result.getResponseBody(),
                    objectMapper.getTypeFactory().constructParametricType(
                            ApiResponse.class,
                            UserContext.class
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse user context response", e);
        }

        if (apiResponse.isError() || apiResponse.getData() == null) {
            String errorMessage = apiResponse.getError() != null
                    ? apiResponse.getError().getMessage()
                    : "No data in response";
            throw new RuntimeException(String.format("Get current user failed: %s", errorMessage));
        }

        return apiResponse.getData();
    }

    /**
     * Performs logout.
     *
     * @param accessToken        The access token
     * @param refreshTokenCookie The refresh token cookie
     */
    public void logout(String accessToken, String refreshTokenCookie) {
        log.debug("Performing logout");

        // Extract tenant ID from JWT token for X-Tenant-Id header
        // The user-service requires this header for authenticated requests
        String tenantId = extractTenantIdFromToken(accessToken);
        boolean isSystemAdmin = isSystemAdminRole(accessToken);

        // Build request with tenant ID header (unless SYSTEM_ADMIN who doesn't need it)
        var requestSpec = webTestClient
                .post()
                .uri(LOGOUT_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                .cookie(REFRESH_TOKEN_COOKIE_NAME, refreshTokenCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue("{}"));

        // Add X-Tenant-Id header if tenant ID is present and user is not SYSTEM_ADMIN
        if (tenantId != null && !tenantId.isEmpty() && !isSystemAdmin) {
            requestSpec = requestSpec.header("X-Tenant-Id", tenantId);
        }

        // Logout returns 204 No Content (no response body)
        requestSpec
                .exchange()
                .expectStatus().isNoContent();

        log.debug("Logout successful");
    }

    /**
     * Extracts tenant ID from JWT token by decoding the payload.
     *
     * @param token JWT access token
     * @return Tenant ID if present, null otherwise
     */
    private String extractTenantIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("Invalid JWT token format: expected 3 parts, got {}", parts.length);
                return null;
            }

            // Decode payload (second part) with proper padding
            String payload = parts[1];
            int padding = 4 - (payload.length() % 4);
            if (padding != 4) {
                payload = payload + "=".repeat(padding);
            }

            String decodedPayload = new String(Base64.getUrlDecoder().decode(payload), java.nio.charset.StandardCharsets.UTF_8);
            JsonNode claims = objectMapper.readTree(decodedPayload);

            if (claims.has("tenant_id")) {
                return claims.get("tenant_id").asText();
            }

            return null;
        } catch (Exception e) {
            log.warn("Failed to extract tenant ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Checks if the JWT token contains the SYSTEM_ADMIN role.
     *
     * @param token JWT access token
     * @return true if the token contains SYSTEM_ADMIN role, false otherwise
     */
    private boolean isSystemAdminRole(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            // Decode payload (second part) with proper padding
            String payload = parts[1];
            int padding = 4 - (payload.length() % 4);
            if (padding != 4) {
                payload = payload + "=".repeat(padding);
            }

            String decodedPayload = new String(Base64.getUrlDecoder().decode(payload), java.nio.charset.StandardCharsets.UTF_8);
            JsonNode claims = objectMapper.readTree(decodedPayload);

            // Check for SYSTEM_ADMIN role in realm_access.roles
            if (claims.has("realm_access")) {
                JsonNode realmAccess = claims.get("realm_access");
                if (realmAccess.has("roles")) {
                    JsonNode roles = realmAccess.get("roles");
                    if (roles.isArray()) {
                        for (JsonNode role : roles) {
                            if ("SYSTEM_ADMIN".equals(role.asText())) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        } catch (Exception e) {
            log.warn("Failed to check SYSTEM_ADMIN role from token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts tenant ID from JWT token (public method for use in tests).
     *
     * @param accessToken The access token
     * @return Tenant ID if present, null otherwise
     */
    public String getTenantIdFromToken(String accessToken) {
        return extractTenantIdFromToken(accessToken);
    }

    /**
     * Checks if the JWT token contains the SYSTEM_ADMIN role (public method for use in tests).
     *
     * @param accessToken The access token
     * @return true if the token contains SYSTEM_ADMIN role, false otherwise
     */
    public boolean isSystemAdminUser(String accessToken) {
        return isSystemAdminRole(accessToken);
    }
}

