package com.ccbsa.wms.stock.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: GetStockItemsQuery
 * <p>
 * Query object for retrieving all stock items for a tenant.
 */
@Getter
@Builder
public final class GetStockItemsQuery {
    private final TenantId tenantId;

    public GetStockItemsQuery(TenantId tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        this.tenantId = tenantId;
    }
}
