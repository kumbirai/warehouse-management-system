package com.ccbsa.wms.location.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;

/**
 * Command DTO: UpdateLocationStatusCommand
 * <p>
 * Command object for updating a location's status.
 */
public final class UpdateLocationStatusCommand {
    private final LocationId locationId;
    private final TenantId tenantId;
    private final LocationStatus status;
    private final String reason;

    private UpdateLocationStatusCommand(Builder builder) {
        this.locationId = builder.locationId;
        this.tenantId = builder.tenantId;
        this.status = builder.status;
        this.reason = builder.reason;
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

    public LocationStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public static class Builder {
        private LocationId locationId;
        private TenantId tenantId;
        private LocationStatus status;
        private String reason;

        public Builder locationId(LocationId locationId) {
            this.locationId = locationId;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder status(LocationStatus status) {
            this.status = status;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public UpdateLocationStatusCommand build() {
            return new UpdateLocationStatusCommand(this);
        }
    }
}

