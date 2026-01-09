package com.ccbsa.common.domain.valueobject;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: OrderNumber
 * <p>
 * Represents a unique identifier for an order. Immutable and self-validating.
 * <p>
 * This value object is shared across services (DRY principle).
 * <p>
 * Business Rules:
 * - Order number cannot be null or empty
 * - Order number cannot exceed 50 characters
 * - Order number must be alphanumeric with dashes only
 */
public final class OrderNumber {
    private final String value;

    private OrderNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Order number cannot be null or empty");
        }
        if (value.length() > 50) {
            throw new IllegalArgumentException("Order number cannot exceed 50 characters");
        }
        if (!value.matches("^[A-Za-z0-9-]+$")) {
            throw new IllegalArgumentException("Order number must be alphanumeric with dashes only");
        }
        this.value = value;
    }

    /**
     * Factory method to create OrderNumber from string value.
     *
     * @param value String value (must be alphanumeric with dashes, max 50 chars)
     * @return OrderNumber instance
     * @throws IllegalArgumentException if value is null, empty, or invalid format
     */
    public static OrderNumber of(String value) {
        return new OrderNumber(value);
    }

    /**
     * Generates a new OrderNumber with format "ORD-{UUID8}".
     *
     * @return New OrderNumber instance
     */
    public static OrderNumber generate() {
        return new OrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
    }

    /**
     * Returns the string value.
     *
     * @return String value
     */
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OrderNumber that = (OrderNumber) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
