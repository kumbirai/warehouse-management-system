package com.ccbsa.wms.picking.domain.core.valueobject;

/**
 * Enum: OrderStatus
 * <p>
 * Represents the status of an order.
 * <p>
 * Status values:
 * - PENDING: Order is pending processing
 * - IN_PROGRESS: Order picking is in progress
 * - COMPLETED: Order picking has been completed
 * - CANCELLED: Order has been cancelled
 */
public enum OrderStatus {
    /**
     * Order is pending processing
     */
    PENDING,

    /**
     * Order picking is in progress
     */
    IN_PROGRESS,

    /**
     * Order picking has been completed
     */
    COMPLETED,

    /**
     * Order has been cancelled
     */
    CANCELLED
}
