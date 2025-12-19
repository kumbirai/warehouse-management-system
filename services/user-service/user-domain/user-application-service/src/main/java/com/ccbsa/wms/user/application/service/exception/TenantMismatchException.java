package com.ccbsa.wms.user.application.service.exception;

/**
 * Exception thrown when there is a tenant mismatch in an operation.
 * <p>
 * This exception indicates that an operation was attempted across tenant boundaries,
 * which is not allowed for tenant-scoped roles.
 */
public class TenantMismatchException
        extends RuntimeException {
    public TenantMismatchException(String message) {
        super(message);
    }

    public TenantMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}

