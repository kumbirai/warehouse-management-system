package com.ccbsa.common.domain.exception;

/**
 * Exception thrown when an entity is not found.
 */
public class EntityNotFoundException extends DomainException {
    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entityType, Object id) {
        super(String.format("%s with id %s not found", entityType, id));
    }
}

