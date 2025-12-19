package com.ccbsa.wms.location.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCoordinates;

/**
 * Command DTO: CreateLocationCommand
 * <p>
 * Command object for creating a new warehouse location.
 */
public final class CreateLocationCommand {
    private final TenantId tenantId;
    private final LocationCoordinates coordinates;
    private final LocationBarcode barcode; // Optional - will be generated if null
    private final String description;
    private final String code; // Original location code from request
    private final String name; // Location name from request
    private final String type; // Location type from request
    private final String parentLocationId; // Parent location ID for hierarchical relationships

    private CreateLocationCommand(Builder builder) {
        this.tenantId = builder.tenantId;
        this.coordinates = builder.coordinates;
        this.barcode = builder.barcode;
        this.description = builder.description;
        this.code = builder.code;
        this.name = builder.name;
        this.type = builder.type;
        this.parentLocationId = builder.parentLocationId;
    }

    public static Builder builder() {
        return new Builder();
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

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getParentLocationId() {
        return parentLocationId;
    }

    public static class Builder {
        private TenantId tenantId;
        private LocationCoordinates coordinates;
        private LocationBarcode barcode;
        private String description;
        private String code;
        private String name;
        private String type;
        private String parentLocationId;

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

        public Builder parentLocationId(String parentLocationId) {
            this.parentLocationId = parentLocationId;
            return this;
        }

        public CreateLocationCommand build() {
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (coordinates == null) {
                throw new IllegalArgumentException("LocationCoordinates is required");
            }
            return new CreateLocationCommand(this);
        }
    }
}

