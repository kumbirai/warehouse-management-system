package com.ccbsa.wms.picking.domain.core.exception;

/**
 * Exception: PickingTaskNotFoundException
 * <p>
 * Thrown when a picking task is not found.
 */
public class PickingTaskNotFoundException extends RuntimeException {
    public PickingTaskNotFoundException(String message) {
        super(message);
    }

    public PickingTaskNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
