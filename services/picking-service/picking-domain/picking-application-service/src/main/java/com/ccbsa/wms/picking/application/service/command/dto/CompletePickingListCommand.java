package com.ccbsa.wms.picking.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;

import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: CompletePickingListCommand
 * <p>
 * Command object for completing a picking list.
 */
@Getter
@Builder
public final class CompletePickingListCommand {
    private final PickingListId pickingListId;
    private final TenantId tenantId;
    private final UserId completedByUserId;
}
