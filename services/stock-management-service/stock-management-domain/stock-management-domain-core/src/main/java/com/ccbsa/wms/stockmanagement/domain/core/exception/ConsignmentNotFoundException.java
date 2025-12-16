package com.ccbsa.wms.stockmanagement.domain.core.exception;

import com.ccbsa.common.domain.exception.DomainException;

/**
 * Domain Exception: ConsignmentNotFoundException
 * <p>
 * Thrown when a consignment is not found.
 */
public class ConsignmentNotFoundException extends DomainException {
    public ConsignmentNotFoundException(String message) {
        super(message);
    }

    public ConsignmentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

