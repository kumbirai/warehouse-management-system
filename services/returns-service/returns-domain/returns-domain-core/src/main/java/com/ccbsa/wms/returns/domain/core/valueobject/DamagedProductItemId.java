package com.ccbsa.wms.returns.domain.core.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: DamagedProductItemId
 * <p>
 * Represents a unique identifier for a damaged product item. Immutable and self-validating.
 */
public final class DamagedProductItemId {
    private final UUID value;

    private DamagedProductItemId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("DamagedProductItemId cannot be null");
        }
        this.value = value;
    }

    /**
     * Factory method to create DamagedProductItemId from UUID.
     *
     * @param value UUID value
     * @return DamagedProductItemId instance
     * @throws IllegalArgumentException if value is null
     */
    public static DamagedProductItemId of(UUID value) {
        return new DamagedProductItemId(value);
    }

    /**
     * Factory method to create DamagedProductItemId from string.
     *
     * @param value UUID string value
     * @return DamagedProductItemId instance
     * @throws IllegalArgumentException if value is null or invalid UUID format
     */
    public static DamagedProductItemId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("DamagedProductItemId string cannot be null or empty");
        }
        try {
            return new DamagedProductItemId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format for DamagedProductItemId: %s", value), e);
        }
    }

    /**
     * Creates a new DamagedProductItemId with random UUID.
     *
     * @return New DamagedProductItemId instance
     */
    public static DamagedProductItemId generate() {
        return new DamagedProductItemId(UUID.randomUUID());
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
        DamagedProductItemId that = (DamagedProductItemId) o;
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
