package com.ccbsa.wms.user.messaging.adapter;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.user.application.service.exception.TenantNotFoundException;
import com.ccbsa.wms.user.application.service.exception.TenantServiceException;
import com.ccbsa.wms.user.application.service.port.service.TenantServicePort;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Adapter: TenantServiceAdapter
 * <p>
 * Implements TenantServicePort for tenant validation operations. Calls tenant-service REST API to validate tenant status.
 */
@Component
public class TenantServiceAdapter
        implements TenantServicePort {
    private static final Logger logger = LoggerFactory.getLogger(TenantServiceAdapter.class);

    private static final ParameterizedTypeReference<ApiResponse<String>> TENANT_STATUS_RESPONSE_TYPE = new ParameterizedTypeReference<ApiResponse<String>>() {
    };
    private static final ParameterizedTypeReference<ApiResponse<TenantResponseDto>> TENANT_RESPONSE_TYPE = new ParameterizedTypeReference<ApiResponse<TenantResponseDto>>() {
    };

    private final RestTemplate restTemplate;
    private final String tenantServiceUrl;

    public TenantServiceAdapter(RestTemplate restTemplate,
                                @Value("${tenant.service.url:http://tenant-service:8080}") String tenantServiceUrl) {
        this.restTemplate = restTemplate;
        this.tenantServiceUrl = tenantServiceUrl;
    }

    @Override
    public boolean isTenantActive(TenantId tenantId) {
        logger.debug("Checking if tenant is active: tenantId={}", tenantId.getValue());

        try {
            String status = getTenantStatus(tenantId);
            boolean isActive = "ACTIVE".equals(status);
            logger.debug("Tenant status check result: tenantId={}, status={}, isActive={}", tenantId.getValue(), status, isActive);
            return isActive;
        } catch (TenantNotFoundException e) {
            logger.warn("Tenant not found: {}", tenantId.getValue());
            throw e;
        } catch (Exception e) {
            logger.error("Failed to check tenant status: {}", e.getMessage(), e);
            throw new TenantServiceException(String.format("Failed to check tenant status: %s", e.getMessage()), e);
        }
    }

    @Override
    public String getTenantStatus(TenantId tenantId) {
        logger.debug("Getting tenant status: tenantId={}", tenantId.getValue());

        try {
            String url = String.format("%s/api/v1/tenants/%s/status", tenantServiceUrl, tenantId.getValue());
            logger.debug("Calling tenant service: {}", url);

            // Forward headers from current request for service-to-service authentication
            HttpHeaders headers = new HttpHeaders();
            String authorizationHeader = getAuthorizationHeader();
            if (authorizationHeader != null) {
                headers.set("Authorization", authorizationHeader);
                logger.debug("Forwarding Authorization header to tenant service");
            } else {
                logger.warn("No Authorization header found in current request - tenant service call may fail");
            }

            // Forward X-Tenant-Id header (required by tenant service)
            String tenantIdHeader = getTenantIdHeader();
            if (tenantIdHeader != null) {
                headers.set("X-Tenant-Id", tenantIdHeader);
                logger.debug("Forwarding X-Tenant-Id header to tenant service: {}", tenantIdHeader);
            } else {
                // Set the tenantId from the method parameter as fallback
                headers.set("X-Tenant-Id", tenantId.getValue());
                logger.debug("Setting X-Tenant-Id header from method parameter: {}", tenantId.getValue());
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<String>> response = restTemplate.exchange(url, HttpMethod.GET, entity, TENANT_STATUS_RESPONSE_TYPE);

            ApiResponse<String> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                String status = responseBody.getData();
                logger.debug("Tenant status retrieved: tenantId={}, status={}", tenantId.getValue(), status);
                return status;
            }

            throw new TenantServiceException(String.format("Unexpected response from tenant service: %s", response.getStatusCode()));
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("Tenant not found: {}", tenantId.getValue());
            throw new TenantNotFoundException(String.format("Tenant not found: %s", tenantId.getValue()), e);
        } catch (HttpClientErrorException.BadRequest e) {
            logger.error("Bad request when calling tenant service (likely missing X-Tenant-Id header): {}", e.getMessage());
            throw new TenantServiceException(String.format("Invalid request to tenant service: %s", e.getMessage()), e);
        } catch (HttpClientErrorException.Unauthorized e) {
            logger.error("Unauthorized when calling tenant service - authentication token may be missing or invalid: {}", e.getMessage());
            throw new TenantServiceException("Failed to authenticate with tenant service. Please ensure you are logged in.", e);
        } catch (RestClientException e) {
            logger.error("Failed to call tenant service: {}", e.getMessage(), e);
            throw new TenantServiceException(String.format("Failed to call tenant service: %s", e.getMessage()), e);
        } catch (Exception e) {
            logger.error("Unexpected error getting tenant status: {}", e.getMessage(), e);
            throw new TenantServiceException(String.format("Failed to get tenant status: %s", e.getMessage()), e);
        }
    }

    /**
     * Extracts the Authorization header from the current HTTP request. This allows service-to-service calls to forward the JWT token.
     *
     * @return Authorization header value or null if not available
     */
    private String getAuthorizationHeader() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                // ServletRequestAttributes.getRequest() is guaranteed to return non-null
                return request.getHeader("Authorization");
            }
        } catch (Exception e) {
            logger.debug("Could not extract Authorization header from request context: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extracts the X-Tenant-Id header from the current HTTP request. This allows service-to-service calls to forward the tenant context.
     *
     * @return X-Tenant-Id header value or null if not available
     */
    private String getTenantIdHeader() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                // ServletRequestAttributes.getRequest() is guaranteed to return non-null
                return request.getHeader("X-Tenant-Id");
            }
        } catch (Exception e) {
            logger.debug("Could not extract X-Tenant-Id header from request context: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public Optional<TenantInfo> getTenantInfo(TenantId tenantId) {
        logger.debug("Getting tenant info: tenantId={}", tenantId.getValue());

        try {
            String url = String.format("%s/api/v1/tenants/%s", tenantServiceUrl, tenantId.getValue());
            logger.debug("Calling tenant service: {}", url);

            // Forward headers from current request for service-to-service authentication
            HttpHeaders headers = new HttpHeaders();
            String authorizationHeader = getAuthorizationHeader();
            if (authorizationHeader != null) {
                headers.set("Authorization", authorizationHeader);
                logger.debug("Forwarding Authorization header to tenant service");
            } else {
                logger.warn("No Authorization header found in current request - tenant service call may fail");
            }

            // Forward X-Tenant-Id header (required by tenant service)
            String tenantIdHeader = getTenantIdHeader();
            if (tenantIdHeader != null) {
                headers.set("X-Tenant-Id", tenantIdHeader);
                logger.debug("Forwarding X-Tenant-Id header to tenant service: {}", tenantIdHeader);
            } else {
                // Set the tenantId from the method parameter as fallback
                headers.set("X-Tenant-Id", tenantId.getValue());
                logger.debug("Setting X-Tenant-Id header from method parameter: {}", tenantId.getValue());
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<TenantResponseDto>> response = restTemplate.exchange(url, HttpMethod.GET, entity, TENANT_RESPONSE_TYPE);

            ApiResponse<TenantResponseDto> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                TenantResponseDto tenantResponse = responseBody.getData();
                TenantServicePort.TenantInfo.TenantStatus status = mapStatus(tenantResponse.getStatus());
                TenantServicePort.TenantInfo info = new TenantServicePort.TenantInfo(
                        TenantId.of(tenantResponse.getTenantId()),
                        tenantResponse.getName(),
                        status
                );
                logger.debug("Tenant info retrieved: tenantId={}, name={}", tenantId.getValue(), tenantResponse.getName());
                return Optional.of(info);
            }

            return Optional.empty();
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("Tenant not found: {}", tenantId.getValue());
            return Optional.empty();
        } catch (HttpClientErrorException.BadRequest e) {
            logger.error("Bad request when calling tenant service (likely missing X-Tenant-Id header): {}", e.getMessage());
            throw new TenantServiceException(String.format("Invalid request to tenant service: %s", e.getMessage()), e);
        } catch (HttpClientErrorException.Unauthorized e) {
            logger.error("Unauthorized when calling tenant service - authentication token may be missing or invalid: {}", e.getMessage());
            throw new TenantServiceException("Failed to authenticate with tenant service. Please ensure you are logged in.", e);
        } catch (RestClientException e) {
            logger.error("Failed to call tenant service: {}", e.getMessage(), e);
            throw new TenantServiceException(String.format("Failed to call tenant service: %s", e.getMessage()), e);
        } catch (Exception e) {
            logger.error("Unexpected error getting tenant info: {}", e.getMessage(), e);
            throw new TenantServiceException(String.format("Failed to get tenant info: %s", e.getMessage()), e);
        }
    }

    private TenantServicePort.TenantInfo.TenantStatus mapStatus(String status) {
        if (status == null) {
            return TenantServicePort.TenantInfo.TenantStatus.PENDING;
        }
        try {
            return TenantServicePort.TenantInfo.TenantStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown tenant status: {}, defaulting to PENDING", status);
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

