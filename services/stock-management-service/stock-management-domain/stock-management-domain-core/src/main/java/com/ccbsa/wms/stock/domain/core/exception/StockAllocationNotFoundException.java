package com.ccbsa.wms.stock.domain.core.exception;

import com.ccbsa.common.domain.exception.DomainException;

/**
 * Domain Exception: StockAllocationNotFoundException
 * <p>
 * Thrown when a stock allocation cannot be found by ID and tenant ID.
 */
public class StockAllocationNotFoundException extends DomainException {
    public StockAllocationNotFoundException(String message) {
        super(message);
    }

    public StockAllocationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

