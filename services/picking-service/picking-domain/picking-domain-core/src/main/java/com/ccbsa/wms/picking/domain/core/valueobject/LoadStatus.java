package com.ccbsa.wms.picking.domain.core.valueobject;

/**
 * Enum: LoadStatus
 * <p>
 * Represents the status of a load.
 * <p>
 * Status values:
 * - CREATED: Load has been created
 * - PLANNED: Picking locations have been planned for the load
 * - IN_PROGRESS: Load picking is in progress
 * - COMPLETED: Load picking has been completed
 */
public enum LoadStatus {
    /**
     * Load has been created
     */
    CREATED,

    /**
     * Picking locations have been planned for the load
     */
    PLANNED,

    /**
     * Load picking is in progress
     */
    IN_PROGRESS,

    /**
     * Load picking has been completed
     */
    COMPLETED
}
