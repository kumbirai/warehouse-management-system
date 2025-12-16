package com.ccbsa.wms.product.domain.core.exception;

import com.ccbsa.common.domain.exception.DomainException;

/**
 * Domain Exception: ProductCodeAlreadyExistsException
 * <p>
 * Thrown when attempting to create a product with a product code that already exists for the tenant.
 */
public class ProductCodeAlreadyExistsException extends DomainException {
    public ProductCodeAlreadyExistsException(String message) {
        super(message);
    }

    public ProductCodeAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}

