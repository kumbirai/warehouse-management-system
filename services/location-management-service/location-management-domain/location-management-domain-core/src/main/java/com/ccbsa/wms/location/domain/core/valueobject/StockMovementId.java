package com.ccbsa.wms.location.domain.core.valueobject;

import java.util.UUID;

/**
 * Value Object: StockMovementId
 * <p>
 * Represents the unique identifier for StockMovement. Immutable and validated on construction.
 */
public final class StockMovementId {
    private final UUID value;

    /**
     * Private constructor to enforce immutability.
     *
     * @param value UUID value
     * @throws IllegalArgumentException if value is null
     */
    private StockMovementId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("StockMovementId value cannot be null");
        }
        this.value = value;
    }

    /**
     * Factory method to create StockMovementId from UUID.
     *
     * @param value UUID value
     * @return StockMovementId instance
     * @throws IllegalArgumentException if value is null
     */
    public static StockMovementId of(UUID value) {
        return new StockMovementId(value);
    }

    /**
     * Factory method to create StockMovementId from String.
     *
     * @param value UUID string representation
     * @return StockMovementId instance
     * @throws IllegalArgumentException if value is invalid
     */
    public static StockMovementId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("StockMovementId string cannot be null or empty");
        }
        try {
            return new StockMovementId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format: %s", value), e);
        }
    }

    /**
     * Generates a new StockMovementId with random UUID.
     *
     * @return New StockMovementId instance
     */
    public static StockMovementId generate() {
        return new StockMovementId(UUID.randomUUID());
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
     * Returns the string representation of the UUID.
     *
     * @return UUID string
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
        StockMovementId that = (StockMovementId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

