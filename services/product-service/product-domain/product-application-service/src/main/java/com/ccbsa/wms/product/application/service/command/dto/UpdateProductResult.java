package com.ccbsa.wms.product.application.service.command.dto;

import java.time.LocalDateTime;

import com.ccbsa.wms.product.domain.core.valueobject.ProductId;

/**
 * Result DTO: UpdateProductResult
 * <p>
 * Result object returned after updating a product.
 */
public final class UpdateProductResult {
    private final ProductId productId;
    private final LocalDateTime lastModifiedAt;

    private UpdateProductResult(Builder builder) {
        this.productId = builder.productId;
        this.lastModifiedAt = builder.lastModifiedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ProductId getProductId() {
        return productId;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public static class Builder {
        private ProductId productId;
        private LocalDateTime lastModifiedAt;

        public Builder productId(ProductId productId) {
            this.productId = productId;
            return this;
        }

        public Builder lastModifiedAt(LocalDateTime lastModifiedAt) {
            this.lastModifiedAt = lastModifiedAt;
            return this;
        }

        public UpdateProductResult build() {
            if (productId == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            return new UpdateProductResult(this);
        }
    }
}

