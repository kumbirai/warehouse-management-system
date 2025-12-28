package com.ccbsa.wms.stock.domain.core.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: StockAdjustmentId
 * <p>
 * Represents a unique identifier for stock adjustment. Immutable and validated on construction.
 */
public final class StockAdjustmentId {
    private final UUID value;

    /**
     * Private constructor to enforce immutability.
     *
     * @param value UUID value
     * @throws IllegalArgumentException if value is null
     */
    private StockAdjustmentId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("StockAdjustmentId value cannot be null");
        }
        this.value = value;
    }

    /**
     * Factory method to create StockAdjustmentId from UUID.
     *
     * @param value UUID value
     * @return StockAdjustmentId instance
     * @throws IllegalArgumentException if value is null
     */
    public static StockAdjustmentId of(UUID value) {
        return new StockAdjustmentId(value);
    }

    /**
     * Factory method to create StockAdjustmentId from String.
     *
     * @param value UUID string representation
     * @return StockAdjustmentId instance
     * @throws IllegalArgumentException if value is invalid
     */
    public static StockAdjustmentId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("StockAdjustmentId string cannot be null or empty");
        }
        try {
            return new StockAdjustmentId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format: %s", value), e);
        }
    }

    /**
     * Generates a new StockAdjustmentId with random UUID.
     *
     * @return New StockAdjustmentId instance
     */
    public static StockAdjustmentId generate() {
        return new StockAdjustmentId(UUID.randomUUID());
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
        StockAdjustmentId that = (StockAdjustmentId) o;
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

