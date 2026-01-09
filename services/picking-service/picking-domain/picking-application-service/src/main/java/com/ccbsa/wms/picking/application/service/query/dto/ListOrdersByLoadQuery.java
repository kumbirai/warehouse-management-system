package com.ccbsa.wms.picking.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: ListOrdersByLoadQuery
 * <p>
 * Query object for listing orders by load.
 */
@Getter
@Builder
public final class ListOrdersByLoadQuery {
    private final TenantId tenantId;
    private final LoadId loadId;

    public ListOrdersByLoadQuery(TenantId tenantId, LoadId loadId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (loadId == null) {
            throw new IllegalArgumentException("LoadId is required");
        }
        this.tenantId = tenantId;
        this.loadId = loadId;
    }
}
