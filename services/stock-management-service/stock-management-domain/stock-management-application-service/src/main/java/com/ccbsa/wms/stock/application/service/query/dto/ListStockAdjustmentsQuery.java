package com.ccbsa.wms.stock.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: ListStockAdjustmentsQuery
 * <p>
 * Query object for listing stock adjustments with optional filters.
 */
@Getter
@Builder
@AllArgsConstructor
public final class ListStockAdjustmentsQuery {
    private final TenantId tenantId;
    private final ProductId productId;
    private final LocationId locationId;
    private final StockItemId stockItemId;
    private final Integer page;
    private final Integer size;

    /**
     * Static factory method with validation.
     */
    public static ListStockAdjustmentsQuery of(TenantId tenantId, ProductId productId, LocationId locationId, StockItemId stockItemId, Integer page, Integer size) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        return ListStockAdjustmentsQuery.builder().tenantId(tenantId).productId(productId).locationId(locationId).stockItemId(stockItemId).page(page).size(size).build();
    }
}

