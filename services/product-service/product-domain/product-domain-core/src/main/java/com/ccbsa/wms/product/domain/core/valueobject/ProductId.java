package com.ccbsa.wms.product.domain.core.valueobject;

import java.util.UUID;

/**
 * Value Object: ProductId
 *
 * Represents the unique identifier for Product. Immutable and validated on construction.
 */
public final class ProductId {
    private final UUID value;

    /**
     * Private constructor to enforce immutability.
     *
     * @param value UUID value
     * @throws IllegalArgumentException if value is null
     */
    private ProductId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("ProductId value cannot be null");
        }
        this.value = value;
    }

    /**
     * Factory method to create ProductId from UUID.
     *
     * @param value UUID value
     * @return ProductId instance
     * @throws IllegalArgumentException if value is null
     */
    public static ProductId of(UUID value) {
        return new ProductId(value);
    }

    /**
     * Factory method to create ProductId from String.
     *
     * @param value UUID string representation
     * @return ProductId instance
     * @throws IllegalArgumentException if value is invalid
     */
    public static ProductId of(String value) {
        if (value == null || value.trim()
                .isEmpty()) {
            throw new IllegalArgumentException("ProductId string cannot be null or empty");
        }
        try {
            return new ProductId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format: %s", value), e);
        }
    }

    /**
     * Generates a new ProductId with random UUID.
     *
     * @return New ProductId instance
     */
    public static ProductId generate() {
        return new ProductId(UUID.randomUUID());
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
        ProductId that = (ProductId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

