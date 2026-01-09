package com.ccbsa.wms.picking.domain.core.exception;

/**
 * Exception: ExpiredStockException
 * <p>
 * Thrown when attempting to pick expired stock.
 */
public class ExpiredStockException extends RuntimeException {
    public ExpiredStockException(String message) {
        super(message);
    }

    public ExpiredStockException(String message, Throwable cause) {
        super(message, cause);
    }
}
