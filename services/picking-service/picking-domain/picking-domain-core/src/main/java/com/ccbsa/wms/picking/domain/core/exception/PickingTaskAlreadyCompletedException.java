package com.ccbsa.wms.picking.domain.core.exception;

/**
 * Exception: PickingTaskAlreadyCompletedException
 * <p>
 * Thrown when attempting to execute a picking task that has already been completed.
 */
public class PickingTaskAlreadyCompletedException extends RuntimeException {
    public PickingTaskAlreadyCompletedException(String message) {
        super(message);
    }

    public PickingTaskAlreadyCompletedException(String message, Throwable cause) {
        super(message, cause);
    }
}
