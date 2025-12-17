package com.ccbsa.wms.user.application.service.exception;

/**
 * Exception thrown when a tenant is not active.
 */
public class TenantNotActiveException
        extends RuntimeException {
    public TenantNotActiveException(String message) {
        super(message);
    }

    public TenantNotActiveException(String message, Throwable cause) {
        super(message, cause);
    }
}

