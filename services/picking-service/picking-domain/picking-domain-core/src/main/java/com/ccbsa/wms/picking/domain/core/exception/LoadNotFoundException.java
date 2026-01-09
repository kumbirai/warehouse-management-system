package com.ccbsa.wms.picking.domain.core.exception;

/**
 * Exception: LoadNotFoundException
 * <p>
 * Thrown when a load is not found.
 */
public class LoadNotFoundException extends RuntimeException {
    public LoadNotFoundException(String loadId) {
        super(String.format("Load not found: %s", loadId));
    }
}
