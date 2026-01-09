package com.ccbsa.wms.picking.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;

import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: PlanPickingLocationsCommand
 * <p>
 * Command object for planning picking locations for a load.
 */
@Getter
@Builder
public final class PlanPickingLocationsCommand {
    private final TenantId tenantId;
    private final PickingListId pickingListId;
    private final LoadId loadId;

    public PlanPickingLocationsCommand(TenantId tenantId, PickingListId pickingListId, LoadId loadId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (pickingListId == null) {
            throw new IllegalArgumentException("PickingListId is required");
        }
        if (loadId == null) {
            throw new IllegalArgumentException("LoadId is required");
        }
        this.tenantId = tenantId;
        this.pickingListId = pickingListId;
        this.loadId = loadId;
    }
}
