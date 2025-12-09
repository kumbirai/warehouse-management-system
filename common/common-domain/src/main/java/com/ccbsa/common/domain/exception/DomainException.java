package com.ccbsa.common.domain.exception;

/**
 * Base exception class for all domain exceptions.
 * Domain exceptions represent business rule violations or domain-specific errors.
 */
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message,
                              Throwable cause) {
        super(message,
                cause);
    }
}

