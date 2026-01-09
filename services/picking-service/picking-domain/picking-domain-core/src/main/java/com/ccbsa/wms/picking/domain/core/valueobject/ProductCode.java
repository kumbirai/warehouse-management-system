package com.ccbsa.wms.picking.domain.core.valueobject;

import java.util.Objects;

/**
 * Value Object: ProductCode
 * <p>
 * Represents a product code. Immutable and self-validating.
 * <p>
 * Business Rules:
 * - Product code cannot be null or empty
 * - Product code cannot exceed 100 characters
 */
public final class ProductCode {
    private final String value;

    private ProductCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Product code cannot be null or empty");
        }
        if (value.length() > 100) {
            throw new IllegalArgumentException("Product code cannot exceed 100 characters");
        }
        this.value = value.trim();
    }

    /**
     * Factory method to create ProductCode from string value.
     *
     * @param value String value (must not be null or empty, max 100 chars)
     * @return ProductCode instance
     * @throws IllegalArgumentException if value is null, empty, or invalid format
     */
    public static ProductCode of(String value) {
        return new ProductCode(value);
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
        ProductCode that = (ProductCode) o;
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
