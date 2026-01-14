package com.ccbsa.common.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: ReturnId
 * <p>
 * Represents a unique identifier for a return. Immutable and self-validating.
 * <p>
 * This value object is shared across services (DRY principle).
 */
public final class ReturnId {
    private final UUID value;

    private ReturnId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("ReturnId cannot be null");
        }
        this.value = value;
    }

    /**
     * Factory method to create ReturnId from UUID.
     *
     * @param value UUID value
     * @return ReturnId instance
     * @throws IllegalArgumentException if value is null
     */
    public static ReturnId of(UUID value) {
        return new ReturnId(value);
    }

    /**
     * Factory method to create ReturnId from string.
     *
     * @param value UUID string value
     * @return ReturnId instance
     * @throws IllegalArgumentException if value is null or invalid UUID format
     */
    public static ReturnId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("ReturnId string cannot be null or empty");
        }
        try {
            return new ReturnId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format for ReturnId: %s", value), e);
        }
    }

    /**
     * Creates a new ReturnId with random UUID.
     *
     * @return New ReturnId instance
     */
    public static ReturnId generate() {
        return new ReturnId(UUID.randomUUID());
    }

    /**
     * Returns the UUID value.
     *
     * @return UUID value
     */
    public UUID getValue() {
        return value;
    }

    /**
     * Returns the UUID value as string.
     *
     * @return UUID string value
     */
    public String getValueAsString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReturnId returnId = (ReturnId) o;
        return Objects.equals(value, returnId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
