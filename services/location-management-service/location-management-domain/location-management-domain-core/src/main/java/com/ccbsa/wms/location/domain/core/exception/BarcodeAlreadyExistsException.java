package com.ccbsa.wms.location.domain.core.exception;

import com.ccbsa.common.domain.exception.DomainException;

/**
 * Domain Exception: BarcodeAlreadyExistsException
 * <p>
 * Thrown when attempting to create a location with a barcode that already exists for the tenant.
 */
public class BarcodeAlreadyExistsException
        extends DomainException {
    public BarcodeAlreadyExistsException(String message) {
        super(message);
    }

    public BarcodeAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}

