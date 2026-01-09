package com.ccbsa.wms.location.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: ListWarehousesQuery
 * <p>
 * Query object for listing warehouses (locations with type WAREHOUSE).
 */
@Getter
@Builder
@AllArgsConstructor
public final class ListWarehousesQuery {
    private final TenantId tenantId;

    /**
     * Static factory method with validation.
     */
    public static ListWarehousesQuery of(TenantId tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        return ListWarehousesQuery.builder().tenantId(tenantId).build();
    }
}
