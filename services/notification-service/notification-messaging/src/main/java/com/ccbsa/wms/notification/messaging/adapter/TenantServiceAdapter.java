package com.ccbsa.wms.notification.messaging.adapter;

import java.util.Optional;

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
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.notification.application.service.port.service.TenantServicePort;

/**
 * Adapter: TenantServiceAdapter
 * <p>
 * Implements TenantServicePort for retrieving tenant information from tenant-service.
 * Calls tenant-service REST API to get tenant email address.
 */
@Component
public class TenantServiceAdapter implements TenantServicePort {
    private static final Logger logger = LoggerFactory.getLogger(TenantServiceAdapter.class);

    private static final ParameterizedTypeReference<ApiResponse<TenantResponse>> TENANT_RESPONSE_TYPE =
            new ParameterizedTypeReference<ApiResponse<TenantResponse>>() {
            };

    private final RestTemplate restTemplate;
    private final String tenantServiceUrl;

    public TenantServiceAdapter(
            RestTemplate restTemplate,
            @Value("${tenant.service.url:http://tenant-service:8080}") String tenantServiceUrl) {
        this.restTemplate = restTemplate;
        this.tenantServiceUrl = tenantServiceUrl;
    }

    @Override
    public EmailAddress getTenantEmail(TenantId tenantId) {
        logger.debug("Getting tenant email from tenant-service: tenantId={}", tenantId.getValue());

        try {
            Optional<TenantServicePort.TenantDetails> details = getTenantDetails(tenantId);
            if (details.isPresent() && details.get().emailAddress() != null && !details.get().emailAddress().isEmpty()) {
                return EmailAddress.of(details.get().emailAddress());
            }
            throw new RuntimeException(String.format("Tenant email not found for tenantId: %s", tenantId.getValue()));
        } catch (RestClientException e) {
            logger.error("Failed to get tenant email from tenant-service: tenantId={}", tenantId.getValue(), e);
            throw new RuntimeException(String.format("Failed to retrieve tenant email: %s", e.getMessage()), e);
        } catch (Exception e) {
            logger.error("Unexpected error getting tenant email: tenantId={}", tenantId.getValue(), e);
            throw new RuntimeException(String.format("Failed to get tenant email: %s", e.getMessage()), e);
        }
    }

    @Override
    public Optional<TenantServicePort.TenantDetails> getTenantDetails(TenantId tenantId) {
        logger.debug("Getting tenant details from tenant-service: tenantId={}", tenantId.getValue());

        try {
            String url = String.format("%s/api/v1/tenants/%s", tenantServiceUrl, tenantId.getValue());
            logger.debug("Calling tenant service: {}", url);

            ResponseEntity<ApiResponse<TenantResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    TENANT_RESPONSE_TYPE);

            ApiResponse<TenantResponse> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                TenantResponse tenantResponse = responseBody.getData();
                TenantServicePort.TenantDetails details = new TenantServicePort.TenantDetails(
                        tenantResponse.getTenantId(),
                        tenantResponse.getName(),
                        tenantResponse.getStatus(),
                        tenantResponse.getEmailAddress(),
                        tenantResponse.getPhone(),
                        tenantResponse.getAddress()
                );
                logger.debug("Tenant details retrieved: tenantId={}, name={}", tenantId.getValue(), tenantResponse.getName());
                return Optional.of(details);
            }

            return Optional.empty();
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("Tenant not found: {}", tenantId.getValue());
            return Optional.empty();
        } catch (RestClientException e) {
            logger.error("Failed to get tenant details from tenant-service: tenantId={}", tenantId.getValue(), e);
            throw new RuntimeException(String.format("Failed to retrieve tenant details: %s", e.getMessage()), e);
        } catch (Exception e) {
            logger.error("Unexpected error getting tenant details: tenantId={}", tenantId.getValue(), e);
            throw new RuntimeException(String.format("Failed to get tenant details: %s", e.getMessage()), e);
        }
    }

    /**
     * DTO for tenant-service response.
     * Matches the TenantResponse structure from tenant-service API.
     */
    private static class TenantResponse {
        private String tenantId;
        private String name;
        private String status;
        private String emailAddress;
        private String phone;
        private String address;

        public String getTenantId() {
            return tenantId;
        }

        @SuppressWarnings("unused")
        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getName() {
            return name;
        }

        @SuppressWarnings("unused")
        public void setName(String name) {
            this.name = name;
        }

        public String getStatus() {
            return status;
        }

        @SuppressWarnings("unused")
        public void setStatus(String status) {
            this.status = status;
        }

        public String getEmailAddress() {
            return emailAddress;
        }

        @SuppressWarnings("unused")
        public void setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
        }

        public String getPhone() {
            return phone;
        }

        @SuppressWarnings("unused")
        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getAddress() {
            return address;
        }

        @SuppressWarnings("unused")
        public void setAddress(String address) {
            this.address = address;
        }
    }
}

