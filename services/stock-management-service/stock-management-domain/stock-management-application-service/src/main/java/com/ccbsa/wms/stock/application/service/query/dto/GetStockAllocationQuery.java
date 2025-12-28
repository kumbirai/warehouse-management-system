package com.ccbsa.wms.stock.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAllocationId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: GetStockAllocationQuery
 * <p>
 * Query object for retrieving a stock allocation by ID.
 */
@Getter
@Builder
public final class GetStockAllocationQuery {
    private final StockAllocationId allocationId;
    private final TenantId tenantId;

    public GetStockAllocationQuery(StockAllocationId allocationId, TenantId tenantId) {
        if (allocationId == null) {
            throw new IllegalArgumentException("AllocationId is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        this.allocationId = allocationId;
        this.tenantId = tenantId;
    }
}

