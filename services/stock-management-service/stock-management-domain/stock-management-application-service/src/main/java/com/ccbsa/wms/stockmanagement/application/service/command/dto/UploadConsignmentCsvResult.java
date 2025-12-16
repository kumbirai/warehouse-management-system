package com.ccbsa.wms.stockmanagement.application.service.command.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Command Result DTO: UploadConsignmentCsvResult
 * <p>
 * Result object returned from UploadConsignmentCsvCommand execution.
 */
public final class UploadConsignmentCsvResult {
    private final int totalRows;
    private final int processedRows;
    private final int createdConsignments;
    private final int errorRows;
    private final List<ConsignmentCsvError> errors;

    private UploadConsignmentCsvResult(Builder builder) {
        this.totalRows = builder.totalRows;
        this.processedRows = builder.processedRows;
        this.createdConsignments = builder.createdConsignments;
        this.errorRows = builder.errorRows;
        this.errors = builder.errors != null ? List.copyOf(builder.errors) : List.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getProcessedRows() {
        return processedRows;
    }

    public int getCreatedConsignments() {
        return createdConsignments;
    }

    public int getErrorRows() {
        return errorRows;
    }

    public List<ConsignmentCsvError> getErrors() {
        return errors;
    }

    public static class Builder {
        private int totalRows;
        private int processedRows;
        private int createdConsignments;
        private int errorRows;
        private List<ConsignmentCsvError> errors;

        public Builder totalRows(int totalRows) {
            this.totalRows = totalRows;
            return this;
        }

        public Builder processedRows(int processedRows) {
            this.processedRows = processedRows;
            return this;
        }

        public Builder createdConsignments(int createdConsignments) {
            this.createdConsignments = createdConsignments;
            return this;
        }

        public Builder errorRows(int errorRows) {
            this.errorRows = errorRows;
            return this;
        }

        public Builder errors(List<ConsignmentCsvError> errors) {
            this.errors = errors != null ? new ArrayList<>(errors) : null;
            return this;
        }

        public UploadConsignmentCsvResult build() {
            return new UploadConsignmentCsvResult(this);
        }
    }
}

