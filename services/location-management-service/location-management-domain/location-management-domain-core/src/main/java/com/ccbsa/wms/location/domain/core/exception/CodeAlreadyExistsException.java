package com.ccbsa.wms.location.domain.core.exception;

import com.ccbsa.common.domain.exception.DomainException;

/**
 * Domain Exception: CodeAlreadyExistsException
 * <p>
 * Thrown when attempting to create a location with a code that already exists for the tenant.
 */
public class CodeAlreadyExistsException
        extends DomainException {
    public CodeAlreadyExistsException(String message) {
        super(message);
    }

    public CodeAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}

