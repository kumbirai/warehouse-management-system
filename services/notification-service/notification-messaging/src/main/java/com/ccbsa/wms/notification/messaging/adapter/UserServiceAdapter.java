package com.ccbsa.wms.notification.messaging.adapter;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.domain.valueobject.EmailAddress;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.notification.application.service.port.service.UserServicePort;

import lombok.extern.slf4j.Slf4j;

/**
 * Adapter: UserServiceAdapter
 * <p>
 * Implements UserServicePort for retrieving user information from user-service.
 * Calls user-service REST API to get user email address.
 * <p>
 * <b>Service-to-Service Authentication:</b> This adapter uses a RestTemplate configured
 * with {@link com.ccbsa.wms.common.security.ServiceAccountAuthenticationInterceptor} that
 * automatically handles authentication for both HTTP request context (forwards user token)
 * and event-driven calls (uses service account token).
 */
@Component
@Slf4j
public class UserServiceAdapter implements UserServicePort {
    private static final ParameterizedTypeReference<ApiResponse<UserResponse>> USER_RESPONSE_TYPE = new ParameterizedTypeReference<ApiResponse<UserResponse>>() {
    };

    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public UserServiceAdapter(RestTemplate restTemplate, @Value("${user.service.url:http://user-service}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    @Override
    public Optional<EmailAddress> getUserEmail(UserId userId, com.ccbsa.common.domain.valueobject.TenantId tenantId) {
        log.debug("Getting user email from user-service: userId={}, tenantId={}", userId.getValue(), tenantId.getValue());

        try {
            String url = String.format("%s/api/v1/users/%s", userServiceUrl, userId.getValue());
            log.debug("Calling user service: {}", url);

            // Service-to-service authentication is handled automatically by ServiceAccountAuthenticationInterceptor
            // The interceptor will:
            // 1. Forward Authorization header from HTTP request context (if available)
            // 2. Use service account token for event-driven calls (no HTTP context)
            HttpHeaders headers = new HttpHeaders();

            // Set X-Tenant-Id header (required by user service)
            headers.set("X-Tenant-Id", tenantId.getValue());
            log.debug("Setting X-Tenant-Id header: {}", tenantId.getValue());

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<UserResponse>> response = restTemplate.exchange(url, HttpMethod.GET, entity, USER_RESPONSE_TYPE);

            ApiResponse<UserResponse> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                UserResponse userResponse = responseBody.getData();
                if (userResponse.getEmailAddress() != null && !userResponse.getEmailAddress().isEmpty()) {
                    log.debug("User email retrieved: userId={}, email={}", userId.getValue(), userResponse.getEmailAddress());
                    return Optional.of(EmailAddress.of(userResponse.getEmailAddress()));
                } else {
                    log.warn("User exists but email is not set: userId={}", userId.getValue());
                    return Optional.empty();
                }
            }

            log.warn("User not found or invalid response: userId={}, status={}", userId.getValue(), response.getStatusCode());
            return Optional.empty();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("User not found: userId={}", userId.getValue());
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("Failed to get user email from user-service: userId={}", userId.getValue(), e);
            throw new RuntimeException(String.format("Failed to retrieve user email: %s", e.getMessage()), e);
        } catch (Exception e) {
            log.error("Unexpected error getting user email: userId={}", userId.getValue(), e);
            throw new RuntimeException(String.format("Failed to get user email: %s", e.getMessage()), e);
        }
    }

    /**
     * DTO for user-service response. Simplified structure matching the expected API response.
     * <p>
     * Note: Field name matches user-service API response which uses @JsonProperty("emailAddress").
     */
    private static class UserResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("emailAddress")
        private String emailAddress;

        public String getEmailAddress() {
            return emailAddress;
        }

        /**
         * Setter required for Jackson deserialization. Suppressed unused warning as Jackson uses reflection to call this method.
         */
        @SuppressWarnings("unused")
        public void setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
        }
    }
}

