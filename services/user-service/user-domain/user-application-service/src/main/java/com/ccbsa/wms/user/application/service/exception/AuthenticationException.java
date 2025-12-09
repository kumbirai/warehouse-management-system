package com.ccbsa.wms.user.application.service.exception;

/**
 * Exception thrown when authentication fails.
 * <p>
 * This exception indicates that the provided credentials are invalid
 * or the user is not authorized. This is a runtime exception for
 * authentication-related errors.
 */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message,
                                   Throwable cause) {
        super(message,
                cause);
    }
}

