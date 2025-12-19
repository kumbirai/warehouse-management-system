package com.ccbsa.wms.stock.application.service.command.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Command Result DTO: ValidateConsignmentResult
 * <p>
 * Result object returned from ValidateConsignmentCommand execution.
 */
public final class ValidateConsignmentResult {
    private final boolean valid;
    private final List<String> validationErrors;

    private ValidateConsignmentResult(Builder builder) {
        this.valid = builder.valid;
        this.validationErrors = builder.validationErrors != null ? List.copyOf(builder.validationErrors) : List.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public static class Builder {
        private boolean valid;
        private List<String> validationErrors;

        public Builder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public Builder validationErrors(List<String> validationErrors) {
            this.validationErrors = validationErrors != null ? new ArrayList<>(validationErrors) : null;
            return this;
        }

        public ValidateConsignmentResult build() {
            return new ValidateConsignmentResult(this);
        }
    }
}

