package com.ccbsa.wms.location.domain.core.valueobject;

/**
 * Enum: MovementStatus
 * <p>
 * Represents the status of a stock movement.
 * <p>
 * Status Values:
 * - INITIATED: Movement has been initiated but not yet completed
 * - IN_PROGRESS: Movement is currently in progress
 * - COMPLETED: Movement has been successfully completed
 * - CANCELLED: Movement has been cancelled
 */
public enum MovementStatus {
    INITIATED, IN_PROGRESS, COMPLETED, CANCELLED
}

