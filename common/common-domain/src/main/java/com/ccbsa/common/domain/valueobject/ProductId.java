package com.ccbsa.common.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: ProductId
 * <p>
 * Represents a unique identifier for a product. Immutable and self-validating.
 * <p>
 * This value object is shared across services (DRY principle).
 */
public final class ProductId {
    private final UUID value;

    private ProductId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("ProductId cannot be null");
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
     * Factory method to create ProductId from string.
     *
     * @param value UUID string value
     * @return ProductId instance
     * @throws IllegalArgumentException if value is null or invalid UUID format
     */
    public static ProductId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("ProductId string cannot be null or empty");
        }
        try {
            return new ProductId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format for ProductId: %s", value), e);
        }
    }

    /**
     * Creates a new ProductId with random UUID.
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
        ProductId productId = (ProductId) o;
        return Objects.equals(value, productId.value);
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

