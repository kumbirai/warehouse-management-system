package com.ccbsa.wms.product.application.service.command.dto;

import java.util.List;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.product.domain.core.valueobject.UnitOfMeasure;

/**
 * Command DTO: CreateProductCommand
 * <p>
 * Command object for creating a new product.
 */
public final class CreateProductCommand {
    private final TenantId tenantId;
    private final ProductCode productCode;
    private final String description;
    private final ProductBarcode primaryBarcode;
    private final List<ProductBarcode> secondaryBarcodes;
    private final UnitOfMeasure unitOfMeasure;
    private final String category;
    private final String brand;

    private CreateProductCommand(Builder builder) {
        this.tenantId = builder.tenantId;
        this.productCode = builder.productCode;
        this.description = builder.description;
        this.primaryBarcode = builder.primaryBarcode;
        this.secondaryBarcodes = builder.secondaryBarcodes != null ? List.copyOf(builder.secondaryBarcodes) : List.of();
        this.unitOfMeasure = builder.unitOfMeasure;
        this.category = builder.category;
        this.brand = builder.brand;
    }

    public static Builder builder() {
        return new Builder();
    }

    public TenantId getTenantId() {
        return tenantId;
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

    public static class Builder {
        private TenantId tenantId;
        private ProductCode productCode;
        private String description;
        private ProductBarcode primaryBarcode;
        private List<ProductBarcode> secondaryBarcodes;
        private UnitOfMeasure unitOfMeasure;
        private String category;
        private String brand;

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
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

        public CreateProductCommand build() {
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (productCode == null) {
                throw new IllegalArgumentException("ProductCode is required");
            }
            if (description == null || description.trim().isEmpty()) {
                throw new IllegalArgumentException("Description is required");
            }
            if (primaryBarcode == null) {
                throw new IllegalArgumentException("PrimaryBarcode is required");
            }
            if (unitOfMeasure == null) {
                throw new IllegalArgumentException("UnitOfMeasure is required");
            }
            return new CreateProductCommand(this);
        }
    }
}

