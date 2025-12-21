package com.ccbsa.wms.gateway.api.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Test DTO matching UploadProductCsvResultDTO from the service.
 * Fields must match the actual API response structure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvUploadResponse {
    private Integer totalRows;
    private Integer createdCount;
    private Integer updatedCount;
    private Integer errorCount;
    private List<CsvUploadError> errors;
    
    /**
     * Convenience method to calculate success count (created + updated).
     */
    public Integer getSuccessCount() {
        if (createdCount == null || updatedCount == null) {
            return null;
        }
        return createdCount + updatedCount;
    }
    
    /**
     * Convenience method for failure count (alias for errorCount).
     */
    public Integer getFailureCount() {
        return errorCount;
    }
}

