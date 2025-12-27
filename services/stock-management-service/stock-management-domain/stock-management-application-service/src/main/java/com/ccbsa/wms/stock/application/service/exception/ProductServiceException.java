package com.ccbsa.wms.stock.application.service.exception;

/**
 * Exception thrown when Product Service is unavailable or returns an error.
 * <p>
 * This exception indicates a problem with the Product Service itself, not with the product data.
 */
public class ProductServiceException extends RuntimeException {
    public ProductServiceException(String message) {
        super(message);
    }

    public ProductServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

