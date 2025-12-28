package com.ccbsa.common.domain.valueobject;

import java.util.Objects;

/**
 * Value Object: Quantity
 * <p>
 * Represents a quantity value with validation. Immutable and self-validating.
 * <p>
 * This value object is shared across services (DRY principle).
 * <p>
 * Business Rules:
 * - Quantity must be non-negative (>= 0)
 * - Quantity cannot be null
 * - For positive quantities, use positive validation in domain logic
 */
public final class Quantity {
    private final int value;

    private Quantity(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Quantity must be non-negative (>= 0)");
        }
        this.value = value;
    }

    /**
     * Factory method to create Quantity from integer value.
     *
     * @param value Integer value (must be non-negative)
     * @return Quantity instance
     * @throws IllegalArgumentException if value is negative
     */
    public static Quantity of(int value) {
        return new Quantity(value);
    }

    /**
     * Returns the integer value.
     *
     * @return Integer value (always non-negative)
     */
    public int getValue() {
        return value;
    }

    /**
     * Checks if quantity is zero.
     *
     * @return true if quantity is zero
     */
    public boolean isZero() {
        return value == 0;
    }

    /**
     * Checks if quantity is positive.
     *
     * @return true if quantity is greater than zero
     */
    public boolean isPositive() {
        return value > 0;
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
     * @throws IllegalArgumentException if other is null or result would be negative
     */
    public Quantity subtract(Quantity other) {
        if (other == null) {
            throw new IllegalArgumentException("Quantity to subtract cannot be null");
        }
        int result = this.value - other.value;
        if (result < 0) {
            throw new IllegalArgumentException("Resulting quantity cannot be negative");
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

    /**
     * Checks if this quantity is less than another quantity.
     *
     * @param other Quantity to compare
     * @return true if this quantity is less
     * @throws IllegalArgumentException if other is null
     */
    public boolean isLessThan(Quantity other) {
        if (other == null) {
            throw new IllegalArgumentException("Quantity to compare cannot be null");
        }
        return this.value < other.value;
    }

    /**
     * Checks if this quantity is less than or equal to another quantity.
     *
     * @param other Quantity to compare
     * @return true if this quantity is less than or equal
     * @throws IllegalArgumentException if other is null
     */
    public boolean isLessThanOrEqual(Quantity other) {
        if (other == null) {
            throw new IllegalArgumentException("Quantity to compare cannot be null");
        }
        return this.value <= other.value;
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

