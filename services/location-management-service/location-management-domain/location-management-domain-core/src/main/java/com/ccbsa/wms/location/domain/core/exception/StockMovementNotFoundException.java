package com.ccbsa.wms.location.domain.core.exception;

import com.ccbsa.common.domain.exception.DomainException;

/**
 * Domain Exception: StockMovementNotFoundException
 * <p>
 * Thrown when a stock movement cannot be found by ID and tenant ID.
 */
public class StockMovementNotFoundException extends DomainException {
    public StockMovementNotFoundException(String message) {
        super(message);
    }

    public StockMovementNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

