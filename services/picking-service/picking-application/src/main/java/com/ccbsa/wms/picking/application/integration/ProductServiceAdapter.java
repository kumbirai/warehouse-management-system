package com.ccbsa.wms.picking.application.integration;

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
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.picking.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.picking.application.service.port.service.dto.ProductInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * Service Adapter: ProductServiceAdapter
 * <p>
 * Implements ProductServicePort for product validation.
 */
@Component
@Slf4j
public class ProductServiceAdapter implements ProductServicePort {
    private static final ParameterizedTypeReference<ApiResponse<ProductResponse>> PRODUCT_RESPONSE_TYPE = new ParameterizedTypeReference<ApiResponse<ProductResponse>>() {
    };

    private final RestTemplate restTemplate;
    private final String productServiceUrl;

    public ProductServiceAdapter(RestTemplate restTemplate, @Value("${product.service.url:http://product-service}") String productServiceUrl) {
        this.restTemplate = restTemplate;
        this.productServiceUrl = productServiceUrl;
    }

    @Override
    public boolean productExists(String productCode) {
        return validateProduct(productCode).isPresent();
    }

    @Override
    public Optional<ProductInfo> validateProduct(String productCode) {
        log.debug("Validating product from product-service: productCode={}", productCode);

        try {
            String url = String.format("%s/api/v1/products/by-code/%s", productServiceUrl, productCode);
            log.debug("Calling product service: {}", url);

            HttpHeaders headers = new HttpHeaders();
            // Note: TenantId should be extracted from TenantContext if needed
            // For now, we'll call without tenant header if not available

            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<ApiResponse<ProductResponse>> response = restTemplate.exchange(url, HttpMethod.GET, entity, PRODUCT_RESPONSE_TYPE);

            ApiResponse<ProductResponse> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                ProductResponse productResponse = responseBody.getData();
                ProductInfo productInfo = ProductInfo.builder()
                        .productId(productResponse.getProductId())
                        .productCode(productResponse.getProductCode())
                        .productName(productResponse.getProductCode()) // Use productCode as name if name not available
                        .description(productResponse.getDescription()).build();
                return Optional.of(productInfo);
            }

            return Optional.empty();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Product not found: {}", productCode);
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("Failed to validate product: productCode={}", productCode, e);
            throw new RuntimeException("Product service is temporarily unavailable", e);
        }
    }

    @Override
    public Optional<ProductInfo> getProductById(String productId, String tenantId) {
        log.debug("Getting product by ID from product-service: productId={}, tenantId={}", productId, tenantId);

        try {
            String url = String.format("%s/api/v1/products/%s", productServiceUrl, productId);
            log.debug("Calling product service: {}", url);

            HttpHeaders headers = new HttpHeaders();
            if (tenantId != null) {
                headers.set("X-Tenant-Id", tenantId);
            }

            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<ApiResponse<ProductResponse>> response = restTemplate.exchange(url, HttpMethod.GET, entity, PRODUCT_RESPONSE_TYPE);

            ApiResponse<ProductResponse> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                ProductResponse productResponse = responseBody.getData();
                ProductInfo productInfo = ProductInfo.builder()
                        .productId(productResponse.getProductId())
                        .productCode(productResponse.getProductCode())
                        .productName(productResponse.getProductCode()) // Use productCode as name if name not available
                        .description(productResponse.getDescription()).build();
                return Optional.of(productInfo);
            }

            return Optional.empty();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Product not found by ID: {}", productId);
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("Failed to get product by ID: productId={}", productId, e);
            throw new RuntimeException("Product service is temporarily unavailable", e);
        }
    }

    @Override
    public Optional<ProductInfo> getProductByCode(String productCode, TenantId tenantId) {
        log.debug("Getting product by code from product-service: productCode={}, tenantId={}", productCode, tenantId);

        try {
            String url = String.format("%s/api/v1/products/by-code/%s", productServiceUrl, productCode);
            log.debug("Calling product service: {}", url);

            HttpHeaders headers = new HttpHeaders();
            if (tenantId != null) {
                headers.set("X-Tenant-Id", tenantId.getValue());
            } else {
                // Fallback to TenantContext if available
                TenantId contextTenantId = TenantContext.getTenantId();
                if (contextTenantId != null) {
                    headers.set("X-Tenant-Id", contextTenantId.getValue());
                }
            }

            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<ApiResponse<ProductResponse>> response = restTemplate.exchange(url, HttpMethod.GET, entity, PRODUCT_RESPONSE_TYPE);

            ApiResponse<ProductResponse> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                ProductResponse productResponse = responseBody.getData();
                ProductInfo productInfo = ProductInfo.builder()
                        .productId(productResponse.getProductId())
                        .productCode(productResponse.getProductCode())
                        .productName(productResponse.getProductCode()) // Use productCode as name if name not available
                        .description(productResponse.getDescription()).build();
                return Optional.of(productInfo);
            }

            return Optional.empty();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Product not found by code: {}", productCode);
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("Failed to get product by code: productCode={}", productCode, e);
            // Graceful degradation: return empty instead of throwing
            // This allows the query to continue even if product service is unavailable
            return Optional.empty();
        }
    }

    /**
     * Response DTO for Product Service
     */
    private static class ProductResponse {
        private String productId;
        private String productCode;
        private String description;

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public String getProductCode() {
            return productCode;
        }

        public void setProductCode(String productCode) {
            this.productCode = productCode;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
