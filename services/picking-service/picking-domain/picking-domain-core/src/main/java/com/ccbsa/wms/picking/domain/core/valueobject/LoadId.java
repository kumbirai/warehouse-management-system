package com.ccbsa.wms.picking.domain.core.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: LoadId
 * <p>
 * Represents a unique identifier for a load. Immutable and self-validating.
 */
public final class LoadId {
    private final UUID value;

    private LoadId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("LoadId cannot be null");
        }
        this.value = value;
    }

    /**
     * Factory method to create LoadId from UUID.
     *
     * @param value UUID value
     * @return LoadId instance
     * @throws IllegalArgumentException if value is null
     */
    public static LoadId of(UUID value) {
        return new LoadId(value);
    }

    /**
     * Factory method to create LoadId from string.
     *
     * @param value UUID string value
     * @return LoadId instance
     * @throws IllegalArgumentException if value is null or invalid UUID format
     */
    public static LoadId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("LoadId string cannot be null or empty");
        }
        try {
            return new LoadId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format for LoadId: %s", value), e);
        }
    }

    /**
     * Creates a new LoadId with random UUID.
     *
     * @return New LoadId instance
     */
    public static LoadId generate() {
        return new LoadId(UUID.randomUUID());
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
        LoadId that = (LoadId) o;
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
