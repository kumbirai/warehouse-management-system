package com.ccbsa.wms.picking.domain.core.exception;

/**
 * Exception: OrderNotFoundException
 * <p>
 * Thrown when an order is not found.
 */
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) {
        super(message);
    }
}
