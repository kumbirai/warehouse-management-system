package com.ccbsa.wms.user.application.service.exception;

/**
 * Exception thrown when a user does not have sufficient privileges to perform an operation.
 */
public class InsufficientPrivilegesException extends RuntimeException {
    public InsufficientPrivilegesException(String message) {
        super(message);
    }

    public InsufficientPrivilegesException(String message, Throwable cause) {
        super(message, cause);
    }
}

