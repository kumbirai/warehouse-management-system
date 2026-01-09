package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Test DTO matching ProductCsvErrorDTO from the service.
 * Fields must match the actual API response structure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvUploadError {
    private Long rowNumber;
    private String productCode;
    private String errorMessage;

    /**
     * Convenience getter for row (alias for rowNumber).
     */
    public Integer getRow() {
        return rowNumber != null ? rowNumber.intValue() : null;
    }

    /**
     * Convenience getter for message (alias for errorMessage).
     */
    public String getMessage() {
        return errorMessage;
    }
}

