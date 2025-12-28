package com.ccbsa.common.domain.valueobject;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object: MaximumQuantity
 * <p>
 * Represents a maximum stock quantity threshold. Immutable and self-validating.
 * <p>
 * This value object is shared across services (DRY principle).
 * <p>
 * Business Rules:
 * - MaximumQuantity must be positive (> 0)
 * - MaximumQuantity must be greater than minimum quantity (validated when used together)
 * - MaximumQuantity cannot be null
 * - Uses BigDecimal for precise decimal calculations
 */
public final class MaximumQuantity {
    private final BigDecimal value;

    private MaximumQuantity(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("MaximumQuantity cannot be null");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("MaximumQuantity must be positive (> 0)");
        }
        this.value = value;
    }

    /**
     * Creates a MaximumQuantity from a BigDecimal value.
     *
     * @param value BigDecimal value (must be positive)
     * @return MaximumQuantity instance
     * @throws IllegalArgumentException if value is null or not positive
     */
    public static MaximumQuantity of(BigDecimal value) {
        return new MaximumQuantity(value);
    }

    /**
     * Creates a MaximumQuantity from an integer value.
     *
     * @param value Integer value (must be positive)
     * @return MaximumQuantity instance
     * @throws IllegalArgumentException if value is not positive
     */
    public static MaximumQuantity of(int value) {
        return new MaximumQuantity(BigDecimal.valueOf(value));
    }

    /**
     * Creates a MaximumQuantity from a double value.
     *
     * @param value Double value (must be positive)
     * @return MaximumQuantity instance
     * @throws IllegalArgumentException if value is not positive
     */
    public static MaximumQuantity of(double value) {
        return new MaximumQuantity(BigDecimal.valueOf(value));
    }

    /**
     * Validates that this maximum quantity is greater than the minimum quantity.
     *
     * @param minimumQuantity Minimum quantity to validate against
     * @throws IllegalArgumentException if maximum is not greater than minimum
     */
    public void validateGreaterThanMinimum(MinimumQuantity minimumQuantity) {
        if (minimumQuantity == null) {
            throw new IllegalArgumentException("MinimumQuantity cannot be null");
        }
        if (this.value.compareTo(minimumQuantity.getValue()) <= 0) {
            throw new IllegalArgumentException("MaximumQuantity must be greater than MinimumQuantity. " + "Maximum: " + this.value + ", Minimum: " + minimumQuantity.getValue());
        }
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
     * Checks if this maximum quantity is greater than another quantity.
     *
     * @param other Quantity to compare
     * @return true if this maximum is greater than the other quantity
     * @throws IllegalArgumentException if other is null
     */
    public boolean isGreaterThan(BigDecimal other) {
        if (other == null) {
            throw new IllegalArgumentException("Quantity to compare cannot be null");
        }
        return this.value.compareTo(other) > 0;
    }

    /**
     * Checks if this maximum quantity is greater than or equal to another quantity.
     *
     * @param other Quantity to compare
     * @return true if this maximum is greater than or equal to the other quantity
     * @throws IllegalArgumentException if other is null
     */
    public boolean isGreaterThanOrEqual(BigDecimal other) {
        if (other == null) {
            throw new IllegalArgumentException("Quantity to compare cannot be null");
        }
        return this.value.compareTo(other) >= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MaximumQuantity that = (MaximumQuantity) o;
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

