package com.ccbsa.wms.stock.domain.core.exception;

import com.ccbsa.common.domain.exception.DomainException;

/**
 * Domain Exception: StockAdjustmentNotFoundException
 * <p>
 * Thrown when a stock adjustment cannot be found by ID and tenant ID.
 */
public class StockAdjustmentNotFoundException extends DomainException {
    public StockAdjustmentNotFoundException(String message) {
        super(message);
    }

    public StockAdjustmentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

