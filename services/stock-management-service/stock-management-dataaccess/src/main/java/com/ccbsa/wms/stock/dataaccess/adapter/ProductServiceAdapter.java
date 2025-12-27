package com.ccbsa.wms.stock.dataaccess.adapter;

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
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.stock.application.service.exception.ProductServiceException;
import com.ccbsa.wms.stock.application.service.port.service.ProductServicePort;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Adapter: ProductServiceAdapter
 * <p>
 * Implements ProductServicePort for retrieving product information from product-service. Calls product-service REST API to validate product codes and barcodes.
 */
@Component
public class ProductServiceAdapter implements ProductServicePort {
    private static final Logger logger = LoggerFactory.getLogger(ProductServiceAdapter.class);

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
        logger.debug("Validating product barcode from product-service: barcode={}, tenantId={}", barcode, tenantId.getValue());

        try {
            String url = String.format("%s/products/validate-barcode?barcode=%s", productServiceUrl, barcode);
            logger.debug("Calling product service: {}", url);

            HttpHeaders headers = new HttpHeaders();

            // Forward Authorization header from current request for service-to-service authentication
            String authorizationHeader = getAuthorizationHeader();
            if (authorizationHeader != null) {
                headers.set("Authorization", authorizationHeader);
                logger.debug("Forwarding Authorization header to product service");
            } else {
                logger.warn("No Authorization header found in current request - product service call may fail");
            }

            // Forward X-Tenant-Id header (required by product service)
            String tenantIdHeader = getTenantIdHeader();
            if (tenantIdHeader != null) {
                headers.set("X-Tenant-Id", tenantIdHeader);
                logger.debug("Forwarding X-Tenant-Id header to product service: {}", tenantIdHeader);
            } else {
                // Set the tenantId from the method parameter as fallback
                headers.set("X-Tenant-Id", tenantId.getValue());
                logger.debug("Setting X-Tenant-Id header from method parameter: {}", tenantId.getValue());
            }

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<ValidateBarcodeResponse>> response = restTemplate.exchange(url, HttpMethod.GET, entity, BARCODE_RESPONSE_TYPE);

            ApiResponse<ValidateBarcodeResponse> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                ValidateBarcodeResponse barcodeResponse = responseBody.getData();
                if (barcodeResponse.isValid() && barcodeResponse.getProductInfo() != null) {
                    ValidateBarcodeResponse.ProductInfoDTO productInfoDTO = barcodeResponse.getProductInfo();
                    ProductInfo productInfo =
                            new ProductInfo(productInfoDTO.getProductId(), productInfoDTO.getProductCode(), productInfoDTO.getDescription(), productInfoDTO.getBarcode());
                    logger.debug("Product barcode validated: productId={}, productCode={}", productInfoDTO.getProductId(), productInfoDTO.getProductCode());
                    return Optional.of(productInfo);
                }
            }

            return Optional.empty();
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("Product barcode not found: {}", barcode);
            return Optional.empty();
        } catch (RestClientException e) {
            logger.error("Failed to validate product barcode from product-service: barcode={}", barcode, e);
            throw new ProductServiceException(String.format("Product service is temporarily unavailable. Please try again later."), e);
        } catch (Exception e) {
            logger.error("Unexpected error validating product barcode: barcode={}", barcode, e);
            throw new ProductServiceException(String.format("Product service error: %s", e.getMessage()), e);
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
                return request.getHeader("X-Tenant-Id");
            }
        } catch (Exception e) {
            logger.debug("Could not extract X-Tenant-Id header from request context: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public Optional<ProductInfo> getProductByCode(ProductCode productCode, TenantId tenantId) {
        logger.debug("Getting product by code from product-service: productCode={}, tenantId={}", productCode.getValue(), tenantId.getValue());

        try {
            String url = String.format("%s/products/by-code/%s", productServiceUrl, productCode.getValue());
            logger.debug("Calling product service: {}", url);

            HttpHeaders headers = new HttpHeaders();

            // Forward Authorization header from current request for service-to-service authentication
            String authorizationHeader = getAuthorizationHeader();
            if (authorizationHeader != null) {
                headers.set("Authorization", authorizationHeader);
                logger.debug("Forwarding Authorization header to product service");
            } else {
                logger.warn("No Authorization header found in current request - product service call may fail");
            }

            // Forward X-Tenant-Id header (required by product service)
            String tenantIdHeader = getTenantIdHeader();
            if (tenantIdHeader != null) {
                headers.set("X-Tenant-Id", tenantIdHeader);
                logger.debug("Forwarding X-Tenant-Id header to product service: {}", tenantIdHeader);
            } else {
                // Set the tenantId from the method parameter as fallback
                headers.set("X-Tenant-Id", tenantId.getValue());
                logger.debug("Setting X-Tenant-Id header from method parameter: {}", tenantId.getValue());
            }

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<ProductResponse>> response = restTemplate.exchange(url, HttpMethod.GET, entity, PRODUCT_RESPONSE_TYPE);

            ApiResponse<ProductResponse> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                ProductResponse productResponse = responseBody.getData();
                ProductInfo productInfo =
                        new ProductInfo(productResponse.getProductId(), productResponse.getProductCode(), productResponse.getDescription(), productResponse.getPrimaryBarcode());
                logger.debug("Product retrieved by code: productId={}, productCode={}", productResponse.getProductId(), productResponse.getProductCode());
                return Optional.of(productInfo);
            }

            return Optional.empty();
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("Product not found by code: {}", productCode.getValue());
            return Optional.empty();
        } catch (RestClientException e) {
            logger.error("Failed to get product by code from product-service: productCode={}", productCode.getValue(), e);
            throw new ProductServiceException(String.format("Product service is temporarily unavailable. Please try again later."), e);
        } catch (Exception e) {
            logger.error("Unexpected error getting product by code: productCode={}", productCode.getValue(), e);
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

