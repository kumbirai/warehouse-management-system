package com.ccbsa.wms.user.application.service.exception;

/**
 * Exception thrown when attempting to create a user with a duplicate username or email.
 */
public class DuplicateUserException
        extends RuntimeException {
    public DuplicateUserException(String message) {
        super(message);
    }

    public DuplicateUserException(String message, Throwable cause) {
        super(message, cause);
    }
}

