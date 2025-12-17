package com.ccbsa.wms.product.application.service.command.dto;

import java.util.Objects;

/**
 * DTO: ProductCsvError
 * <p>
 * Represents an error encountered while processing a CSV row.
 */
public final class ProductCsvError {
    private final long rowNumber;
    private final String productCode;
    private final String errorMessage;

    private ProductCsvError(Builder builder) {
        this.rowNumber = builder.rowNumber;
        this.productCode = builder.productCode;
        this.errorMessage = builder.errorMessage;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProductCsvError that = (ProductCsvError) o;
        return rowNumber == that.rowNumber && Objects.equals(productCode, that.productCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rowNumber, productCode);
    }

    public static class Builder {
        private long rowNumber;
        private String productCode;
        private String errorMessage;

        public Builder rowNumber(long rowNumber) {
            this.rowNumber = rowNumber;
            return this;
        }

        public Builder productCode(String productCode) {
            this.productCode = productCode;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public ProductCsvError build() {
            if (errorMessage == null || errorMessage.trim()
                    .isEmpty()) {
                throw new IllegalArgumentException("ErrorMessage is required");
            }
            return new ProductCsvError(this);
        }
    }
}

