package com.ccbsa.common.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: ReturnLineItemId
 * <p>
 * Represents a unique identifier for a return line item. Immutable and self-validating.
 * <p>
 * This value object is shared across services (DRY principle).
 */
public final class ReturnLineItemId {
    private final UUID value;

    private ReturnLineItemId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("ReturnLineItemId cannot be null");
        }
        this.value = value;
    }

    /**
     * Factory method to create ReturnLineItemId from UUID.
     *
     * @param value UUID value
     * @return ReturnLineItemId instance
     * @throws IllegalArgumentException if value is null
     */
    public static ReturnLineItemId of(UUID value) {
        return new ReturnLineItemId(value);
    }

    /**
     * Factory method to create ReturnLineItemId from string.
     *
     * @param value UUID string value
     * @return ReturnLineItemId instance
     * @throws IllegalArgumentException if value is null or invalid UUID format
     */
    public static ReturnLineItemId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("ReturnLineItemId string cannot be null or empty");
        }
        try {
            return new ReturnLineItemId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format for ReturnLineItemId: %s", value), e);
        }
    }

    /**
     * Creates a new ReturnLineItemId with random UUID.
     *
     * @return New ReturnLineItemId instance
     */
    public static ReturnLineItemId generate() {
        return new ReturnLineItemId(UUID.randomUUID());
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
        ReturnLineItemId that = (ReturnLineItemId) o;
        return Objects.equals(value, that.value);
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
