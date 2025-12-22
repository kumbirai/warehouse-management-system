package com.ccbsa.wms.stock.domain.core.exception;

import com.ccbsa.common.domain.exception.DomainException;
import com.ccbsa.wms.stock.domain.core.valueobject.StockItemId;

/**
 * Domain Exception: StockItemNotFoundException
 * <p>
 * Thrown when a stock item is not found.
 */
public class StockItemNotFoundException extends DomainException {
    public StockItemNotFoundException(StockItemId stockItemId) {
        super(String.format("Stock item not found: %s", stockItemId.getValueAsString()));
    }

    public StockItemNotFoundException(String message) {
        super(message);
    }

    public StockItemNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

