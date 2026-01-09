package com.ccbsa.wms.stock.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Query DTO: GetFEFOStockItemsQuery
 * <p>
 * Query for retrieving stock items sorted by FEFO (First-Expired, First-Out) principles.
 * Returns stock items sorted by expiration date (earliest first), then by received date.
 */
@Getter
@Builder
@EqualsAndHashCode
public final class GetFEFOStockItemsQuery {
    private final TenantId tenantId;
    private final ProductId productId;
    private final LocationId locationId; // Optional - null for all locations

    /**
     * Static factory method with validation.
     */
    public static GetFEFOStockItemsQuery of(TenantId tenantId, ProductId productId, LocationId locationId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (productId == null) {
            throw new IllegalArgumentException("ProductId is required");
        }
        return GetFEFOStockItemsQuery.builder().tenantId(tenantId).productId(productId).locationId(locationId).build();
    }
}
