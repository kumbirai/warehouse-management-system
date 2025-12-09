package com.ccbsa.wms.notification.messaging.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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

/**
 * Adapter: UserServiceAdapter
 * <p>
 * Implements UserServicePort for retrieving user information from user-service.
 * Calls user-service REST API to get user email address.
 */
@Component
public class UserServiceAdapter implements UserServicePort {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceAdapter.class);

    private static final ParameterizedTypeReference<ApiResponse<UserResponse>> USER_RESPONSE_TYPE =
            new ParameterizedTypeReference<ApiResponse<UserResponse>>() {
            };

    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public UserServiceAdapter(
            RestTemplate restTemplate,
            @Value("${user.service.url:http://user-service:8080}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    @Override
    public EmailAddress getUserEmail(UserId userId) {
        logger.debug("Getting user email from user-service: userId={}", userId.getValue());

        try {
            String url = String.format("%s/api/v1/users/%s", userServiceUrl, userId.getValue());
            logger.debug("Calling user service: {}", url);

            ResponseEntity<ApiResponse<UserResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    USER_RESPONSE_TYPE);

            ApiResponse<UserResponse> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                UserResponse userResponse = responseBody.getData();
                if (userResponse.getEmail() != null && !userResponse.getEmail().isEmpty()) {
                    logger.debug("User email retrieved: userId={}, email={}", userId.getValue(), userResponse.getEmail());
                    return EmailAddress.of(userResponse.getEmail());
                } else {
                    throw new RuntimeException(String.format("User email not found for userId: %s", userId.getValue()));
                }
            }

            throw new RuntimeException(String.format("User not found: %s", userId.getValue()));
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("User not found: {}", userId.getValue());
            throw new RuntimeException(String.format("User not found: %s", userId.getValue()), e);
        } catch (RestClientException e) {
            logger.error("Failed to get user email from user-service: userId={}", userId.getValue(), e);
            throw new RuntimeException(String.format("Failed to retrieve user email: %s", e.getMessage()), e);
        } catch (Exception e) {
            logger.error("Unexpected error getting user email: userId={}", userId.getValue(), e);
            throw new RuntimeException(String.format("Failed to get user email: %s", e.getMessage()), e);
        }
    }

    /**
     * DTO for user-service response.
     * Simplified structure matching the expected API response.
     */
    private static class UserResponse {
        private String email;

        public String getEmail() {
            return email;
        }

        /**
         * Setter required for Jackson deserialization.
         * Suppressed unused warning as Jackson uses reflection to call this method.
         */
        @SuppressWarnings("unused")
        public void setEmail(String email) {
            this.email = email;
        }
    }
}

