package com.ccbsa.wms.picking.domain.core.exception;

/**
 * Exception: PickingListNotFoundException
 * <p>
 * Thrown when a picking list is not found.
 */
public class PickingListNotFoundException extends RuntimeException {
    public PickingListNotFoundException(String pickingListId) {
        super(String.format("Picking list not found: %s", pickingListId));
    }
}
