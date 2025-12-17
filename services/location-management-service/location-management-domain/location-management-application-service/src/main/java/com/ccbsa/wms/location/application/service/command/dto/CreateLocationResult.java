package com.ccbsa.wms.location.application.service.command.dto;

import java.time.LocalDateTime;

import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCoordinates;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;

/**
 * Result DTO: CreateLocationResult
 * <p>
 * Result object returned after creating a location. Contains only the information needed by the caller (not the full domain entity).
 */
public final class CreateLocationResult {
    private final LocationId locationId;
    private final LocationBarcode barcode;
    private final LocationCoordinates coordinates;
    private final LocationStatus status;
    private final LocalDateTime createdAt;

    private CreateLocationResult(Builder builder) {
        this.locationId = builder.locationId;
        this.barcode = builder.barcode;
        this.coordinates = builder.coordinates;
        this.status = builder.status;
        this.createdAt = builder.createdAt;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public static class Builder {
        private LocationId locationId;
        private LocationBarcode barcode;
        private LocationCoordinates coordinates;
        private LocationStatus status;
        private LocalDateTime createdAt;

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

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public CreateLocationResult build() {
            if (locationId == null) {
                throw new IllegalArgumentException("LocationId is required");
            }
            if (barcode == null) {
                throw new IllegalArgumentException("LocationBarcode is required");
            }
            if (coordinates == null) {
                throw new IllegalArgumentException("LocationCoordinates is required");
            }
            if (status == null) {
                throw new IllegalArgumentException("LocationStatus is required");
            }
            return new CreateLocationResult(this);
        }
    }
}

