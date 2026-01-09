package com.ccbsa.wms.picking.application.service.command.dto;

import java.util.List;

import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;

import lombok.Builder;
import lombok.Getter;

/**
 * Result DTO: CsvUploadResult
 * <p>
 * Result object returned after CSV upload processing.
 */
@Getter
@Builder
public final class CsvUploadResult {
    private final int totalRows;
    private final int successfulRows;
    private final int errorRows;
    private final List<PickingListId> createdPickingListIds;
    private final List<CsvValidationError> errors;

    public CsvUploadResult(int totalRows, int successfulRows, int errorRows, List<PickingListId> createdPickingListIds, List<CsvValidationError> errors) {
        this.totalRows = totalRows;
        this.successfulRows = successfulRows;
        this.errorRows = errorRows;
        this.createdPickingListIds = createdPickingListIds != null ? List.copyOf(createdPickingListIds) : List.of();
        this.errors = errors != null ? List.copyOf(errors) : List.of();
    }

    /**
     * DTO: CsvValidationError
     * <p>
     * Represents a validation error in CSV processing.
     */
    @Getter
    @Builder
    public static final class CsvValidationError {
        private final int rowNumber;
        private final String fieldName;
        private final String errorMessage;
        private final String invalidValue;

        public CsvValidationError(int rowNumber, String fieldName, String errorMessage, String invalidValue) {
            this.rowNumber = rowNumber;
            this.fieldName = fieldName;
            this.errorMessage = errorMessage;
            this.invalidValue = invalidValue;
        }
    }
}
