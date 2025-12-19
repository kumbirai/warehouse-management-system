package com.ccbsa.wms.location.application.service.query.dto;

import java.time.LocalDateTime;

import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCapacity;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCoordinates;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;

/**
 * Query Result DTO: LocationQueryResult
 * <p>
 * Result object returned from location queries. Contains optimized read model data for location information.
 */
public final class LocationQueryResult {
    private final LocationId locationId;
    private final LocationBarcode barcode;
    private final LocationCoordinates coordinates;
    private final LocationStatus status;
    private final LocationCapacity capacity;
    private final String code;
    private final String name;
    private final String type;
    private final String path;
    private final String description;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastModifiedAt;

    private LocationQueryResult(Builder builder) {
        this.locationId = builder.locationId;
        this.barcode = builder.barcode;
        this.coordinates = builder.coordinates;
        this.status = builder.status;
        this.capacity = builder.capacity;
        this.code = builder.code;
        this.name = builder.name;
        this.type = builder.type;
        this.path = builder.path;
        this.description = builder.description;
        this.createdAt = builder.createdAt;
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

    public LocationCapacity getCapacity() {
        return capacity;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public static class Builder {
        private LocationId locationId;
        private LocationBarcode barcode;
        private LocationCoordinates coordinates;
        private LocationStatus status;
        private LocationCapacity capacity;
        private String code;
        private String name;
        private String type;
        private String path;
        private String description;
        private LocalDateTime createdAt;
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

        public Builder capacity(LocationCapacity capacity) {
            this.capacity = capacity;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder lastModifiedAt(LocalDateTime lastModifiedAt) {
            this.lastModifiedAt = lastModifiedAt;
            return this;
        }

        public LocationQueryResult build() {
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
            return new LocationQueryResult(this);
        }
    }
}

