package com.ccbsa.wms.picking.domain.core.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: OrderLineItemId
 * <p>
 * Represents a unique identifier for an order line item. Immutable and self-validating.
 */
public final class OrderLineItemId {
    private final UUID value;

    private OrderLineItemId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("OrderLineItemId cannot be null");
        }
        this.value = value;
    }

    /**
     * Factory method to create OrderLineItemId from UUID.
     *
     * @param value UUID value
     * @return OrderLineItemId instance
     * @throws IllegalArgumentException if value is null
     */
    public static OrderLineItemId of(UUID value) {
        return new OrderLineItemId(value);
    }

    /**
     * Factory method to create OrderLineItemId from string.
     *
     * @param value UUID string value
     * @return OrderLineItemId instance
     * @throws IllegalArgumentException if value is null or invalid UUID format
     */
    public static OrderLineItemId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("OrderLineItemId string cannot be null or empty");
        }
        try {
            return new OrderLineItemId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format for OrderLineItemId: %s", value), e);
        }
    }

    /**
     * Creates a new OrderLineItemId with random UUID.
     *
     * @return New OrderLineItemId instance
     */
    public static OrderLineItemId generate() {
        return new OrderLineItemId(UUID.randomUUID());
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
        OrderLineItemId that = (OrderLineItemId) o;
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
