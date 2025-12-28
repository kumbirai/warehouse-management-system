package com.ccbsa.common.domain.valueobject;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object: MinimumQuantity
 * <p>
 * Represents a minimum stock quantity threshold. Immutable and self-validating.
 * <p>
 * This value object is shared across services (DRY principle).
 * <p>
 * Business Rules:
 * - MinimumQuantity must be positive (> 0)
 * - MinimumQuantity cannot be null
 * - Uses BigDecimal for precise decimal calculations
 */
public final class MinimumQuantity {
    private final BigDecimal value;

    private MinimumQuantity(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("MinimumQuantity cannot be null");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("MinimumQuantity must be positive (> 0)");
        }
        this.value = value;
    }

    /**
     * Creates a MinimumQuantity from a BigDecimal value.
     *
     * @param value BigDecimal value (must be positive)
     * @return MinimumQuantity instance
     * @throws IllegalArgumentException if value is null or not positive
     */
    public static MinimumQuantity of(BigDecimal value) {
        return new MinimumQuantity(value);
    }

    /**
     * Creates a MinimumQuantity from an integer value.
     *
     * @param value Integer value (must be positive)
     * @return MinimumQuantity instance
     * @throws IllegalArgumentException if value is not positive
     */
    public static MinimumQuantity of(int value) {
        return new MinimumQuantity(BigDecimal.valueOf(value));
    }

    /**
     * Creates a MinimumQuantity from a double value.
     *
     * @param value Double value (must be positive)
     * @return MinimumQuantity instance
     * @throws IllegalArgumentException if value is not positive
     */
    public static MinimumQuantity of(double value) {
        return new MinimumQuantity(BigDecimal.valueOf(value));
    }

    /**
     * Returns the BigDecimal value.
     *
     * @return BigDecimal value (always positive)
     */
    public BigDecimal getValue() {
        return value;
    }

    /**
     * Checks if this minimum quantity is less than another quantity.
     *
     * @param other Quantity to compare
     * @return true if this minimum is less than the other quantity
     * @throws IllegalArgumentException if other is null
     */
    public boolean isLessThan(BigDecimal other) {
        if (other == null) {
            throw new IllegalArgumentException("Quantity to compare cannot be null");
        }
        return this.value.compareTo(other) < 0;
    }

    /**
     * Checks if this minimum quantity is less than or equal to another quantity.
     *
     * @param other Quantity to compare
     * @return true if this minimum is less than or equal to the other quantity
     * @throws IllegalArgumentException if other is null
     */
    public boolean isLessThanOrEqual(BigDecimal other) {
        if (other == null) {
            throw new IllegalArgumentException("Quantity to compare cannot be null");
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
        MinimumQuantity that = (MinimumQuantity) o;
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

