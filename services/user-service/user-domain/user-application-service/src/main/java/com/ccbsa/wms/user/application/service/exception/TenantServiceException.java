package com.ccbsa.wms.user.application.service.exception;

/**
 * Exception thrown when tenant service operations fail.
 */
public class TenantServiceException extends RuntimeException {
    public TenantServiceException(String message) {
        super(message);
    }

    public TenantServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

