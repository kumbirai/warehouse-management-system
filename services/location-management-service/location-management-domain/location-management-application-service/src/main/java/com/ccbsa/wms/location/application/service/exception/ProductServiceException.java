package com.ccbsa.wms.location.application.service.exception;

/**
 * Exception: ProductServiceException
 * <p>
 * Thrown when Product Service is unavailable or returns an error.
 */
public class ProductServiceException extends RuntimeException {
    public ProductServiceException(String message) {
        super(message);
    }

    public ProductServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
