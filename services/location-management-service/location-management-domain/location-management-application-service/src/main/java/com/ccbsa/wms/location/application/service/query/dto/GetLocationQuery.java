package com.ccbsa.wms.location.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

/**
 * Query DTO: GetLocationQuery
 * <p>
 * Query object for retrieving a location by ID.
 */
public final class GetLocationQuery {
    private final LocationId locationId;
    private final TenantId tenantId;

    private GetLocationQuery(Builder builder) {
        this.locationId = builder.locationId;
        this.tenantId = builder.tenantId;
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

    public static class Builder {
        private LocationId locationId;
        private TenantId tenantId;

        public Builder locationId(LocationId locationId) {
            this.locationId = locationId;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public GetLocationQuery build() {
            if (locationId == null) {
                throw new IllegalArgumentException("LocationId is required");
            }
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            return new GetLocationQuery(this);
        }
    }
}

