package com.ccbsa.wms.returns.application.service.exception;

/**
 * Exception thrown when Picking Service is unavailable or returns an error.
 * <p>
 * This exception indicates a problem with the Picking Service itself, not with the picking data.
 */
public class PickingServiceException extends RuntimeException {
    public PickingServiceException(String message) {
        super(message);
    }

    public PickingServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
