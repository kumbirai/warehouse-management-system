package com.ccbsa.wms.stock.domain.core.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: StockLevelThresholdId
 * <p>
 * Represents a unique identifier for stock level threshold. Immutable and validated on construction.
 */
public final class StockLevelThresholdId {
    private final UUID value;

    /**
     * Private constructor to enforce immutability.
     *
     * @param value UUID value
     * @throws IllegalArgumentException if value is null
     */
    private StockLevelThresholdId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("StockLevelThresholdId value cannot be null");
        }
        this.value = value;
    }

    /**
     * Factory method to create StockLevelThresholdId from UUID.
     *
     * @param value UUID value
     * @return StockLevelThresholdId instance
     * @throws IllegalArgumentException if value is null
     */
    public static StockLevelThresholdId of(UUID value) {
        return new StockLevelThresholdId(value);
    }

    /**
     * Factory method to create StockLevelThresholdId from String.
     *
     * @param value UUID string representation
     * @return StockLevelThresholdId instance
     * @throws IllegalArgumentException if value is invalid
     */
    public static StockLevelThresholdId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("StockLevelThresholdId string cannot be null or empty");
        }
        try {
            return new StockLevelThresholdId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format: %s", value), e);
        }
    }

    /**
     * Generates a new StockLevelThresholdId with random UUID.
     *
     * @return New StockLevelThresholdId instance
     */
    public static StockLevelThresholdId generate() {
        return new StockLevelThresholdId(UUID.randomUUID());
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
        StockLevelThresholdId that = (StockLevelThresholdId) o;
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

