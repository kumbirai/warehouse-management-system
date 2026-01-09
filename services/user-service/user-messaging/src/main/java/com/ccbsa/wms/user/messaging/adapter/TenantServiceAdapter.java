package com.ccbsa.wms.user.messaging.adapter;

import java.util.Locale;
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
import com.ccbsa.wms.user.application.service.exception.TenantNotFoundException;
import com.ccbsa.wms.user.application.service.exception.TenantServiceException;
import com.ccbsa.wms.user.application.service.port.service.TenantServicePort;

import lombok.extern.slf4j.Slf4j;

/**
 * Adapter: TenantServiceAdapter
 * <p>
 * Implements TenantServicePort for tenant validation operations. Calls tenant-service REST API to validate tenant status.
 */
@Component
@Slf4j
public class TenantServiceAdapter implements TenantServicePort {
    private static final ParameterizedTypeReference<ApiResponse<String>> TENANT_STATUS_RESPONSE_TYPE = new ParameterizedTypeReference<ApiResponse<String>>() {
    };
    private static final ParameterizedTypeReference<ApiResponse<TenantResponseDto>> TENANT_RESPONSE_TYPE = new ParameterizedTypeReference<ApiResponse<TenantResponseDto>>() {
    };

    private final RestTemplate restTemplate;
    private final String tenantServiceUrl;

    public TenantServiceAdapter(RestTemplate restTemplate, @Value("${tenant.service.url:http://tenant-service}") String tenantServiceUrl) {
        this.restTemplate = restTemplate;
        this.tenantServiceUrl = tenantServiceUrl;
    }

    @Override
    public boolean isTenantActive(TenantId tenantId) {
        log.debug("Checking if tenant is active: tenantId={}", tenantId.getValue());

        try {
            String status = getTenantStatus(tenantId);
            boolean isActive = "ACTIVE".equals(status);
            log.debug("Tenant status check result: tenantId={}, status={}, isActive={}", tenantId.getValue(), status, isActive);
            return isActive;
        } catch (TenantNotFoundException e) {
            log.warn("Tenant not found: {}", tenantId.getValue());
            throw e;
        } catch (Exception e) {
            log.error("Failed to check tenant status: {}", e.getMessage(), e);
            throw new TenantServiceException(String.format("Failed to check tenant status: %s", e.getMessage()), e);
        }
    }

    @Override
    public String getTenantStatus(TenantId tenantId) {
        log.debug("Getting tenant status: tenantId={}", tenantId.getValue());

        try {
            String url = String.format("%s/api/v1/tenants/%s/status", tenantServiceUrl, tenantId.getValue());
            log.debug("Calling tenant service: {}", url);

            // Service-to-service authentication is handled automatically by ServiceAccountAuthenticationInterceptor
            // The interceptor will:
            // 1. Forward Authorization header from HTTP request context (if available)
            // 2. Use service account token for event-driven calls (no HTTP context)
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-Id", tenantId.getValue());

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<String>> response = restTemplate.exchange(url, HttpMethod.GET, entity, TENANT_STATUS_RESPONSE_TYPE);

            ApiResponse<String> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                String status = responseBody.getData();
                log.debug("Tenant status retrieved: tenantId={}, status={}", tenantId.getValue(), status);
                return status;
            }

            throw new TenantServiceException(String.format("Unexpected response from tenant service: %s", response.getStatusCode()));
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Tenant not found: {}", tenantId.getValue());
            throw new TenantNotFoundException(String.format("Tenant not found: %s", tenantId.getValue()), e);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Bad request when calling tenant service (likely missing X-Tenant-Id header): {}", e.getMessage());
            throw new TenantServiceException(String.format("Invalid request to tenant service: %s", e.getMessage()), e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Unauthorized when calling tenant service - authentication token may be missing or invalid: {}", e.getMessage());
            throw new TenantServiceException("Failed to authenticate with tenant service. Please ensure you are logged in.", e);
        } catch (RestClientException e) {
            log.error("Failed to call tenant service: {}", e.getMessage(), e);
            throw new TenantServiceException(String.format("Failed to call tenant service: %s", e.getMessage()), e);
        } catch (Exception e) {
            log.error("Unexpected error getting tenant status: {}", e.getMessage(), e);
            throw new TenantServiceException(String.format("Failed to get tenant status: %s", e.getMessage()), e);
        }
    }

    @Override
    public Optional<TenantInfo> getTenantInfo(TenantId tenantId) {
        log.debug("Getting tenant info: tenantId={}", tenantId.getValue());

        try {
            String url = String.format("%s/api/v1/tenants/%s", tenantServiceUrl, tenantId.getValue());
            log.debug("Calling tenant service: {}", url);

            // Service-to-service authentication is handled automatically by ServiceAccountAuthenticationInterceptor
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-Id", tenantId.getValue());

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<TenantResponseDto>> response = restTemplate.exchange(url, HttpMethod.GET, entity, TENANT_RESPONSE_TYPE);

            ApiResponse<TenantResponseDto> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                TenantResponseDto tenantResponse = responseBody.getData();
                TenantServicePort.TenantInfo.TenantStatus status = mapStatus(tenantResponse.getStatus());
                TenantServicePort.TenantInfo info = new TenantServicePort.TenantInfo(TenantId.of(tenantResponse.getTenantId()), tenantResponse.getName(), status);
                log.debug("Tenant info retrieved: tenantId={}, name={}", tenantId.getValue(), tenantResponse.getName());
                return Optional.of(info);
            }

            return Optional.empty();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Tenant not found: {}", tenantId.getValue());
            return Optional.empty();
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Bad request when calling tenant service (likely missing X-Tenant-Id header): {}", e.getMessage());
            throw new TenantServiceException(String.format("Invalid request to tenant service: %s", e.getMessage()), e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Unauthorized when calling tenant service - authentication token may be missing or invalid: {}", e.getMessage());
            throw new TenantServiceException("Failed to authenticate with tenant service. Please ensure you are logged in.", e);
        } catch (IllegalArgumentException e) {
            // Handle LoadBalancer service discovery failures
            if (e.getMessage() != null && e.getMessage().contains("Service Instance cannot be null")) {
                log.error("Tenant service not found in service registry (Eureka). Please ensure tenant-service is running and registered with Eureka: tenantId={}, serviceUrl={}",
                        tenantId.getValue(), tenantServiceUrl, e);
                throw new TenantServiceException(String.format(
                        "Tenant service is not available. The service may not be running or not registered with service discovery. Please contact system administrator."), e);
            }
            log.error("Invalid argument when calling tenant service: tenantId={}, error={}", tenantId.getValue(), e.getMessage(), e);
            throw new TenantServiceException(String.format("Failed to call tenant service: %s", e.getMessage()), e);
        } catch (RestClientException e) {
            log.error("Failed to call tenant service: {}", e.getMessage(), e);
            throw new TenantServiceException(String.format("Failed to call tenant service: %s", e.getMessage()), e);
        } catch (Exception e) {
            log.error("Unexpected error getting tenant info: {}", e.getMessage(), e);
            throw new TenantServiceException(String.format("Failed to get tenant info: %s", e.getMessage()), e);
        }
    }

    private TenantServicePort.TenantInfo.TenantStatus mapStatus(String status) {
        if (status == null) {
            return TenantServicePort.TenantInfo.TenantStatus.PENDING;
        }
        try {
            return TenantServicePort.TenantInfo.TenantStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warn("Unknown tenant status: {}, defaulting to PENDING", status);
            return TenantServicePort.TenantInfo.TenantStatus.PENDING;
        }
    }

    /**
     * DTO for tenant response from tenant-service.
     */
    private static class TenantResponseDto {
        private String tenantId;
        private String name;
        private String status;
        private String keycloakRealmName;

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getKeycloakRealmName() {
            return keycloakRealmName;
        }

        public void setKeycloakRealmName(String keycloakRealmName) {
            this.keycloakRealmName = keycloakRealmName;
        }
    }

}

