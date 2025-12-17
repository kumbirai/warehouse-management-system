package com.ccbsa.wms.user.application.service.exception;

/**
 * Exception thrown when token refresh fails.
 * <p>
 * This is a checked exception for token refresh-related errors.
 */
public class TokenRefreshException
        extends Exception {
    public TokenRefreshException(String message) {
        super(message);
    }

    public TokenRefreshException(String message, Throwable cause) {
        super(message, cause);
    }
}

