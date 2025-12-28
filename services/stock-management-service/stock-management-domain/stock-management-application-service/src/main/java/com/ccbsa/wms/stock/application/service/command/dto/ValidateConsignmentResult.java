package com.ccbsa.wms.stock.application.service.command.dto;

import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Command Result DTO: ValidateConsignmentResult
 * <p>
 * Result object returned from ValidateConsignmentCommand execution.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Defensive copy made in constructor and getter returns immutable view.")
public final class ValidateConsignmentResult {
    private final boolean valid;
    private final List<String> validationErrors;

    public ValidateConsignmentResult(boolean valid, List<String> validationErrors) {
        this.valid = valid;
        // Defensive copy to prevent external modification
        this.validationErrors = validationErrors != null ? List.copyOf(validationErrors) : List.of();
    }

    /**
     * Returns an unmodifiable view of the validation errors list.
     *
     * @return Unmodifiable list of validation errors
     */
    public List<String> getValidationErrors() {
        return Collections.unmodifiableList(validationErrors);
    }
}

