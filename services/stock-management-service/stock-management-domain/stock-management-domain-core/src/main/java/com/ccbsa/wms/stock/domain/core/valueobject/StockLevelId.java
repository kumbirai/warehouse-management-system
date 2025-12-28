package com.ccbsa.wms.stock.domain.core.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: StockLevelId
 * <p>
 * Represents a unique identifier for stock level. Immutable and validated on construction.
 */
public final class StockLevelId {
    private final UUID value;

    /**
     * Private constructor to enforce immutability.
     *
     * @param value UUID value
     * @throws IllegalArgumentException if value is null
     */
    private StockLevelId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("StockLevelId value cannot be null");
        }
        this.value = value;
    }

    /**
     * Factory method to create StockLevelId from UUID.
     *
     * @param value UUID value
     * @return StockLevelId instance
     * @throws IllegalArgumentException if value is null
     */
    public static StockLevelId of(UUID value) {
        return new StockLevelId(value);
    }

    /**
     * Factory method to create StockLevelId from String.
     *
     * @param value UUID string representation
     * @return StockLevelId instance
     * @throws IllegalArgumentException if value is invalid
     */
    public static StockLevelId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("StockLevelId string cannot be null or empty");
        }
        try {
            return new StockLevelId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format: %s", value), e);
        }
    }

    /**
     * Generates a new StockLevelId with random UUID.
     *
     * @return New StockLevelId instance
     */
    public static StockLevelId generate() {
        return new StockLevelId(UUID.randomUUID());
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
        StockLevelId that = (StockLevelId) o;
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

