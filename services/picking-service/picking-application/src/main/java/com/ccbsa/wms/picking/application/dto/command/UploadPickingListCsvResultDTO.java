package com.ccbsa.wms.picking.application.dto.command;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * Result DTO: UploadPickingListCsvResultDTO
 * <p>
 * DTO for CSV upload result.
 */
@Getter
@Builder
public class UploadPickingListCsvResultDTO {
    private final int totalRows;
    private final int successfulRows;
    private final int errorRows;
    private final List<String> createdPickingListIds;
    private final List<CsvValidationErrorDTO> errors;

    @Getter
    @Builder
    public static class CsvValidationErrorDTO {
        private final int rowNumber;
        private final String fieldName;
        private final String errorMessage;
        private final String invalidValue;
    }
}
