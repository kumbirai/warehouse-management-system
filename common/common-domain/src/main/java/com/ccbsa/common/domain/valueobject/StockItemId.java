package com.ccbsa.common.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: StockItemId
 * <p>
 * Represents a unique identifier for a stock item. Immutable and self-validating.
 * <p>
 * This value object is shared across services (DRY principle).
 * Supports both UUID and String representations for cross-service references.
 */
public final class StockItemId {
    private final UUID value;

    private StockItemId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("StockItemId cannot be null");
        }
        this.value = value;
    }

    /**
     * Factory method to create StockItemId from UUID.
     *
     * @param value UUID value
     * @return StockItemId instance
     * @throws IllegalArgumentException if value is null
     */
    public static StockItemId of(UUID value) {
        return new StockItemId(value);
    }

    /**
     * Factory method to create StockItemId from string.
     *
     * @param value UUID string value
     * @return StockItemId instance
     * @throws IllegalArgumentException if value is null or invalid UUID format
     */
    public static StockItemId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("StockItemId string cannot be null or empty");
        }
        try {
            return new StockItemId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format for StockItemId: %s", value), e);
        }
    }

    /**
     * Creates a new StockItemId with random UUID.
     *
     * @return New StockItemId instance
     */
    public static StockItemId generate() {
        return new StockItemId(UUID.randomUUID());
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
        StockItemId that = (StockItemId) o;
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
