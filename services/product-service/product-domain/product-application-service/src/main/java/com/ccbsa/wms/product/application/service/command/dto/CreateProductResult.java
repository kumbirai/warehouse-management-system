package com.ccbsa.wms.product.application.service.command.dto;

import java.time.LocalDateTime;

import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductId;

/**
 * Result DTO: CreateProductResult
 * <p>
 * Result object returned after creating a product. Contains only the information needed by the caller (not the full domain entity).
 */
public final class CreateProductResult {
    private final ProductId productId;
    private final ProductCode productCode;
    private final String description;
    private final ProductBarcode primaryBarcode;
    private final LocalDateTime createdAt;

    private CreateProductResult(Builder builder) {
        this.productId = builder.productId;
        this.productCode = builder.productCode;
        this.description = builder.description;
        this.primaryBarcode = builder.primaryBarcode;
        this.createdAt = builder.createdAt;
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

    public ProductBarcode getPrimaryBarcode() {
        return primaryBarcode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public static class Builder {
        private ProductId productId;
        private ProductCode productCode;
        private String description;
        private ProductBarcode primaryBarcode;
        private LocalDateTime createdAt;

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

        public Builder primaryBarcode(ProductBarcode primaryBarcode) {
            this.primaryBarcode = primaryBarcode;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public CreateProductResult build() {
            if (productId == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            if (productCode == null) {
                throw new IllegalArgumentException("ProductCode is required");
            }
            if (description == null) {
                throw new IllegalArgumentException("Description is required");
            }
            if (primaryBarcode == null) {
                throw new IllegalArgumentException("PrimaryBarcode is required");
            }
            return new CreateProductResult(this);
        }
    }
}

