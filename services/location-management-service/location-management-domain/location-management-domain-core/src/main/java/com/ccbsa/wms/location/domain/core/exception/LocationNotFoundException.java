package com.ccbsa.wms.location.domain.core.exception;

import com.ccbsa.common.domain.exception.DomainException;

/**
 * Domain Exception: LocationNotFoundException
 * <p>
 * Thrown when a location cannot be found by ID and tenant ID.
 */
public class LocationNotFoundException
        extends DomainException {
    public LocationNotFoundException(String message) {
        super(message);
    }

    public LocationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

