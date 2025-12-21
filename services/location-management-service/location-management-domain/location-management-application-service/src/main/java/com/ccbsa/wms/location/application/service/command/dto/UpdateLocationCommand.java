package com.ccbsa.wms.location.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCoordinates;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

/**
 * Command DTO: UpdateLocationCommand
 * <p>
 * Command object for updating an existing warehouse location.
 */
public final class UpdateLocationCommand {
    private final LocationId locationId;
    private final TenantId tenantId;
    private final LocationCoordinates coordinates;
    private final LocationBarcode barcode; // Optional - can be null if not updating
    private final String description; // Optional - can be null if not updating

    private UpdateLocationCommand(Builder builder) {
        this.locationId = builder.locationId;
        this.tenantId = builder.tenantId;
        this.coordinates = builder.coordinates;
        this.barcode = builder.barcode;
        this.description = builder.description;
    }

    public static Builder builder() {
        return new Builder();
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public LocationCoordinates getCoordinates() {
        return coordinates;
    }

    public LocationBarcode getBarcode() {
        return barcode;
    }

    public String getDescription() {
        return description;
    }

    public static class Builder {
        private LocationId locationId;
        private TenantId tenantId;
        private LocationCoordinates coordinates;
        private LocationBarcode barcode;
        private String description;

        public Builder locationId(LocationId locationId) {
            this.locationId = locationId;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder coordinates(LocationCoordinates coordinates) {
            this.coordinates = coordinates;
            return this;
        }

        public Builder barcode(LocationBarcode barcode) {
            this.barcode = barcode;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public UpdateLocationCommand build() {
            if (locationId == null) {
                throw new IllegalArgumentException("LocationId is required");
            }
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (coordinates == null) {
                throw new IllegalArgumentException("LocationCoordinates is required");
            }
            return new UpdateLocationCommand(this);
        }
    }
}

