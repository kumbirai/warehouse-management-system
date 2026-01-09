package com.ccbsa.wms.picking.application.service.command.dto;

import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskId;

import lombok.Builder;
import lombok.Getter;

/**
 * Command Result DTO: ExecutePickingTaskResult
 * <p>
 * Result object returned after executing a picking task.
 */
@Getter
@Builder
public final class ExecutePickingTaskResult {
    private final PickingTaskId pickingTaskId;
    private final String status;
    private final int pickedQuantity;
    private final boolean isPartialPicking;
}
