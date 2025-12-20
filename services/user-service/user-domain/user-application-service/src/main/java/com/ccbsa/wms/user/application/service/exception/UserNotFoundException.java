package com.ccbsa.wms.user.application.service.exception;

/**
 * Exception thrown when a user is not found.
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

