package com.ccbsa.wms.common.security;

import java.time.Instant;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Service Account Token Provider
 * <p>
 * Production-grade implementation for obtaining and caching service account JWT tokens
 * using OAuth2 client_credentials grant flow for service-to-service authentication.
 * <p>
 * This provider:
 * - Obtains tokens from Keycloak using client_credentials grant
 * - Caches tokens with automatic refresh before expiry
 * - Thread-safe token management
 * - Handles token refresh failures gracefully
 * - Supports event-driven calls without HTTP request context
 * <p>
 * Usage:
 * <pre>
 * String token = serviceAccountTokenProvider.getAccessToken();
 * headers.set("Authorization", "Bearer " + token);
 * </pre>
 *
 * @see ServiceAccountAuthenticationInterceptor
 */
@Component
@Slf4j
@SuppressFBWarnings(value = {"CT_CONSTRUCTOR_THROW", "UPM_UNCALLED_PRIVATE_METHOD", "UUF_UNUSED_FIELD"}, justification =
        "CT_CONSTRUCTOR_THROW: Constructor is safe for Spring-managed components. "
                + "UPM_UNCALLED_PRIVATE_METHOD: Private method requestTokenFromKeycloak() is called from getAccessToken(). "
                + "UUF_UNUSED_FIELD: All fields are used - SpotBugs cannot detect usage of volatile fields and constructor parameters.")
public class ServiceAccountTokenProvider {
    // Configuration for token refresh buffer (refresh 60 seconds before expiry)
    private static final long TOKEN_REFRESH_BUFFER_SECONDS = 60;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "RestTemplate is a Spring-managed bean that should not be mutated externally. "
            + "Constructor injection ensures proper lifecycle management by Spring container.")
    private final RestTemplate restTemplate;
    private final String tokenEndpoint;
    private final String clientId;
    private final String clientSecret;
    // Token cache with thread-safe access
    private volatile CachedToken cachedToken;

    public ServiceAccountTokenProvider(RestTemplate restTemplate,
                                       @Value("${keycloak.service-account.token-endpoint:http://localhost:7080/realms/wms-realm/protocol/openid-connect/token}")
                                       String tokenEndpoint, @Value("${keycloak.service-account.client-id:wms-service-account}") String clientId,
                                       @Value("${keycloak.service-account.client-secret:}") String clientSecret) {
        this.restTemplate = restTemplate;
        this.tokenEndpoint = tokenEndpoint;
        this.clientId = clientId;
        this.clientSecret = clientSecret;

        if (clientSecret == null || clientSecret.isEmpty()) {
            log.warn("Service account client secret is not configured. Service-to-service authentication will fail. "
                    + "Please configure 'keycloak.service-account.client-secret' in application.yml");
        }

        log.info("Service account token provider initialized: clientId={}, tokenEndpoint={}", clientId, tokenEndpoint);
    }

    /**
     * Gets a valid access token for service-to-service authentication.
     * <p>
     * This method returns a cached token if still valid, otherwise requests a new token
     * from Keycloak using client_credentials grant flow.
     *
     * @return Valid JWT access token
     * @throws ServiceAccountAuthenticationException if token cannot be obtained
     */
    public String getAccessToken() {
        CachedToken currentToken = cachedToken;

        // Check if cached token is still valid
        if (currentToken != null && !currentToken.isExpired()) {
            log.debug("Using cached service account token (expires in {} seconds)", currentToken.getSecondsUntilExpiry());
            return currentToken.getAccessToken();
        }

        // Token is expired or doesn't exist - obtain new token
        synchronized (this) {
            // Double-check after acquiring lock (another thread may have refreshed)
            currentToken = cachedToken;
            if (currentToken != null && !currentToken.isExpired()) {
                log.debug("Using cached service account token after lock acquisition");
                return currentToken.getAccessToken();
            }

            // Obtain new token from Keycloak
            log.info("Obtaining new service account token from Keycloak: clientId={}", clientId);
            try {
                TokenResponse tokenResponse = requestTokenFromKeycloak();
                cachedToken = new CachedToken(tokenResponse.getAccessToken(), tokenResponse.getExpiresIn());
                log.info("Successfully obtained service account token (expires in {} seconds)", tokenResponse.getExpiresIn());
                return cachedToken.getAccessToken();
            } catch (Exception e) {
                log.error("Failed to obtain service account token from Keycloak: clientId={}, error={}", clientId, e.getMessage(), e);
                throw new ServiceAccountAuthenticationException("Failed to obtain service account token: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Requests a new access token from Keycloak using client_credentials grant.
     *
     * @return TokenResponse containing access token and expiry
     * @throws RestClientException if token request fails
     */
    private TokenResponse requestTokenFromKeycloak() {
        log.debug("Requesting service account token from Keycloak: endpoint={}", tokenEndpoint);

        // Build OAuth2 client_credentials grant request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(tokenEndpoint, request, TokenResponse.class);

            TokenResponse tokenResponse = response.getBody();
            if (tokenResponse == null) {
                throw new ServiceAccountAuthenticationException("Token endpoint returned empty response");
            }

            log.debug("Token obtained successfully: expiresIn={} seconds", tokenResponse.getExpiresIn());
            return tokenResponse;

        } catch (RestClientException e) {
            log.error("Failed to request token from Keycloak: endpoint={}, clientId={}, error={}", tokenEndpoint, clientId, e.getMessage());
            throw new ServiceAccountAuthenticationException("Failed to request service account token from Keycloak", e);
        }
    }

    /**
     * Clears the cached token, forcing a new token to be obtained on next request.
     * <p>
     * This is useful for testing or when token is known to be invalid.
     */
    public void clearCache() {
        synchronized (this) {
            log.info("Clearing cached service account token");
            cachedToken = null;
        }
    }

    /**
     * Internal class to hold cached token with expiry tracking.
     */
    @SuppressFBWarnings(value = {"CT_CONSTRUCTOR_THROW", "UUF_UNUSED_FIELD"}, justification = "CT_CONSTRUCTOR_THROW: Constructor is safe, no exceptions thrown. "
            + "UUF_UNUSED_FIELD: Fields are accessed via getter methods, SpotBugs cannot detect this.")
    private static class CachedToken {
        private final String accessToken;
        private final Instant expiresAt;

        CachedToken(String accessToken, long expiresInSeconds) {
            this.accessToken = accessToken;
            // Add buffer to refresh before actual expiry
            this.expiresAt = Instant.now().plusSeconds(expiresInSeconds - TOKEN_REFRESH_BUFFER_SECONDS);
        }

        String getAccessToken() {
            return accessToken;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        long getSecondsUntilExpiry() {
            return Instant.now().until(expiresAt, java.time.temporal.ChronoUnit.SECONDS);
        }
    }

    /**
     * DTO for Keycloak token endpoint response.
     */
    @Getter
    @Setter
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Lombok-generated constructor is safe, no exceptions thrown.")
    private static class TokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("expires_in")
        private long expiresIn;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("scope")
        private String scope;
    }
}
