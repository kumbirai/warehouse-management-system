package com.ccbsa.wms.picking.domain.core.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: LocationId
 * <p>
 * Represents a unique identifier for a warehouse location. Immutable and self-validating.
 */
public final class LocationId {
    private final UUID value;

    private LocationId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("LocationId cannot be null");
        }
        this.value = value;
    }

    /**
     * Factory method to create LocationId from UUID.
     *
     * @param value UUID value
     * @return LocationId instance
     * @throws IllegalArgumentException if value is null
     */
    public static LocationId of(UUID value) {
        return new LocationId(value);
    }

    /**
     * Factory method to create LocationId from string.
     *
     * @param value UUID string value
     * @return LocationId instance
     * @throws IllegalArgumentException if value is null or invalid UUID format
     */
    public static LocationId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("LocationId string cannot be null or empty");
        }
        try {
            return new LocationId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format for LocationId: %s", value), e);
        }
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
        LocationId that = (LocationId) o;
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
