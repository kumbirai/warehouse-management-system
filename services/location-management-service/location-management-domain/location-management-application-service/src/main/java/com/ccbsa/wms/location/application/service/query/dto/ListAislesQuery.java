package com.ccbsa.wms.location.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: ListAislesQuery
 * <p>
 * Query object for listing aisles under a zone.
 */
@Getter
@Builder
@AllArgsConstructor
public final class ListAislesQuery {
    private final TenantId tenantId;
    private final LocationId zoneId;

    /**
     * Static factory method with validation.
     */
    public static ListAislesQuery of(TenantId tenantId, LocationId zoneId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (zoneId == null) {
            throw new IllegalArgumentException("ZoneId is required");
        }
        return ListAislesQuery.builder().tenantId(tenantId).zoneId(zoneId).build();
    }
}
