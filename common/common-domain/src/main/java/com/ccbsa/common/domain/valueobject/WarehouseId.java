package com.ccbsa.common.domain.valueobject;

import java.util.Objects;

/**
 * Value Object: WarehouseId
 * <p>
 * Represents a warehouse identifier. Immutable and self-validating.
 * <p>
 * This is a shared value object used across multiple services. Business Rules: - Cannot be null or empty - Maximum length: 50 characters
 */
public final class WarehouseId {
    private static final int MAX_LENGTH = 50;

    private final String value;

    private WarehouseId(String value) {
        validate(value);
        this.value = value.trim();
    }

    /**
     * Validates the warehouse ID.
     *
     * @param value Warehouse ID value
     * @throws IllegalArgumentException if validation fails
     */
    private void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("WarehouseId cannot be null or empty");
        }
        if (value.trim().length() > MAX_LENGTH) {
            throw new IllegalArgumentException(String.format("WarehouseId cannot exceed %d characters", MAX_LENGTH));
        }
    }

    /**
     * Factory method to create WarehouseId instance.
     *
     * @param value Warehouse ID string value
     * @return WarehouseId instance
     * @throws IllegalArgumentException if value is invalid
     */
    public static WarehouseId of(String value) {
        return new WarehouseId(value);
    }

    /**
     * Returns the warehouse ID value.
     *
     * @return Warehouse ID string value
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
        WarehouseId that = (WarehouseId) o;
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

