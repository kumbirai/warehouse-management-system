package com.ccbsa.wms.product.application.service.command.dto;

import java.util.Objects;

/**
 * DTO: ProductCsvRow
 * <p>
 * Represents a single row from a CSV file for product data.
 */
public final class ProductCsvRow {
    private final long rowNumber;
    private final String productCode;
    private final String description;
    private final String primaryBarcode;
    private final String unitOfMeasure;
    private final String secondaryBarcode; // Optional
    private final String category; // Optional
    private final String brand; // Optional

    private ProductCsvRow(Builder builder) {
        this.rowNumber = builder.rowNumber;
        this.productCode = builder.productCode;
        this.description = builder.description;
        this.primaryBarcode = builder.primaryBarcode;
        this.unitOfMeasure = builder.unitOfMeasure;
        this.secondaryBarcode = builder.secondaryBarcode;
        this.category = builder.category;
        this.brand = builder.brand;
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getRowNumber() {
        return rowNumber;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getDescription() {
        return description;
    }

    public String getPrimaryBarcode() {
        return primaryBarcode;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public String getSecondaryBarcode() {
        return secondaryBarcode;
    }

    public String getCategory() {
        return category;
    }

    public String getBrand() {
        return brand;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProductCsvRow that = (ProductCsvRow) o;
        return rowNumber == that.rowNumber && Objects.equals(productCode, that.productCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rowNumber, productCode);
    }

    public static class Builder {
        private long rowNumber;
        private String productCode;
        private String description;
        private String primaryBarcode;
        private String unitOfMeasure;
        private String secondaryBarcode;
        private String category;
        private String brand;

        public Builder rowNumber(long rowNumber) {
            this.rowNumber = rowNumber;
            return this;
        }

        public Builder productCode(String productCode) {
            this.productCode = productCode;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder primaryBarcode(String primaryBarcode) {
            this.primaryBarcode = primaryBarcode;
            return this;
        }

        public Builder unitOfMeasure(String unitOfMeasure) {
            this.unitOfMeasure = unitOfMeasure;
            return this;
        }

        public Builder secondaryBarcode(String secondaryBarcode) {
            this.secondaryBarcode = secondaryBarcode;
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

        public ProductCsvRow build() {
            if (productCode == null || productCode.trim().isEmpty()) {
                throw new IllegalArgumentException("ProductCode is required");
            }
            if (description == null || description.trim().isEmpty()) {
                throw new IllegalArgumentException("Description is required");
            }
            if (primaryBarcode == null || primaryBarcode.trim().isEmpty()) {
                throw new IllegalArgumentException("PrimaryBarcode is required");
            }
            if (unitOfMeasure == null || unitOfMeasure.trim().isEmpty()) {
                throw new IllegalArgumentException("UnitOfMeasure is required");
            }
            return new ProductCsvRow(this);
        }
    }
}

