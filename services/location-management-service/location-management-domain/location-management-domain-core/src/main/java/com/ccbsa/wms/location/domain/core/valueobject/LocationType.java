package com.ccbsa.wms.location.domain.core.valueobject;

import java.util.Locale;
import java.util.Objects;

/**
 * Value Object: LocationType
 * <p>
 * Represents a location type (WAREHOUSE, ZONE, AISLE, RACK, BIN). Immutable and validated on construction.
 * <p>
 * Business Rules:
 * - Location type is optional (can be null)
 * - If provided, must not be empty after trimming
 * - Maximum length: 50 characters
 * - Should be one of: WAREHOUSE, ZONE, AISLE, RACK, BIN (validation at application service layer)
 */
public final class LocationType {
    private final String value;

    private LocationType(String value) {
        if (value != null) {
            String trimmed = value.trim().toUpperCase(Locale.ROOT);
            if (trimmed.isEmpty()) {
                this.value = null;
            } else {
                if (trimmed.length() > 50) {
                    throw new IllegalArgumentException("Location type cannot exceed 50 characters");
                }
                this.value = trimmed;
            }
        } else {
            this.value = null;
        }
    }

    /**
     * Creates a LocationType from a string value. Returns null if value is null or empty.
     *
     * @param value Location type string (can be null or empty)
     * @return LocationType instance or null if value is null/empty
     */
    public static LocationType of(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return new LocationType(value);
    }

    /**
     * Creates a LocationType from a string value, allowing null.
     *
     * @param value Location type string (can be null)
     * @return LocationType instance or null
     */
    public static LocationType ofNullable(String value) {
        return value == null ? null : new LocationType(value);
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
        LocationType that = (LocationType) o;
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

