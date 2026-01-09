package com.ccbsa.wms.picking.domain.core.valueobject;

/**
 * Enum: PickingTaskStatus
 * <p>
 * Represents the status of a picking task.
 * <p>
 * Status values:
 * - PENDING: Picking task is pending execution
 * - IN_PROGRESS: Picking task is in progress
 * - COMPLETED: Picking task has been completed
 */
public enum PickingTaskStatus {
    /**
     * Picking task is pending execution
     */
    PENDING,

    /**
     * Picking task is in progress
     */
    IN_PROGRESS,

    /**
     * Picking task has been completed
     */
    COMPLETED
}
