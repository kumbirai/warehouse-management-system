package com.ccbsa.wms.picking.domain.core.exception;

/**
 * Exception: InvalidPickingQuantityException
 * <p>
 * Thrown when attempting to pick an invalid quantity (zero, negative, or exceeding required).
 */
public class InvalidPickingQuantityException extends RuntimeException {
    public InvalidPickingQuantityException(String message) {
        super(message);
    }

    public InvalidPickingQuantityException(String message, Throwable cause) {
        super(message, cause);
    }
}
