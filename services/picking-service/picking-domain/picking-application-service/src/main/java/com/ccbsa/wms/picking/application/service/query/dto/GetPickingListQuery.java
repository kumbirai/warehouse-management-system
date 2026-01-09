package com.ccbsa.wms.picking.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: GetPickingListQuery
 * <p>
 * Query object for getting a picking list by ID.
 */
@Getter
@Builder
public final class GetPickingListQuery {
    private final TenantId tenantId;
    private final PickingListId pickingListId;

    public GetPickingListQuery(TenantId tenantId, PickingListId pickingListId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (pickingListId == null) {
            throw new IllegalArgumentException("PickingListId is required");
        }
        this.tenantId = tenantId;
        this.pickingListId = pickingListId;
    }
}
