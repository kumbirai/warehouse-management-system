package com.ccbsa.wms.stock.domain.core.exception;

import com.ccbsa.common.domain.exception.DomainException;

/**
 * Domain Exception: InvalidQuantityException
 * <p>
 * Thrown when a quantity value is invalid or violates business rules.
 */
public class InvalidQuantityException
        extends DomainException {
    public InvalidQuantityException(String message) {
        super(message);
    }

    public InvalidQuantityException(String message, Throwable cause) {
        super(message, cause);
    }
}

