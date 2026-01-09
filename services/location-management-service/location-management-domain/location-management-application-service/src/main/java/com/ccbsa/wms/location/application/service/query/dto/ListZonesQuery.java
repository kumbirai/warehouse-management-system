package com.ccbsa.wms.location.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: ListZonesQuery
 * <p>
 * Query object for listing zones under a warehouse.
 */
@Getter
@Builder
@AllArgsConstructor
public final class ListZonesQuery {
    private final TenantId tenantId;
    private final LocationId warehouseId;

    /**
     * Static factory method with validation.
     */
    public static ListZonesQuery of(TenantId tenantId, LocationId warehouseId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (warehouseId == null) {
            throw new IllegalArgumentException("WarehouseId is required");
        }
        return ListZonesQuery.builder().tenantId(tenantId).warehouseId(warehouseId).build();
    }
}
