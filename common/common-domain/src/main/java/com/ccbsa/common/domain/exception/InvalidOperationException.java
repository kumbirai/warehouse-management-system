package com.ccbsa.common.domain.exception;

/**
 * Exception thrown when an invalid operation is attempted on a domain entity.
 */
public class InvalidOperationException extends DomainException {
    public InvalidOperationException(String message) {
        super(message);
    }

    public InvalidOperationException(String entityType,
                                     String operation,
                                     String reason) {
        super(String.format("Cannot %s %s: %s",
                operation,
                entityType,
                reason));
    }
}

