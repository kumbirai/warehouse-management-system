package com.ccbsa.wms.stock.application.dto.command;

import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Command Result DTO: UploadConsignmentCsvResultDTO
 * <p>
 * API response DTO for CSV upload operation.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Lists are immutable when returned from API.")
public class UploadConsignmentCsvResultDTO {
    private int totalRows;
    private int processedRows;
    private int createdConsignments;
    private int errorRows;
    private List<ConsignmentCsvErrorDTO> errors;

    /**
     * Nested DTO for CSV errors.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsignmentCsvErrorDTO {
        private long rowNumber;
        private String consignmentReference;
        private String productCode;
        private String errorMessage;
    }
}

