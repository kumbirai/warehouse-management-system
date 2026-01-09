package com.ccbsa.wms.picking.domain.core.valueobject;

/**
 * Enum: PickingListStatus
 * <p>
 * Represents the status of a picking list.
 * <p>
 * Status values:
 * - RECEIVED: Picking list has been received and is ready for processing
 * - PROCESSING: Picking list is being processed
 * - PLANNED: Picking locations have been planned
 * - COMPLETED: Picking list has been completed
 */
public enum PickingListStatus {
    /**
     * Picking list has been received and is ready for processing
     */
    RECEIVED,

    /**
     * Picking list is being processed
     */
    PROCESSING,

    /**
     * Picking locations have been planned
     */
    PLANNED,

    /**
     * Picking list has been completed
     */
    COMPLETED
}
