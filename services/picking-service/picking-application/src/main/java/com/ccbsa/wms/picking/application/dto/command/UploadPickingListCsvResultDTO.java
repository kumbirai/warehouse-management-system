package com.ccbsa.wms.picking.application.dto.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Result DTO: UploadPickingListCsvResultDTO
 * <p>
 * DTO for CSV upload result.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder, and getters return defensive copies")
public class UploadPickingListCsvResultDTO {
    private final int totalRows;
    private final int successfulRows;
    private final int errorRows;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder")
    private final List<String> createdPickingListIds;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder")
    private final List<CsvValidationErrorDTO> errors;

    /**
     * Returns a defensive copy of the created picking list IDs list to prevent external modification.
     *
     * @return unmodifiable copy of the created picking list IDs list
     */
    public List<String> getCreatedPickingListIds() {
        if (createdPickingListIds == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(createdPickingListIds));
    }

    /**
     * Returns a defensive copy of the errors list to prevent external modification.
     *
     * @return unmodifiable copy of the errors list
     */
    public List<CsvValidationErrorDTO> getErrors() {
        if (errors == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(errors));
    }

    @Getter
    @Builder
    public static class CsvValidationErrorDTO {
        private final int rowNumber;
        private final String fieldName;
        private final String errorMessage;
        private final String invalidValue;
    }
}
