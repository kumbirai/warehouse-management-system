package com.ccbsa.wms.location.application.service.command.dto;

import java.time.LocalDateTime;

import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

/**
 * Result DTO: UpdateLocationStatusResult
 * <p>
 * Result object for location status update operation.
 */
public final class UpdateLocationStatusResult {
    private final LocationId locationId;
    private final String status;
    private final LocalDateTime lastModifiedAt;

    private UpdateLocationStatusResult(Builder builder) {
        this.locationId = builder.locationId;
        this.status = builder.status;
        this.lastModifiedAt = builder.lastModifiedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public static class Builder {
        private LocationId locationId;
        private String status;
        private LocalDateTime lastModifiedAt;

        public Builder locationId(LocationId locationId) {
            this.locationId = locationId;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder lastModifiedAt(LocalDateTime lastModifiedAt) {
            this.lastModifiedAt = lastModifiedAt;
            return this;
        }

        public UpdateLocationStatusResult build() {
            return new UpdateLocationStatusResult(this);
        }
    }
}

