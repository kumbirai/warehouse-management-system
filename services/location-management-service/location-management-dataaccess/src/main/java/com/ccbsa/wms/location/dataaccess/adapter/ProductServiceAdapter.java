package com.ccbsa.wms.location.dataaccess.adapter;

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
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.application.service.exception.ProductServiceException;
import com.ccbsa.wms.location.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;

import lombok.extern.slf4j.Slf4j;

/**
 * Adapter: ProductServiceAdapter
 * <p>
 * Implements ProductServicePort for retrieving product information from product-service.
 * Calls product-service REST API to get product information.
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
    public Optional<ProductInfo> getProductByCode(ProductCode productCode, TenantId tenantId) {
        log.debug("Getting product by code from product-service: productCode={}, tenantId={}", productCode.getValue(), tenantId.getValue());

        try {
            String url = String.format("%s/api/v1/products/by-code/%s", productServiceUrl, productCode.getValue());
            log.debug("Calling product service: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-Id", tenantId.getValue());

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<ProductResponse>> response = restTemplate.exchange(url, HttpMethod.GET, entity, PRODUCT_RESPONSE_TYPE);

            ApiResponse<ProductResponse> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                ProductResponse productResponse = responseBody.getData();
                ProductInfo productInfo =
                        new ProductInfo(productResponse.getProductId(), productResponse.getProductCode(), productResponse.getDescription(), productResponse.getPrimaryBarcode());
                log.debug("Product retrieved by code: productId={}, productCode={}", productResponse.getProductId(), productResponse.getProductCode());
                return Optional.of(productInfo);
            }

            return Optional.empty();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Product not found by code: {}", productCode.getValue());
            return Optional.empty();
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Unauthorized access to product-service: productCode={}, status={}", productCode.getValue(), e.getStatusCode(), e);
            throw new ProductServiceException("Product service authentication failed. Please check service account configuration.", e);
        } catch (HttpServerErrorException e) {
            log.error("Product service returned server error: productCode={}, status={}", productCode.getValue(), e.getStatusCode(), e);
            throw new ProductServiceException(
                    String.format("Product service error (status %s): %s", e.getStatusCode(), e.getResponseBodyAsString() != null ? e.getResponseBodyAsString() : e.getMessage()),
                    e);
        } catch (RestClientException e) {
            log.error("Failed to get product by code from product-service: productCode={}, error={}", productCode.getValue(), e.getMessage(), e);
            throw new ProductServiceException("Product service is temporarily unavailable. Please try again later.", e);
        } catch (Exception e) {
            log.error("Unexpected error getting product by code: productCode={}", productCode.getValue(), e);
            throw new ProductServiceException(String.format("Product service error: %s", e.getMessage()), e);
        }
    }

    @Override
    public Optional<ProductInfo> getProductById(ProductId productId, TenantId tenantId) {
        log.debug("Getting product by ID from product-service: productId={}, tenantId={}", productId.getValueAsString(), tenantId.getValue());

        try {
            String url = String.format("%s/api/v1/products/%s", productServiceUrl, productId.getValueAsString());
            log.debug("Calling product service: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-Id", tenantId.getValue());

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<ProductResponse>> response = restTemplate.exchange(url, HttpMethod.GET, entity, PRODUCT_RESPONSE_TYPE);

            ApiResponse<ProductResponse> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                ProductResponse productResponse = responseBody.getData();
                ProductInfo productInfo =
                        new ProductInfo(productResponse.getProductId(), productResponse.getProductCode(), productResponse.getDescription(), productResponse.getPrimaryBarcode());
                log.debug("Product retrieved by ID: productId={}, productCode={}", productResponse.getProductId(), productResponse.getProductCode());
                return Optional.of(productInfo);
            }

            return Optional.empty();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Product not found by ID: {}", productId.getValueAsString());
            return Optional.empty();
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Unauthorized access to product-service: productId={}, status={}", productId.getValueAsString(), e.getStatusCode(), e);
            throw new ProductServiceException("Product service authentication failed. Please check service account configuration.", e);
        } catch (HttpServerErrorException e) {
            log.error("Product service returned server error: productId={}, status={}", productId.getValueAsString(), e.getStatusCode(), e);
            throw new ProductServiceException(
                    String.format("Product service error (status %s): %s", e.getStatusCode(), e.getResponseBodyAsString() != null ? e.getResponseBodyAsString() : e.getMessage()),
                    e);
        } catch (RestClientException e) {
            log.error("Failed to get product by ID from product-service: productId={}, error={}", productId.getValueAsString(), e.getMessage(), e);
            throw new ProductServiceException("Product service is temporarily unavailable. Please try again later.", e);
        } catch (Exception e) {
            log.error("Unexpected error getting product by ID: productId={}", productId.getValueAsString(), e);
            throw new ProductServiceException(String.format("Product service error: %s", e.getMessage()), e);
        }
    }

    /**
     * DTO for product-service product response.
     */
    private static class ProductResponse {
        private String productId;
        private String productCode;
        private String description;
        private String primaryBarcode;

        public String getProductId() {
            return productId;
        }

        @SuppressWarnings("unused")
        public void setProductId(String productId) {
            this.productId = productId;
        }

        public String getProductCode() {
            return productCode;
        }

        @SuppressWarnings("unused")
        public void setProductCode(String productCode) {
            this.productCode = productCode;
        }

        public String getDescription() {
            return description;
        }

        @SuppressWarnings("unused")
        public void setDescription(String description) {
            this.description = description;
        }

        public String getPrimaryBarcode() {
            return primaryBarcode;
        }

        @SuppressWarnings("unused")
        public void setPrimaryBarcode(String primaryBarcode) {
            this.primaryBarcode = primaryBarcode;
        }
    }
}
