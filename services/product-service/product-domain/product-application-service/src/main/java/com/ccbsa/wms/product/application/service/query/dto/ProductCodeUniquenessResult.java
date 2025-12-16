package com.ccbsa.wms.product.application.service.query.dto;

import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;

/**
 * Query Result DTO: ProductCodeUniquenessResult
 * <p>
 * Result object returned from product code uniqueness check.
 */
public final class ProductCodeUniquenessResult {
    private final ProductCode productCode;
    private final boolean isUnique;

    private ProductCodeUniquenessResult(Builder builder) {
        this.productCode = builder.productCode;
        this.isUnique = builder.isUnique;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ProductCode getProductCode() {
        return productCode;
    }

    public boolean isUnique() {
        return isUnique;
    }

    public static class Builder {
        private ProductCode productCode;
        private boolean isUnique;

        public Builder productCode(ProductCode productCode) {
            this.productCode = productCode;
            return this;
        }

        public Builder isUnique(boolean isUnique) {
            this.isUnique = isUnique;
            return this;
        }

        public ProductCodeUniquenessResult build() {
            if (productCode == null) {
                throw new IllegalArgumentException("ProductCode is required");
            }
            return new ProductCodeUniquenessResult(this);
        }
    }
}

