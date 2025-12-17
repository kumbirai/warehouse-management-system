package com.ccbsa.wms.stockmanagement.domain.core.exception;

import com.ccbsa.common.domain.exception.DomainException;

/**
 * Domain Exception: InvalidExpirationDateException
 * <p>
 * Thrown when an expiration date is invalid or violates business rules.
 */
public class InvalidExpirationDateException
        extends DomainException {
    public InvalidExpirationDateException(String message) {
        super(message);
    }

    public InvalidExpirationDateException(String message, Throwable cause) {
        super(message, cause);
    }
}

