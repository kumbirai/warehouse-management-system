package com.ccbsa.wms.location.domain.core.valueobject;

import java.util.Objects;

/**
 * Value Object: LocationName
 * <p>
 * Represents a location name. Immutable and validated on construction.
 * <p>
 * Business Rules:
 * - Location name is optional (can be null)
 * - If provided, must not be empty after trimming
 * - Maximum length: 200 characters
 */
public final class LocationName {
    private final String value;

    private LocationName(String value) {
        if (value != null) {
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                this.value = null;
            } else {
                if (trimmed.length() > 200) {
                    throw new IllegalArgumentException("Location name cannot exceed 200 characters");
                }
                this.value = trimmed;
            }
        } else {
            this.value = null;
        }
    }

    /**
     * Creates a LocationName from a string value. Returns null if value is null or empty.
     *
     * @param value Location name string (can be null or empty)
     * @return LocationName instance or null if value is null/empty
     */
    public static LocationName of(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return new LocationName(value);
    }

    /**
     * Creates a LocationName from a string value, allowing null.
     *
     * @param value Location name string (can be null)
     * @return LocationName instance or null
     */
    public static LocationName ofNullable(String value) {
        return value == null ? null : new LocationName(value);
    }

    public String getValue() {
        return value;
    }

    public boolean isEmpty() {
        return value == null || value.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LocationName that = (LocationName) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value != null ? value : "";
    }
}

