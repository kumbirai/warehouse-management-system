package com.ccbsa.wms.stock.dataaccess.adapter;

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
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.stock.application.service.port.service.UserServicePort;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter: UserServiceAdapter
 * <p>
 * Implements UserServicePort for retrieving user information from user-service.
 * Uses circuit breaker and retry for fault tolerance.
 */
@Component
@Slf4j
public class UserServiceAdapter implements UserServicePort {
    private static final ParameterizedTypeReference<ApiResponse<UserInfoResponse>> USER_RESPONSE_TYPE = new ParameterizedTypeReference<ApiResponse<UserInfoResponse>>() {
    };

    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public UserServiceAdapter(RestTemplate restTemplate, @Value("${user.service.url:http://user-service}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    @Override
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserInfoFallback")
    @Retry(name = "userService")
    public Optional<UserInfo> getUserInfo(UserId userId, TenantId tenantId) {
        log.debug("Getting user info from user-service: userId={}, tenantId={}", userId.getValue(), tenantId.getValue());

        try {
            String url = String.format("%s/api/v1/users/%s", userServiceUrl, userId.getValue());
            log.debug("Calling user service: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-Id", tenantId.getValue());
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<UserInfoResponse>> response = restTemplate.exchange(url, HttpMethod.GET, entity, USER_RESPONSE_TYPE);

            ApiResponse<UserInfoResponse> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                UserInfoResponse userResponse = responseBody.getData();
                return Optional.of(new UserInfo(userResponse.getUserId(), userResponse.getFirstName(), userResponse.getLastName(), userResponse.getUsername()));
            }

            log.warn("User service returned unexpected response: status={}", response.getStatusCode());
            return Optional.empty();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("User not found: {}", userId.getValue());
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("Failed to get user info from user-service: userId={}", userId.getValue(), e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error getting user info: userId={}", userId.getValue(), e);
            return Optional.empty();
        }
    }

    /**
     * Fallback method for circuit breaker.
     * Returns empty when circuit is open or service is down.
     */
    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD", justification = "Fallback method called by Resilience4j circuit breaker via reflection")
    private Optional<UserInfo> getUserInfoFallback(UserId userId, TenantId tenantId, Exception e) {
        log.warn("Circuit breaker fallback triggered for user info: userId={}, error={}", userId.getValue(), e.getMessage());
        return Optional.empty();
    }

    /**
     * DTO for user-service response.
     */
    private static class UserInfoResponse {
        private String userId;
        private String firstName;
        private String lastName;
        private String username;

        public String getUserId() {
            return userId;
        }

        @SuppressWarnings("unused")
        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getFirstName() {
            return firstName;
        }

        @SuppressWarnings("unused")
        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        @SuppressWarnings("unused")
        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getUsername() {
            return username;
        }

        @SuppressWarnings("unused")
        public void setUsername(String username) {
            this.username = username;
        }
    }
}
