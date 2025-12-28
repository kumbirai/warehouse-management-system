package com.ccbsa.wms.stock.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: GetStockItemQuery
 * <p>
 * Query object for retrieving a stock item by ID.
 */
@Getter
@Builder
public final class GetStockItemQuery {
    private final StockItemId stockItemId;
    private final TenantId tenantId;

    public GetStockItemQuery(StockItemId stockItemId, TenantId tenantId) {
        if (stockItemId == null) {
            throw new IllegalArgumentException("StockItemId is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        this.stockItemId = stockItemId;
        this.tenantId = tenantId;
    }
}

