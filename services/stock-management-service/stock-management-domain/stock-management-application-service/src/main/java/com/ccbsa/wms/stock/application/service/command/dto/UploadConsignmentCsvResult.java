package com.ccbsa.wms.stock.application.service.command.dto;

import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Command Result DTO: UploadConsignmentCsvResult
 * <p>
 * Result object returned from UploadConsignmentCsvCommand execution.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Defensive copy made in constructor and getter returns immutable view.")
public final class UploadConsignmentCsvResult {
    private final int totalRows;
    private final int processedRows;
    private final int createdConsignments;
    private final int errorRows;
    private final List<ConsignmentCsvError> errors;

    public UploadConsignmentCsvResult(int totalRows, int processedRows, int createdConsignments, int errorRows, List<ConsignmentCsvError> errors) {
        this.totalRows = totalRows;
        this.processedRows = processedRows;
        this.createdConsignments = createdConsignments;
        this.errorRows = errorRows;
        // Defensive copy to prevent external modification
        this.errors = errors != null ? List.copyOf(errors) : List.of();
    }

    /**
     * Returns an unmodifiable view of the errors list.
     *
     * @return Unmodifiable list of errors
     */
    public List<ConsignmentCsvError> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}

