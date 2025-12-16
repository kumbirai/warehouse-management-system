package com.ccbsa.wms.stockmanagement.application.dto.command;

import java.util.List;

/**
 * Command Result DTO: UploadConsignmentCsvResultDTO
 * <p>
 * API response DTO for CSV upload operation.
 */
public class UploadConsignmentCsvResultDTO {
    private int totalRows;
    private int processedRows;
    private int createdConsignments;
    private int errorRows;
    private List<ConsignmentCsvErrorDTO> errors;

    public UploadConsignmentCsvResultDTO() {
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getProcessedRows() {
        return processedRows;
    }

    public void setProcessedRows(int processedRows) {
        this.processedRows = processedRows;
    }

    public int getCreatedConsignments() {
        return createdConsignments;
    }

    public void setCreatedConsignments(int createdConsignments) {
        this.createdConsignments = createdConsignments;
    }

    public int getErrorRows() {
        return errorRows;
    }

    public void setErrorRows(int errorRows) {
        this.errorRows = errorRows;
    }

    public List<ConsignmentCsvErrorDTO> getErrors() {
        return errors;
    }

    public void setErrors(List<ConsignmentCsvErrorDTO> errors) {
        this.errors = errors;
    }

    /**
     * Nested DTO for CSV errors.
     */
    public static class ConsignmentCsvErrorDTO {
        private long rowNumber;
        private String consignmentReference;
        private String productCode;
        private String errorMessage;

        public ConsignmentCsvErrorDTO() {
        }

        public long getRowNumber() {
            return rowNumber;
        }

        public void setRowNumber(long rowNumber) {
            this.rowNumber = rowNumber;
        }

        public String getConsignmentReference() {
            return consignmentReference;
        }

        public void setConsignmentReference(String consignmentReference) {
            this.consignmentReference = consignmentReference;
        }

        public String getProductCode() {
            return productCode;
        }

        public void setProductCode(String productCode) {
            this.productCode = productCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}

