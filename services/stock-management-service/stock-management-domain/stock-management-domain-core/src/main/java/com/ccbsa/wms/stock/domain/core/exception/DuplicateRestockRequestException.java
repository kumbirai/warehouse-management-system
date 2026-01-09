package com.ccbsa.wms.stock.domain.core.exception;

/**
 * Exception: DuplicateRestockRequestException
 * <p>
 * Thrown when attempting to create a duplicate restock request for a product.
 */
public class DuplicateRestockRequestException extends RuntimeException {
    public DuplicateRestockRequestException(String message) {
        super(message);
    }

    public DuplicateRestockRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
