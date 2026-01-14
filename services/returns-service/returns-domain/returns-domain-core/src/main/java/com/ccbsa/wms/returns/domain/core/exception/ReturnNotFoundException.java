package com.ccbsa.wms.returns.domain.core.exception;

/**
 * Exception: ReturnNotFoundException
 * <p>
 * Thrown when a return cannot be found.
 */
public class ReturnNotFoundException extends RuntimeException {
    public ReturnNotFoundException(String message) {
        super(message);
    }

    public ReturnNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
