package com.ccbsa.wms.stock.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAdjustmentId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: GetStockAdjustmentQuery
 * <p>
 * Query object for retrieving a stock adjustment by ID.
 */
@Getter
@Builder
public final class GetStockAdjustmentQuery {
    private final StockAdjustmentId adjustmentId;
    private final TenantId tenantId;

    public GetStockAdjustmentQuery(StockAdjustmentId adjustmentId, TenantId tenantId) {
        if (adjustmentId == null) {
            throw new IllegalArgumentException("AdjustmentId is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        this.adjustmentId = adjustmentId;
        this.tenantId = tenantId;
    }
}

