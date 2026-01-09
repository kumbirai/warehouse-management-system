package com.ccbsa.wms.picking.application.service.command.dto;

import java.util.Collections;
import java.util.List;

import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Result DTO: CsvUploadResult
 * <p>
 * Result object returned after CSV upload processing.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor and getters return defensive copies")
public final class CsvUploadResult {
    private final int totalRows;
    private final int successfulRows;
    private final int errorRows;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor")
    private final List<PickingListId> createdPickingListIds;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor")
    private final List<CsvValidationError> errors;

    public CsvUploadResult(int totalRows, int successfulRows, int errorRows, List<PickingListId> createdPickingListIds, List<CsvValidationError> errors) {
        this.totalRows = totalRows;
        this.successfulRows = successfulRows;
        this.errorRows = errorRows;
        this.createdPickingListIds = createdPickingListIds != null ? List.copyOf(createdPickingListIds) : List.of();
        this.errors = errors != null ? List.copyOf(errors) : List.of();
    }

    /**
     * Returns a defensive copy of the created picking list IDs list to prevent external modification.
     *
     * @return unmodifiable copy of the created picking list IDs list
     */
    public List<PickingListId> getCreatedPickingListIds() {
        return Collections.unmodifiableList(createdPickingListIds);
    }

    /**
     * Returns a defensive copy of the errors list to prevent external modification.
     *
     * @return unmodifiable copy of the errors list
     */
    public List<CsvValidationError> getErrors() {
        return Collections.unmodifiableList(errors);
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
