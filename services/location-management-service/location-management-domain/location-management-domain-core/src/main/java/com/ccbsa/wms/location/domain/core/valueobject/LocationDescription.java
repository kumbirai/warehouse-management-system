package com.ccbsa.wms.location.domain.core.valueobject;

import java.util.Objects;

/**
 * Value Object: LocationDescription
 * <p>
 * Represents a location description. Immutable and validated on construction.
 * <p>
 * Business Rules:
 * - Location description is optional (can be null)
 * - If provided, must not be empty after trimming
 * - Maximum length: 500 characters
 */
public final class LocationDescription {
    private final String value;

    private LocationDescription(String value) {
        if (value != null) {
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                this.value = null;
            } else {
                if (trimmed.length() > 500) {
                    throw new IllegalArgumentException("Location description cannot exceed 500 characters");
                }
                this.value = trimmed;
            }
        } else {
            this.value = null;
        }
    }

    /**
     * Creates a LocationDescription from a string value. Returns null if value is null or empty.
     *
     * @param value Location description string (can be null or empty)
     * @return LocationDescription instance or null if value is null/empty
     */
    public static LocationDescription of(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return new LocationDescription(value);
    }

    /**
     * Creates a LocationDescription from a string value, allowing null.
     *
     * @param value Location description string (can be null)
     * @return LocationDescription instance or null
     */
    public static LocationDescription ofNullable(String value) {
        return value == null ? null : new LocationDescription(value);
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
        LocationDescription that = (LocationDescription) o;
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

