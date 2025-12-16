package com.ccbsa.wms.location.domain.core.valueobject;

import java.util.UUID;

/**
 * Value Object: LocationId
 *
 * Represents the unique identifier for Location.
 * Immutable and validated on construction.
 */
public final class LocationId {
    private final UUID value;

    /**
     * Private constructor to enforce immutability.
     *
     * @param value UUID value
     * @throws IllegalArgumentException if value is null
     */
    private LocationId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("LocationId value cannot be null");
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
     * Factory method to create LocationId from String.
     *
     * @param value UUID string representation
     * @return LocationId instance
     * @throws IllegalArgumentException if value is invalid
     */
    public static LocationId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("LocationId string cannot be null or empty");
        }
        try {
            return new LocationId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format: %s", value), e);
        }
    }

    /**
     * Generates a new LocationId with random UUID.
     *
     * @return New LocationId instance
     */
    public static LocationId generate() {
        return new LocationId(UUID.randomUUID());
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
        LocationId that = (LocationId) o;
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

