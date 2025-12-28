package com.ccbsa.wms.location.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: GetLocationQuery
 * <p>
 * Query object for retrieving a location by ID.
 */
@Getter
@Builder
public final class GetLocationQuery {
    private final LocationId locationId;
    private final TenantId tenantId;

    public GetLocationQuery(LocationId locationId, TenantId tenantId) {
        if (locationId == null) {
            throw new IllegalArgumentException("LocationId is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        this.locationId = locationId;
        this.tenantId = tenantId;
    }
}

