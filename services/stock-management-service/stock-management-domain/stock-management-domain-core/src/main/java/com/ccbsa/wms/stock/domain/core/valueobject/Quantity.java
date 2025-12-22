package com.ccbsa.wms.stock.domain.core.valueobject;

import java.util.Objects;

/**
 * Value Object: Quantity
 * <p>
 * Represents a quantity value with validation. Immutable and self-validating.
 * <p>
 * Business Rules:
 * - Quantity must be positive (> 0)
 * - Quantity cannot be null
 */
public final class Quantity {
    private final int value;

    private Quantity(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.value = value;
    }

    /**
     * Factory method to create Quantity from integer value.
     *
     * @param value Integer value (must be positive)
     * @return Quantity instance
     * @throws IllegalArgumentException if value is not positive
     */
    public static Quantity of(int value) {
        return new Quantity(value);
    }

    /**
     * Returns the integer value.
     *
     * @return Integer value (always positive)
     */
    public int getValue() {
        return value;
    }

    /**
     * Adds another quantity to this quantity.
     *
     * @param other Quantity to add
     * @return New Quantity instance with sum
     * @throws IllegalArgumentException if other is null
     */
    public Quantity add(Quantity other) {
        if (other == null) {
            throw new IllegalArgumentException("Quantity to add cannot be null");
        }
        return new Quantity(this.value + other.value);
    }

    /**
     * Subtracts another quantity from this quantity.
     *
     * @param other Quantity to subtract
     * @return New Quantity instance with difference
     * @throws IllegalArgumentException if other is null or result would be non-positive
     */
    public Quantity subtract(Quantity other) {
        if (other == null) {
            throw new IllegalArgumentException("Quantity to subtract cannot be null");
        }
        int result = this.value - other.value;
        if (result <= 0) {
            throw new IllegalArgumentException("Resulting quantity must be positive");
        }
        return new Quantity(result);
    }

    /**
     * Checks if this quantity is greater than another quantity.
     *
     * @param other Quantity to compare
     * @return true if this quantity is greater
     * @throws IllegalArgumentException if other is null
     */
    public boolean isGreaterThan(Quantity other) {
        if (other == null) {
            throw new IllegalArgumentException("Quantity to compare cannot be null");
        }
        return this.value > other.value;
    }

    /**
     * Checks if this quantity is greater than or equal to another quantity.
     *
     * @param other Quantity to compare
     * @return true if this quantity is greater than or equal
     * @throws IllegalArgumentException if other is null
     */
    public boolean isGreaterThanOrEqual(Quantity other) {
        if (other == null) {
            throw new IllegalArgumentException("Quantity to compare cannot be null");
        }
        return this.value >= other.value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Quantity quantity = (Quantity) o;
        return value == quantity.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}

