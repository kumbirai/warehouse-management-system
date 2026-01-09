package com.ccbsa.wms.picking.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.picking.domain.core.valueobject.PartialReason;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskId;

import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: ExecutePickingTaskCommand
 * <p>
 * Command object for executing a picking task.
 */
@Getter
@Builder
public final class ExecutePickingTaskCommand {
    private final PickingTaskId pickingTaskId;
    private final TenantId tenantId;
    private final Quantity pickedQuantity;
    private final boolean isPartialPicking;
    private final PartialReason partialReason;
    private final UserId pickedByUserId;
}
