package com.ccbsa.wms.product.domain.core.exception;

import com.ccbsa.common.domain.exception.DomainException;

/**
 * Domain Exception: ProductNotFoundException
 * <p>
 * Thrown when a product cannot be found by ID and tenant ID.
 */
public class ProductNotFoundException extends DomainException {
    public ProductNotFoundException(String message) {
        super(message);
    }

    public ProductNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

