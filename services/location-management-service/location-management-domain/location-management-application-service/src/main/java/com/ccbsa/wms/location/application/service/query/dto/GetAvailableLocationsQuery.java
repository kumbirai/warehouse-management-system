package com.ccbsa.wms.location.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Query DTO: GetAvailableLocationsQuery
 * <p>
 * Query object for retrieving available locations.
 */
public final class GetAvailableLocationsQuery {
    private final TenantId tenantId;

    private GetAvailableLocationsQuery(Builder builder) {
        this.tenantId = builder.tenantId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public static class Builder {
        private TenantId tenantId;

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public GetAvailableLocationsQuery build() {
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            return new GetAvailableLocationsQuery(this);
        }
    }
}

