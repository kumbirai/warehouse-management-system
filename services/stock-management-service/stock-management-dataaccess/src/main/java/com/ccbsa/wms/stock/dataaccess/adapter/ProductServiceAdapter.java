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

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.stock.application.service.port.service.ProductServicePort;

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

    public ProductServiceAdapter(RestTemplate restTemplate, @Value("${product.service.url:http://product-service:8080}") String productServiceUrl) {
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
            headers.set("X-Tenant-Id", tenantId.getValue());
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
            throw new RuntimeException(String.format("Failed to validate product barcode: %s", e.getMessage()), e);
        } catch (Exception e) {
            logger.error("Unexpected error validating product barcode: barcode={}", barcode, e);
            throw new RuntimeException(String.format("Failed to validate product barcode: %s", e.getMessage()), e);
        }
    }

    @Override
    public Optional<ProductInfo> getProductByCode(ProductCode productCode, TenantId tenantId) {
        logger.debug("Getting product by code from product-service: productCode={}, tenantId={}", productCode.getValue(), tenantId.getValue());

        try {
            String url = String.format("%s/products/by-code/%s", productServiceUrl, productCode.getValue());
            logger.debug("Calling product service: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-Id", tenantId.getValue());
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
            throw new RuntimeException(String.format("Failed to get product by code: %s", e.getMessage()), e);
        } catch (Exception e) {
            logger.error("Unexpected error getting product by code: productCode={}", productCode.getValue(), e);
            throw new RuntimeException(String.format("Failed to get product by code: %s", e.getMessage()), e);
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

