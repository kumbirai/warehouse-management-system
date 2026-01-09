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
 * - PARTIALLY_COMPLETED: Picking task has been partially completed
 * - CANCELLED: Picking task has been cancelled
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
    COMPLETED,

    /**
     * Picking task has been partially completed
     */
    PARTIALLY_COMPLETED,

    /**
     * Picking task has been cancelled
     */
    CANCELLED
}
