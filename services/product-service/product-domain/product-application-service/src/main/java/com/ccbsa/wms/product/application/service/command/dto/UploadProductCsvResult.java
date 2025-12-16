package com.ccbsa.wms.product.application.service.command.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Result DTO: UploadProductCsvResult
 * <p>
 * Result object returned after uploading a CSV file.
 * Contains summary statistics and error details.
 */
public final class UploadProductCsvResult {
    private final int totalRows;
    private final int createdCount;
    private final int updatedCount;
    private final int errorCount;
    private final List<ProductCsvError> errors;

    private UploadProductCsvResult(Builder builder) {
        this.totalRows = builder.totalRows;
        this.createdCount = builder.createdCount;
        this.updatedCount = builder.updatedCount;
        this.errorCount = builder.errors != null ? builder.errors.size() : 0;
        this.errors = builder.errors != null ? List.copyOf(builder.errors) : List.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getCreatedCount() {
        return createdCount;
    }

    public int getUpdatedCount() {
        return updatedCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public List<ProductCsvError> getErrors() {
        return errors;
    }

    public static class Builder {
        private int totalRows;
        private int createdCount;
        private int updatedCount;
        private List<ProductCsvError> errors;

        public Builder totalRows(int totalRows) {
            this.totalRows = totalRows;
            return this;
        }

        public Builder createdCount(int createdCount) {
            this.createdCount = createdCount;
            return this;
        }

        public Builder updatedCount(int updatedCount) {
            this.updatedCount = updatedCount;
            return this;
        }

        public Builder errors(List<ProductCsvError> errors) {
            this.errors = errors != null ? List.copyOf(errors) : null;
            return this;
        }

        public Builder addError(ProductCsvError error) {
            if (this.errors == null) {
                this.errors = new ArrayList<>();
            }
            this.errors.add(error);
            return this;
        }

        public UploadProductCsvResult build() {
            return new UploadProductCsvResult(this);
        }
    }
}

