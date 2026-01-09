package com.ccbsa.wms.picking.domain.core.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: OrderId
 * <p>
 * Represents a unique identifier for an order. Immutable and self-validating.
 */
public final class OrderId {
    private final UUID value;

    private OrderId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("OrderId cannot be null");
        }
        this.value = value;
    }

    /**
     * Factory method to create OrderId from UUID.
     *
     * @param value UUID value
     * @return OrderId instance
     * @throws IllegalArgumentException if value is null
     */
    public static OrderId of(UUID value) {
        return new OrderId(value);
    }

    /**
     * Factory method to create OrderId from string.
     *
     * @param value UUID string value
     * @return OrderId instance
     * @throws IllegalArgumentException if value is null or invalid UUID format
     */
    public static OrderId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("OrderId string cannot be null or empty");
        }
        try {
            return new OrderId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format for OrderId: %s", value), e);
        }
    }

    /**
     * Creates a new OrderId with random UUID.
     *
     * @return New OrderId instance
     */
    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
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
     * Returns the UUID value as string.
     *
     * @return UUID string value
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
        OrderId that = (OrderId) o;
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
