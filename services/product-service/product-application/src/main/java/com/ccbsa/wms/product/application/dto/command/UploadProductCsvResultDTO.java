package com.ccbsa.wms.product.application.dto.command;

import java.util.List;

/**
 * Result DTO: UploadProductCsvResultDTO
 * <p>
 * API response DTO for CSV upload result.
 */
public final class UploadProductCsvResultDTO {
    private int totalRows;
    private int createdCount;
    private int updatedCount;
    private int errorCount;
    private List<ProductCsvErrorDTO> errors;

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(int createdCount) {
        this.createdCount = createdCount;
    }

    public int getUpdatedCount() {
        return updatedCount;
    }

    public void setUpdatedCount(int updatedCount) {
        this.updatedCount = updatedCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public List<ProductCsvErrorDTO> getErrors() {
        return errors;
    }

    public void setErrors(List<ProductCsvErrorDTO> errors) {
        this.errors = errors;
    }
}

