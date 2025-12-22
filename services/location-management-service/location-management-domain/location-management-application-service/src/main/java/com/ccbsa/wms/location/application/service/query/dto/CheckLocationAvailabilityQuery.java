package com.ccbsa.wms.location.application.service.query.dto;

import java.math.BigDecimal;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

/**
 * Query DTO: CheckLocationAvailabilityQuery
 * <p>
 * Query object for checking location availability and capacity.
 */
public final class CheckLocationAvailabilityQuery {
    private final LocationId locationId;
    private final BigDecimal requiredQuantity;
    private final TenantId tenantId;

    private CheckLocationAvailabilityQuery(Builder builder) {
        this.locationId = builder.locationId;
        this.requiredQuantity = builder.requiredQuantity;
        this.tenantId = builder.tenantId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public BigDecimal getRequiredQuantity() {
        return requiredQuantity;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public static class Builder {
        private LocationId locationId;
        private BigDecimal requiredQuantity;
        private TenantId tenantId;

        public Builder locationId(LocationId locationId) {
            this.locationId = locationId;
            return this;
        }

        public Builder requiredQuantity(BigDecimal requiredQuantity) {
            this.requiredQuantity = requiredQuantity;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public CheckLocationAvailabilityQuery build() {
            if (locationId == null) {
                throw new IllegalArgumentException("LocationId is required");
            }
            if (requiredQuantity == null || requiredQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("RequiredQuantity must be positive");
            }
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            return new CheckLocationAvailabilityQuery(this);
        }
    }
}

