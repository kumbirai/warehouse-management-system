package com.ccbsa.wms.product.application.service.command.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * DTO: ProductCsvError
 * <p>
 * Represents an error encountered while processing a CSV row.
 */
@Getter
@Builder
@EqualsAndHashCode
public final class ProductCsvError {
    private final long rowNumber;
    private final String productCode;
    private final String errorMessage;

    public ProductCsvError(long rowNumber, String productCode, String errorMessage) {
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("ErrorMessage is required");
        }
        this.rowNumber = rowNumber;
        this.productCode = productCode;
        this.errorMessage = errorMessage;
    }
}

