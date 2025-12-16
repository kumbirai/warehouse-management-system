package com.ccbsa.wms.stockmanagement.application.dto.command;

import java.util.List;

/**
 * Command Result DTO: ValidateConsignmentResultDTO
 * <p>
 * API response DTO for consignment validation.
 */
public class ValidateConsignmentResultDTO {
    private boolean valid;
    private List<String> validationErrors;

    public ValidateConsignmentResultDTO() {
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<String> validationErrors) {
        this.validationErrors = validationErrors;
    }
}

