package com.ccbsa.wms.picking.application.service.exception;

/**
 * Exception thrown when Stock Management Service is unavailable or returns an error.
 * <p>
 * This exception indicates a problem with the Stock Management Service itself, not with the stock data.
 */
public class StockManagementServiceException extends RuntimeException {
    public StockManagementServiceException(String message) {
        super(message);
    }

    public StockManagementServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
