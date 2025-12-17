package com.ccbsa.wms.user.application.service.exception;

/**
 * Exception thrown when user creation fails.
 */
public class UserCreationException
        extends RuntimeException {
    public UserCreationException(String message) {
        super(message);
    }

    public UserCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}

