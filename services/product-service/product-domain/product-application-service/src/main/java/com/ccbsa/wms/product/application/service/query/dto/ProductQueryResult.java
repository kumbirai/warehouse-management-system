package com.ccbsa.wms.product.application.service.query.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductId;
import com.ccbsa.wms.product.domain.core.valueobject.UnitOfMeasure;

/**
 * Query Result DTO: ProductQueryResult
 * <p>
 * Result object returned from product queries.
 * Contains optimized read model data for product information.
 */
public final class ProductQueryResult {
    private final ProductId productId;
    private final ProductCode productCode;
    private final String description;
    private final ProductBarcode primaryBarcode;
    private final List<ProductBarcode> secondaryBarcodes;
    private final UnitOfMeasure unitOfMeasure;
    private final String category;
    private final String brand;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastModifiedAt;

    private ProductQueryResult(Builder builder) {
        this.productId = builder.productId;
        this.productCode = builder.productCode;
        this.description = builder.description;
        this.primaryBarcode = builder.primaryBarcode;
        this.secondaryBarcodes = builder.secondaryBarcodes != null ? List.copyOf(builder.secondaryBarcodes) : List.of();
        this.unitOfMeasure = builder.unitOfMeasure;
        this.category = builder.category;
        this.brand = builder.brand;
        this.createdAt = builder.createdAt;
        this.lastModifiedAt = builder.lastModifiedAt;
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

    public List<ProductBarcode> getSecondaryBarcodes() {
        return secondaryBarcodes;
    }

    public UnitOfMeasure getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public String getCategory() {
        return category;
    }

    public String getBrand() {
        return brand;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public static class Builder {
        private ProductId productId;
        private ProductCode productCode;
        private String description;
        private ProductBarcode primaryBarcode;
        private List<ProductBarcode> secondaryBarcodes;
        private UnitOfMeasure unitOfMeasure;
        private String category;
        private String brand;
        private LocalDateTime createdAt;
        private LocalDateTime lastModifiedAt;

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

        public Builder secondaryBarcodes(List<ProductBarcode> secondaryBarcodes) {
            this.secondaryBarcodes = secondaryBarcodes != null ? List.copyOf(secondaryBarcodes) : null;
            return this;
        }

        public Builder unitOfMeasure(UnitOfMeasure unitOfMeasure) {
            this.unitOfMeasure = unitOfMeasure;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder brand(String brand) {
            this.brand = brand;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder lastModifiedAt(LocalDateTime lastModifiedAt) {
            this.lastModifiedAt = lastModifiedAt;
            return this;
        }

        public ProductQueryResult build() {
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
            if (unitOfMeasure == null) {
                throw new IllegalArgumentException("UnitOfMeasure is required");
            }
            return new ProductQueryResult(this);
        }
    }
}

