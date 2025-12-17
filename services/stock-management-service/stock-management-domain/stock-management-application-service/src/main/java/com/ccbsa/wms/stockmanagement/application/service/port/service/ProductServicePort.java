package com.ccbsa.wms.stockmanagement.application.service.port.service;

import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;

/**
 * Service Port: ProductServicePort
 * <p>
 * Defines the contract for Product Service integration. Implemented by infrastructure adapters (REST client).
 * <p>
 * This port allows Stock Management Service to query Product Service for product validation without direct coupling.
 */
public interface ProductServicePort {
    /**
     * Validates a product barcode and returns product information if found.
     *
     * @param barcode  Product barcode to validate
     * @param tenantId Tenant identifier
     * @return Optional ProductInfo if product found, empty otherwise
     */
    Optional<ProductInfo> validateProductBarcode(String barcode, TenantId tenantId);

    /**
     * Gets product information by product code.
     *
     * @param productCode Product code
     * @param tenantId    Tenant identifier
     * @return Optional ProductInfo if product found, empty otherwise
     */
    Optional<ProductInfo> getProductByCode(ProductCode productCode, TenantId tenantId);

    /**
     * Product information DTO. Lightweight representation of product data for validation.
     */
    class ProductInfo {
        private final String productId;
        private final String productCode;
        private final String description;
        private final String barcode;

        public ProductInfo(String productId, String productCode, String description, String barcode) {
            this.productId = productId;
            this.productCode = productCode;
            this.description = description;
            this.barcode = barcode;
        }

        public String getProductId() {
            return productId;
        }

        public String getProductCode() {
            return productCode;
        }

        public String getDescription() {
            return description;
        }

        public String getBarcode() {
            return barcode;
        }
    }
}

