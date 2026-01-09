package com.ccbsa.wms.picking.application.service.command.dto;

import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskId;

import lombok.Builder;
import lombok.Getter;

/**
 * Result DTO: CreatePickingTaskResult
 * <p>
 * Result object for picking task creation.
 */
@Getter
@Builder
public final class CreatePickingTaskResult {
    private final PickingTaskId taskId;
    private final String status;
    private final String orderId;

    public CreatePickingTaskResult(PickingTaskId taskId, String status, String orderId) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID is required");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        this.taskId = taskId;
        this.status = status;
        this.orderId = orderId;
    }
}
