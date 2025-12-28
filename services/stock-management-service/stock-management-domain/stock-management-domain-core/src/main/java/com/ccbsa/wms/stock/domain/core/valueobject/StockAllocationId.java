package com.ccbsa.wms.stock.domain.core.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: StockAllocationId
 * <p>
 * Represents a unique identifier for stock allocation. Immutable and validated on construction.
 */
public final class StockAllocationId {
    private final UUID value;

    /**
     * Private constructor to enforce immutability.
     *
     * @param value UUID value
     * @throws IllegalArgumentException if value is null
     */
    private StockAllocationId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("StockAllocationId value cannot be null");
        }
        this.value = value;
    }

    /**
     * Factory method to create StockAllocationId from UUID.
     *
     * @param value UUID value
     * @return StockAllocationId instance
     * @throws IllegalArgumentException if value is null
     */
    public static StockAllocationId of(UUID value) {
        return new StockAllocationId(value);
    }

    /**
     * Factory method to create StockAllocationId from String.
     *
     * @param value UUID string representation
     * @return StockAllocationId instance
     * @throws IllegalArgumentException if value is invalid
     */
    public static StockAllocationId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("StockAllocationId string cannot be null or empty");
        }
        try {
            return new StockAllocationId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format: %s", value), e);
        }
    }

    /**
     * Generates a new StockAllocationId with random UUID.
     *
     * @return New StockAllocationId instance
     */
    public static StockAllocationId generate() {
        return new StockAllocationId(UUID.randomUUID());
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
        StockAllocationId that = (StockAllocationId) o;
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

