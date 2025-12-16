package com.ccbsa.wms.product.application.dto.command;

/**
 * Error DTO: ProductCsvErrorDTO
 * <p>
 * Represents an error encountered while processing a CSV row.
 */
public final class ProductCsvErrorDTO {
    private long rowNumber;
    private String productCode;
    private String errorMessage;

    public long getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(long rowNumber) {
        this.rowNumber = rowNumber;
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

