package com.ccbsa.common.domain.valueobject;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object: BigDecimalQuantity
 * <p>
 * Represents a quantity value with BigDecimal precision. Immutable and self-validating.
 * <p>
 * This value object is shared across services (DRY principle).
 * <p>
 * Business Rules:
 * - Quantity must be non-negative (>= 0)
 * - Quantity cannot be null
 * - Uses BigDecimal for precise decimal calculations
 * <p>
 * Use this for general quantity operations that require decimal precision.
 * For threshold values, use MinimumQuantity or MaximumQuantity instead.
 */
public final class BigDecimalQuantity {
    private final BigDecimal value;

    private BigDecimalQuantity(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("BigDecimalQuantity cannot be null");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("BigDecimalQuantity must be non-negative (>= 0)");
        }
        this.value = value;
    }

    /**
     * Factory method to create BigDecimalQuantity from BigDecimal value.
     *
     * @param value BigDecimal value (must be non-negative)
     * @return BigDecimalQuantity instance
     * @throws IllegalArgumentException if value is null or negative
     */
    public static BigDecimalQuantity of(BigDecimal value) {
        return new BigDecimalQuantity(value);
    }

    /**
     * Factory method to create BigDecimalQuantity from integer value.
     *
     * @param value Integer value (must be non-negative)
     * @return BigDecimalQuantity instance
     * @throws IllegalArgumentException if value is negative
     */
    public static BigDecimalQuantity of(int value) {
        return new BigDecimalQuantity(BigDecimal.valueOf(value));
    }

    /**
     * Factory method to create BigDecimalQuantity from double value.
     *
     * @param value Double value (must be non-negative)
     * @return BigDecimalQuantity instance
     * @throws IllegalArgumentException if value is negative
     */
    public static BigDecimalQuantity of(double value) {
        return new BigDecimalQuantity(BigDecimal.valueOf(value));
    }

    /**
     * Factory method to create BigDecimalQuantity from string value.
     *
     * @param value String representation of BigDecimal (must be non-negative)
     * @return BigDecimalQuantity instance
     * @throws IllegalArgumentException if value is null, empty, or invalid
     */
    public static BigDecimalQuantity of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("BigDecimalQuantity string cannot be null or empty");
        }
        try {
            return new BigDecimalQuantity(new BigDecimal(value.trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Invalid BigDecimal format for BigDecimalQuantity: %s", value), e);
        }
    }

    /**
     * Creates a BigDecimalQuantity with zero value.
     *
     * @return BigDecimalQuantity instance with value zero
     */
    public static BigDecimalQuantity zero() {
        return new BigDecimalQuantity(BigDecimal.ZERO);
    }

    /**
     * Returns the BigDecimal value.
     *
     * @return BigDecimal value (always non-negative)
     */
    public BigDecimal getValue() {
        return value;
    }

    /**
     * Checks if quantity is zero.
     *
     * @return true if quantity is zero
     */
    public boolean isZero() {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Checks if quantity is positive.
     *
     * @return true if quantity is greater than zero
     */
    public boolean isPositive() {
        return value.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Adds another quantity to this quantity.
     *
     * @param other Quantity to add
     * @return New BigDecimalQuantity instance with sum
     * @throws IllegalArgumentException if other is null
     */
    public BigDecimalQuantity add(BigDecimalQuantity other) {
        if (other == null) {
            throw new IllegalArgumentException("Quantity to add cannot be null");
        }
        return new BigDecimalQuantity(this.value.add(other.value));
    }

    /**
     * Adds a BigDecimal value to this quantity.
     *
     * @param other BigDecimal value to add (must be non-negative)
     * @return New BigDecimalQuantity instance with sum
     * @throws IllegalArgumentException if other is null or negative
     */
    public BigDecimalQuantity add(BigDecimal other) {
        if (other == null) {
            throw new IllegalArgumentException("BigDecimal to add cannot be null");
        }
        if (other.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("BigDecimal to add must be non-negative");
        }
        return new BigDecimalQuantity(this.value.add(other));
    }

    /**
     * Subtracts another quantity from this quantity.
     *
     * @param other Quantity to subtract
     * @return New BigDecimalQuantity instance with difference
     * @throws IllegalArgumentException if other is null or result would be negative
     */
    public BigDecimalQuantity subtract(BigDecimalQuantity other) {
        if (other == null) {
            throw new IllegalArgumentException("Quantity to subtract cannot be null");
        }
        BigDecimal result = this.value.subtract(other.value);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(String.format("Subtraction would result in negative quantity. This: %s, Other: %s", this.value, other.value));
        }
        return new BigDecimalQuantity(result);
    }

    /**
     * Subtracts a BigDecimal value from this quantity.
     *
     * @param other BigDecimal value to subtract (must be non-negative)
     * @return New BigDecimalQuantity instance with difference
     * @throws IllegalArgumentException if other is null, negative, or result would be negative
     */
    public BigDecimalQuantity subtract(BigDecimal other) {
        if (other == null) {
            throw new IllegalArgumentException("BigDecimal to subtract cannot be null");
        }
        if (other.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("BigDecimal to subtract must be non-negative");
        }
        BigDecimal result = this.value.subtract(other);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(String.format("Subtraction would result in negative quantity. This: %s, Other: %s", this.value, other));
        }
        return new BigDecimalQuantity(result);
    }

    /**
     * Checks if this quantity is greater than another quantity.
     *
     * @param other Quantity to compare
     * @return true if this quantity is greater
     * @throws IllegalArgumentException if other is null
     */
    public boolean isGreaterThan(BigDecimalQuantity other) {
        if (other == null) {
            throw new IllegalArgumentException("Quantity to compare cannot be null");
        }
        return this.value.compareTo(other.value) > 0;
    }

    /**
     * Checks if this quantity is greater than a BigDecimal value.
     *
     * @param other BigDecimal value to compare
     * @return true if this quantity is greater
     * @throws IllegalArgumentException if other is null
     */
    public boolean isGreaterThan(BigDecimal other) {
        if (other == null) {
            throw new IllegalArgumentException("BigDecimal to compare cannot be null");
        }
        return this.value.compareTo(other) > 0;
    }

    /**
     * Checks if this quantity is greater than or equal to another quantity.
     *
     * @param other Quantity to compare
     * @return true if this quantity is greater than or equal
     * @throws IllegalArgumentException if other is null
     */
    public boolean isGreaterThanOrEqual(BigDecimalQuantity other) {
        if (other == null) {
            throw new IllegalArgumentException("Quantity to compare cannot be null");
        }
        return this.value.compareTo(other.value) >= 0;
    }

    /**
     * Checks if this quantity is greater than or equal to a BigDecimal value.
     *
     * @param other BigDecimal value to compare
     * @return true if this quantity is greater than or equal
     * @throws IllegalArgumentException if other is null
     */
    public boolean isGreaterThanOrEqual(BigDecimal other) {
        if (other == null) {
            throw new IllegalArgumentException("BigDecimal to compare cannot be null");
        }
        return this.value.compareTo(other) >= 0;
    }

    /**
     * Checks if this quantity is less than another quantity.
     *
     * @param other Quantity to compare
     * @return true if this quantity is less
     * @throws IllegalArgumentException if other is null
     */
    public boolean isLessThan(BigDecimalQuantity other) {
        if (other == null) {
            throw new IllegalArgumentException("Quantity to compare cannot be null");
        }
        return this.value.compareTo(other.value) < 0;
    }

    /**
     * Checks if this quantity is less than a BigDecimal value.
     *
     * @param other BigDecimal value to compare
     * @return true if this quantity is less
     * @throws IllegalArgumentException if other is null
     */
    public boolean isLessThan(BigDecimal other) {
        if (other == null) {
            throw new IllegalArgumentException("BigDecimal to compare cannot be null");
        }
        return this.value.compareTo(other) < 0;
    }

    /**
     * Checks if this quantity is less than or equal to another quantity.
     *
     * @param other Quantity to compare
     * @return true if this quantity is less than or equal
     * @throws IllegalArgumentException if other is null
     */
    public boolean isLessThanOrEqual(BigDecimalQuantity other) {
        if (other == null) {
            throw new IllegalArgumentException("Quantity to compare cannot be null");
        }
        return this.value.compareTo(other.value) <= 0;
    }

    /**
     * Checks if this quantity is less than or equal to a BigDecimal value.
     *
     * @param other BigDecimal value to compare
     * @return true if this quantity is less than or equal
     * @throws IllegalArgumentException if other is null
     */
    public boolean isLessThanOrEqual(BigDecimal other) {
        if (other == null) {
            throw new IllegalArgumentException("BigDecimal to compare cannot be null");
        }
        return this.value.compareTo(other) <= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BigDecimalQuantity that = (BigDecimalQuantity) o;
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
