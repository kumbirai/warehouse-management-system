package com.ccbsa.wms.product.application.service.query.dto;

import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductId;

/**
 * Product Info DTO
 * <p>
 * Lightweight product information for barcode validation results.
 * Used in caching and query results.
 */
public final class ProductInfo {
    private final ProductId productId;
    private final ProductCode productCode;
    private final String description;
    private final ProductBarcode barcode;

    private ProductInfo(Builder builder) {
        this.productId = builder.productId;
        this.productCode = builder.productCode;
        this.description = builder.description;
        this.barcode = builder.barcode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ProductId getProductId() {
        return productId;
    }

    public ProductCode getProductCode() {
        return productCode;
    }

    public String getDescription() {
        return description;
    }

    public ProductBarcode getBarcode() {
        return barcode;
    }

    public static class Builder {
        private ProductId productId;
        private ProductCode productCode;
        private String description;
        private ProductBarcode barcode;

        public Builder productId(ProductId productId) {
            this.productId = productId;
            return this;
        }

        public Builder productCode(ProductCode productCode) {
            this.productCode = productCode;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder barcode(ProductBarcode barcode) {
            this.barcode = barcode;
            return this;
        }

        public ProductInfo build() {
            if (productId == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            if (productCode == null) {
                throw new IllegalArgumentException("ProductCode is required");
            }
            if (description == null) {
                throw new IllegalArgumentException("Description is required");
            }
            if (barcode == null) {
                throw new IllegalArgumentException("Barcode is required");
            }
            return new ProductInfo(this);
        }
    }
}

