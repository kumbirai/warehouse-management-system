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
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.stock.application.service.exception.ProductServiceException;
import com.ccbsa.wms.stock.application.service.port.service.ProductServicePort;

import lombok.extern.slf4j.Slf4j;

/**
 * Adapter: ProductServiceAdapter
 * <p>
 * Implements ProductServicePort for retrieving product information from product-service.
 * Calls product-service REST API to validate product codes and barcodes.
 * <p>
 * <b>Service-to-Service Authentication:</b> This adapter uses a RestTemplate configured
 * with {@link com.ccbsa.wms.common.security.ServiceAccountAuthenticationInterceptor} that
 * automatically handles authentication for both HTTP request context (forwards user token)
 * and event-driven calls (uses service account token).
 */
@Component
@Slf4j
public class ProductServiceAdapter implements ProductServicePort {
    private static final ParameterizedTypeReference<ApiResponse<ProductResponse>> PRODUCT_RESPONSE_TYPE = new ParameterizedTypeReference<ApiResponse<ProductResponse>>() {
    };

    private static final ParameterizedTypeReference<ApiResponse<ValidateBarcodeResponse>> BARCODE_RESPONSE_TYPE =
            new ParameterizedTypeReference<ApiResponse<ValidateBarcodeResponse>>() {
            };

    private final RestTemplate restTemplate;
    private final String productServiceUrl;

    public ProductServiceAdapter(RestTemplate restTemplate, @Value("${product.service.url:http://product-service}") String productServiceUrl) {
        this.restTemplate = restTemplate;
        this.productServiceUrl = productServiceUrl;
    }

    @Override
    public Optional<ProductInfo> validateProductBarcode(String barcode, TenantId tenantId) {
        log.debug("Validating product barcode from product-service: barcode={}, tenantId={}", barcode, tenantId.getValue());

        try {
            String url = String.format("%s/api/v1/products/validate-barcode?barcode=%s", productServiceUrl, barcode);
            log.debug("Calling product service: {}", url);

            // Service-to-service authentication is handled automatically by ServiceAccountAuthenticationInterceptor
            HttpHeaders headers = new HttpHeaders();

            // Set X-Tenant-Id header (required by product service)
            headers.set("X-Tenant-Id", tenantId.getValue());
            log.debug("Setting X-Tenant-Id header: {}", tenantId.getValue());

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<ValidateBarcodeResponse>> response = restTemplate.exchange(url, HttpMethod.GET, entity, BARCODE_RESPONSE_TYPE);

            ApiResponse<ValidateBarcodeResponse> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                ValidateBarcodeResponse barcodeResponse = responseBody.getData();
                if (barcodeResponse.isValid() && barcodeResponse.getProductInfo() != null) {
                    ValidateBarcodeResponse.ProductInfoDTO productInfoDTO = barcodeResponse.getProductInfo();
                    ProductInfo productInfo =
                            new ProductInfo(productInfoDTO.getProductId(), productInfoDTO.getProductCode(), productInfoDTO.getDescription(), productInfoDTO.getBarcode());
                    log.debug("Product barcode validated: productId={}, productCode={}", productInfoDTO.getProductId(), productInfoDTO.getProductCode());
                    return Optional.of(productInfo);
                }
            }

            return Optional.empty();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Product barcode not found: {}", barcode);
            return Optional.empty();
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Unauthorized access to product-service: barcode={}, status={}, response={}", barcode, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new ProductServiceException(String.format("Product service authentication failed. Please check service account configuration."), e);
        } catch (HttpServerErrorException e) {
            log.error("Product service returned server error: barcode={}, status={}, response={}", barcode, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new ProductServiceException(
                    String.format("Product service error (status %s): %s", e.getStatusCode(), e.getResponseBodyAsString() != null ? e.getResponseBodyAsString() : e.getMessage()),
                    e);
        } catch (IllegalArgumentException e) {
            // Handle LoadBalancer service discovery failures
            if (e.getMessage() != null && e.getMessage().contains("Service Instance cannot be null")) {
                log.error("Product service not found in service registry (Eureka). Please ensure product-service is running and registered with Eureka: barcode={}, serviceUrl={}",
                        barcode, productServiceUrl, e);
                throw new ProductServiceException(String.format(
                        "Product service is not available. The service may not be running or not registered with service discovery. Please contact system administrator."), e);
            }
            log.error("Invalid argument when calling product-service: barcode={}, error={}", barcode, e.getMessage(), e);
            throw new ProductServiceException(String.format("Product service error: %s", e.getMessage()), e);
        } catch (RestClientException e) {
            log.error("Failed to validate product barcode from product-service: barcode={}, error={}", barcode, e.getMessage(), e);
            throw new ProductServiceException(String.format("Product service is temporarily unavailable. Please try again later."), e);
        } catch (Exception e) {
            log.error("Unexpected error validating product barcode: barcode={}", barcode, e);
            throw new ProductServiceException(String.format("Product service error: %s", e.getMessage()), e);
        }
    }

    @Override
    public Optional<ProductInfo> getProductByCode(ProductCode productCode, TenantId tenantId) {
        log.debug("Getting product by code from product-service: productCode={}, tenantId={}", productCode.getValue(), tenantId.getValue());

        try {
            String url = String.format("%s/api/v1/products/by-code/%s", productServiceUrl, productCode.getValue());
            log.debug("Calling product service: {}", url);

            // Service-to-service authentication is handled automatically by ServiceAccountAuthenticationInterceptor
            HttpHeaders headers = new HttpHeaders();

            // Set X-Tenant-Id header (required by product service)
            headers.set("X-Tenant-Id", tenantId.getValue());
            log.debug("Setting X-Tenant-Id header: {}", tenantId.getValue());

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
            log.error("Unauthorized access to product-service: productCode={}, status={}, response={}", productCode.getValue(), e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new ProductServiceException(String.format("Product service authentication failed. Please check service account configuration."), e);
        } catch (HttpServerErrorException e) {
            log.error("Product service returned server error: productCode={}, status={}, response={}", productCode.getValue(), e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new ProductServiceException(
                    String.format("Product service error (status %s): %s", e.getStatusCode(), e.getResponseBodyAsString() != null ? e.getResponseBodyAsString() : e.getMessage()),
                    e);
        } catch (IllegalArgumentException e) {
            // Handle LoadBalancer service discovery failures
            if (e.getMessage() != null && e.getMessage().contains("Service Instance cannot be null")) {
                log.error(
                        "Product service not found in service registry (Eureka). Please ensure product-service is running and registered with Eureka: productCode={}, "
                                + "serviceUrl={}",
                        productCode.getValue(), productServiceUrl, e);
                throw new ProductServiceException(String.format(
                        "Product service is not available. The service may not be running or not registered with service discovery. Please contact system administrator."), e);
            }
            log.error("Invalid argument when calling product-service: productCode={}, error={}", productCode.getValue(), e.getMessage(), e);
            throw new ProductServiceException(String.format("Product service error: %s", e.getMessage()), e);
        } catch (RestClientException e) {
            log.error("Failed to get product by code from product-service: productCode={}, error={}", productCode.getValue(), e.getMessage(), e);
            throw new ProductServiceException(String.format("Product service is temporarily unavailable. Please try again later."), e);
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

            // Service-to-service authentication is handled automatically by ServiceAccountAuthenticationInterceptor
            HttpHeaders headers = new HttpHeaders();

            // Set X-Tenant-Id header (required by product service)
            headers.set("X-Tenant-Id", tenantId.getValue());
            log.debug("Setting X-Tenant-Id header: {}", tenantId.getValue());

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
            log.error("Unauthorized access to product-service: productId={}, status={}, response={}", productId.getValueAsString(), e.getStatusCode(), e.getResponseBodyAsString(),
                    e);
            throw new ProductServiceException(String.format("Product service authentication failed. Please check service account configuration."), e);
        } catch (HttpServerErrorException e) {
            log.error("Product service returned server error: productId={}, status={}, response={}", productId.getValueAsString(), e.getStatusCode(), e.getResponseBodyAsString(),
                    e);
            throw new ProductServiceException(
                    String.format("Product service error (status %s): %s", e.getStatusCode(), e.getResponseBodyAsString() != null ? e.getResponseBodyAsString() : e.getMessage()),
                    e);
        } catch (IllegalArgumentException e) {
            // Handle LoadBalancer service discovery failures
            if (e.getMessage() != null && e.getMessage().contains("Service Instance cannot be null")) {
                log.error(
                        "Product service not found in service registry (Eureka). Please ensure product-service is running and registered with Eureka: productId={}, serviceUrl={}",
                        productId.getValueAsString(), productServiceUrl, e);
                throw new ProductServiceException(String.format(
                        "Product service is not available. The service may not be running or not registered with service discovery. Please contact system administrator."), e);
            }
            log.error("Invalid argument when calling product-service: productId={}, error={}", productId.getValueAsString(), e.getMessage(), e);
            throw new ProductServiceException(String.format("Product service error: %s", e.getMessage()), e);
        } catch (RestClientException e) {
            log.error("Failed to get product by ID from product-service: productId={}, error={}", productId.getValueAsString(), e.getMessage(), e);
            throw new ProductServiceException(String.format("Product service is temporarily unavailable. Please try again later."), e);
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

    /**
     * DTO for product-service barcode validation response.
     */
    private static class ValidateBarcodeResponse {
        private boolean valid;
        private ProductInfoDTO productInfo;

        public boolean isValid() {
            return valid;
        }

        @SuppressWarnings("unused")
        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public ProductInfoDTO getProductInfo() {
            return productInfo;
        }

        @SuppressWarnings("unused")
        public void setProductInfo(ProductInfoDTO productInfo) {
            this.productInfo = productInfo;
        }

        public static class ProductInfoDTO {
            private String productId;
            private String productCode;
            private String description;
            private String barcode;

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

            public String getBarcode() {
                return barcode;
            }

            @SuppressWarnings("unused")
            public void setBarcode(String barcode) {
                this.barcode = barcode;
            }
        }
    }
}

