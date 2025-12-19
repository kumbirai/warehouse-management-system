package com.ccbsa.wms.user.application.service.exception;

/**
 * Exception thrown when role assignment operations fail.
 */
public class RoleAssignmentException
        extends RuntimeException {
    public RoleAssignmentException(String message) {
        super(message);
    }

    public RoleAssignmentException(String message, Throwable cause) {
        super(message, cause);
    }
}

