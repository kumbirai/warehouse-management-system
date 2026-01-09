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
    private Integer processedRows;
    private Integer createdConsignments;  // Matches UploadConsignmentCsvResultDTO field name
    private Integer errorRows;  // Matches UploadConsignmentCsvResultDTO field name
    private List<CsvUploadError> errors;

    // Legacy fields for backward compatibility (if needed by other tests)
    @Deprecated
    private Integer createdCount;
    @Deprecated
    private Integer updatedCount;
    @Deprecated
    private Integer errorCount;

    /**
     * Convenience method to calculate success count (created consignments).
     * For consignment CSV upload, success = createdConsignments (no updates).
     */
    public Integer getSuccessCount() {
        return createdConsignments != null ? createdConsignments : (createdCount != null ? createdCount : 0);
    }

    /**
     * Convenience method for failure count (alias for errorRows/errorCount).
     */
    public Integer getFailureCount() {
        return errorRows != null ? errorRows : (errorCount != null ? errorCount : 0);
    }
}

