package com.ccbsa.wms.location.application.dto.command;

import java.time.LocalDateTime;

/**
 * Result DTO: UpdateLocationStatusResultDTO
 * <p>
 * Response DTO for location status update operation.
 */
public final class UpdateLocationStatusResultDTO {
    private String locationId;
    private String status;
    private LocalDateTime lastModifiedAt;

    public UpdateLocationStatusResultDTO() {
    }

    public UpdateLocationStatusResultDTO(String locationId, String status, LocalDateTime lastModifiedAt) {
        this.locationId = locationId;
        this.status = status;
        this.lastModifiedAt = lastModifiedAt;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }
}

