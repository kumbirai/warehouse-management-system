package com.ccbsa.wms.location.application.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Command DTO: UpdateLocationStatusCommandDTO
 * <p>
 * Request DTO for updating a location's status.
 */
public final class UpdateLocationStatusCommandDTO {
    @NotBlank(message = "Status is required")
    @Size(max = 50, message = "Status must not exceed 50 characters")
    private String status;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;

    public UpdateLocationStatusCommandDTO() {
    }

    public UpdateLocationStatusCommandDTO(String status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

