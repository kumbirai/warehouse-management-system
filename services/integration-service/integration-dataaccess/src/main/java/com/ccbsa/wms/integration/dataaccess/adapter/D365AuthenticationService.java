package com.ccbsa.wms.integration.dataaccess.adapter;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * Service: D365AuthenticationService
 * <p>
 * Handles OAuth token management for D365 Finance and Operations.
 * <p>
 * Implements token caching to avoid unnecessary authentication requests.
 */
@Service
@Slf4j
public class D365AuthenticationService {
    private static final int TOKEN_EXPIRY_BUFFER_SECONDS = 60; // Refresh token 60 seconds before expiry

    private final RestTemplate restTemplate;
    private final String d365TokenEndpoint;
    private final String d365ClientId;
    private final String d365ClientSecret;
    private final String d365Resource;

    // Token cache: tenantId -> CachedToken
    private final ConcurrentMap<String, CachedToken> tokenCache = new ConcurrentHashMap<>();

    public D365AuthenticationService(RestTemplate restTemplate, @Value("${d365.token-endpoint:}") String d365TokenEndpoint, @Value("${d365.client-id:}") String d365ClientId,
                                     @Value("${d365.client-secret:}") String d365ClientSecret, @Value("${d365.resource:}") String d365Resource) {
        this.restTemplate = restTemplate;
        this.d365TokenEndpoint = d365TokenEndpoint;
        this.d365ClientId = d365ClientId;
        this.d365ClientSecret = d365ClientSecret;
        this.d365Resource = d365Resource;
    }

    /**
     * Gets a valid OAuth access token for D365.
     * <p>
     * Uses cached token if available and not expired, otherwise fetches a new token.
     *
     * @param tenantId Tenant identifier (for multi-tenant token caching)
     * @return OAuth access token
     */
    public String getAccessToken(String tenantId) {
        CachedToken cachedToken = tokenCache.get(tenantId);
        if (cachedToken != null && !cachedToken.isExpired()) {
            log.debug("Using cached D365 access token for tenant: {}", tenantId);
            return cachedToken.token;
        }

        log.info("Fetching new D365 access token for tenant: {}", tenantId);
        String newToken = fetchAccessToken();
        long expiresAt = Instant.now().plusSeconds(3600).getEpochSecond(); // Default 1 hour expiry
        tokenCache.put(tenantId, new CachedToken(newToken, expiresAt));
        return newToken;
    }

    /**
     * Fetches a new OAuth access token from D365.
     *
     * @return OAuth access token
     */
    private String fetchAccessToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            body.add("client_id", d365ClientId);
            body.add("client_secret", d365ClientSecret);
            body.add("resource", d365Resource);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<TokenResponse> response = restTemplate.exchange(d365TokenEndpoint, HttpMethod.POST, request, TokenResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                TokenResponse tokenResponse = response.getBody();
                return tokenResponse.accessToken;
            }

            throw new RuntimeException("Failed to obtain D365 access token: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("Error fetching D365 access token", e);
            throw new RuntimeException("Failed to obtain D365 access token", e);
        }
    }

    /**
     * Clears the token cache for a specific tenant.
     *
     * @param tenantId Tenant identifier
     */
    public void clearTokenCache(String tenantId) {
        tokenCache.remove(tenantId);
        log.debug("Cleared D365 token cache for tenant: {}", tenantId);
    }

    /**
     * Cached token with expiry information.
     */
    private static class CachedToken {
        private final String token;
        private final long expiresAt;

        CachedToken(String token, long expiresAt) {
            this.token = token;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return Instant.now().getEpochSecond() >= (expiresAt - TOKEN_EXPIRY_BUFFER_SECONDS);
        }
    }

    /**
     * Token response DTO from D365 OAuth endpoint.
     */
    private static class TokenResponse {
        private String accessToken;
        private String tokenType;
        private int expiresIn;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }

        public int getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(int expiresIn) {
            this.expiresIn = expiresIn;
        }
    }
}
