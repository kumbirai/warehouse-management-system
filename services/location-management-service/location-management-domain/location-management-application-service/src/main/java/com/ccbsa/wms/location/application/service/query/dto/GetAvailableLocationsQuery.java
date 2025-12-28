package com.ccbsa.wms.location.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: GetAvailableLocationsQuery
 * <p>
 * Query object for retrieving available locations.
 */
@Getter
@Builder
public final class GetAvailableLocationsQuery {
    private final TenantId tenantId;

    public GetAvailableLocationsQuery(TenantId tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        this.tenantId = tenantId;
    }
}

