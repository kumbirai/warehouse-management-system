package com.ccbsa.wms.stock.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Query DTO: GetStockItemsByProductAndLocationQuery
 * <p>
 * Query for retrieving stock items by product ID and location ID.
 */
@Getter
@Builder
@EqualsAndHashCode
public final class GetStockItemsByProductAndLocationQuery {
    private final TenantId tenantId;
    private final ProductId productId;
    private final LocationId locationId;

    /**
     * Static factory method with validation.
     */
    public static GetStockItemsByProductAndLocationQuery of(TenantId tenantId, ProductId productId, LocationId locationId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (productId == null) {
            throw new IllegalArgumentException("ProductId is required");
        }
        if (locationId == null) {
            throw new IllegalArgumentException("LocationId is required");
        }
        return GetStockItemsByProductAndLocationQuery.builder().tenantId(tenantId).productId(productId).locationId(locationId).build();
    }
}
