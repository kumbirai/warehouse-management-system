package com.ccbsa.wms.stockmanagement.application.service.command.dto;

/**
 * CSV Error DTO: ConsignmentCsvError
 * <p>
 * Represents an error in a CSV row during parsing or validation.
 */
public final class ConsignmentCsvError {
    private final long rowNumber;
    private final String consignmentReference;
    private final String productCode;
    private final String errorMessage;

    private ConsignmentCsvError(Builder builder) {
        this.rowNumber = builder.rowNumber;
        this.consignmentReference = builder.consignmentReference;
        this.productCode = builder.productCode;
        this.errorMessage = builder.errorMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getRowNumber() {
        return rowNumber;
    }

    public String getConsignmentReference() {
        return consignmentReference;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static class Builder {
        private long rowNumber;
        private String consignmentReference;
        private String productCode;
        private String errorMessage;

        public Builder rowNumber(long rowNumber) {
            this.rowNumber = rowNumber;
            return this;
        }

        public Builder consignmentReference(String consignmentReference) {
            this.consignmentReference = consignmentReference;
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

        public ConsignmentCsvError build() {
            if (errorMessage == null || errorMessage.trim()
                    .isEmpty()) {
                throw new IllegalArgumentException("ErrorMessage is required");
            }
            return new ConsignmentCsvError(this);
        }
    }
}

