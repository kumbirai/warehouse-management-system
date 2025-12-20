package com.ccbsa.wms.stock.domain.core.exception;

import com.ccbsa.common.domain.exception.DomainException;

/**
 * Domain Exception: InvalidConsignmentReferenceException
 * <p>
 * Thrown when a consignment reference is invalid or violates business rules.
 */
public class InvalidConsignmentReferenceException extends DomainException {
    public InvalidConsignmentReferenceException(String message) {
        super(message);
    }

    public InvalidConsignmentReferenceException(String message, Throwable cause) {
        super(message, cause);
    }
}

