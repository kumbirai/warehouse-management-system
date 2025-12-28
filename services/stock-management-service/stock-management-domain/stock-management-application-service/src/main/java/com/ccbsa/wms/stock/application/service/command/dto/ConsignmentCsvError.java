package com.ccbsa.wms.stock.application.service.command.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * CSV Error DTO: ConsignmentCsvError
 * <p>
 * Represents an error in a CSV row during parsing or validation.
 */
@Getter
@Builder
public final class ConsignmentCsvError {
    private final long rowNumber;
    private final String consignmentReference;
    private final String productCode;
    private final String errorMessage;

    public ConsignmentCsvError(long rowNumber, String consignmentReference, String productCode, String errorMessage) {
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("ErrorMessage is required");
        }
        this.rowNumber = rowNumber;
        this.consignmentReference = consignmentReference;
        this.productCode = productCode;
        this.errorMessage = errorMessage;
    }
}

