package com.ccbsa.wms.location.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: ListRacksQuery
 * <p>
 * Query object for listing racks under an aisle.
 */
@Getter
@Builder
@AllArgsConstructor
public final class ListRacksQuery {
    private final TenantId tenantId;
    private final LocationId aisleId;

    /**
     * Static factory method with validation.
     */
    public static ListRacksQuery of(TenantId tenantId, LocationId aisleId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (aisleId == null) {
            throw new IllegalArgumentException("AisleId is required");
        }
        return ListRacksQuery.builder().tenantId(tenantId).aisleId(aisleId).build();
    }
}
