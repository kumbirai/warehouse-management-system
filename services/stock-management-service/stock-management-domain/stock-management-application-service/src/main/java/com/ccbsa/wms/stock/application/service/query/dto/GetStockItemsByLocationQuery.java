package com.ccbsa.wms.stock.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: GetStockItemsByLocationQuery
 * <p>
 * Query object for retrieving stock items by location ID.
 */
@Getter
@Builder
public final class GetStockItemsByLocationQuery {
    private final TenantId tenantId;
    private final LocationId locationId;

    /**
     * Static factory method with validation.
     */
    public static GetStockItemsByLocationQuery of(TenantId tenantId, LocationId locationId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (locationId == null) {
            throw new IllegalArgumentException("LocationId is required");
        }
        return GetStockItemsByLocationQuery.builder().tenantId(tenantId).locationId(locationId).build();
    }
}
