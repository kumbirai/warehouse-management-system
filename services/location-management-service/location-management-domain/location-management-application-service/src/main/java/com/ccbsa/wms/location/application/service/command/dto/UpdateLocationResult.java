package com.ccbsa.wms.location.application.service.command.dto;

import java.time.LocalDateTime;

import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCoordinates;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;

/**
 * Result DTO: UpdateLocationResult
 * <p>
 * Result object returned after updating a location.
 */
public final class UpdateLocationResult {
    private final LocationId locationId;
    private final LocationBarcode barcode;
    private final LocationCoordinates coordinates;
    private final LocationStatus status;
    private final String description;
    private final LocalDateTime lastModifiedAt;

    private UpdateLocationResult(Builder builder) {
        this.locationId = builder.locationId;
        this.barcode = builder.barcode;
        this.coordinates = builder.coordinates;
        this.status = builder.status;
        this.description = builder.description;
        this.lastModifiedAt = builder.lastModifiedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public LocationBarcode getBarcode() {
        return barcode;
    }

    public LocationCoordinates getCoordinates() {
        return coordinates;
    }

    public LocationStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public static class Builder {
        private LocationId locationId;
        private LocationBarcode barcode;
        private LocationCoordinates coordinates;
        private LocationStatus status;
        private String description;
        private LocalDateTime lastModifiedAt;

        public Builder locationId(LocationId locationId) {
            this.locationId = locationId;
            return this;
        }

        public Builder barcode(LocationBarcode barcode) {
            this.barcode = barcode;
            return this;
        }

        public Builder coordinates(LocationCoordinates coordinates) {
            this.coordinates = coordinates;
            return this;
        }

        public Builder status(LocationStatus status) {
            this.status = status;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder lastModifiedAt(LocalDateTime lastModifiedAt) {
            this.lastModifiedAt = lastModifiedAt;
            return this;
        }

        public UpdateLocationResult build() {
            return new UpdateLocationResult(this);
        }
    }
}

