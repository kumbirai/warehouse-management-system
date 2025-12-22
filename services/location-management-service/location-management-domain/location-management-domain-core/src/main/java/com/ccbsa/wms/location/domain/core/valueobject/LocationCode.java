package com.ccbsa.wms.location.domain.core.valueobject;

import java.util.Objects;

/**
 * Value Object: LocationCode
 * <p>
 * Represents a location code (e.g., "WH-53"). Immutable and validated on construction.
 * <p>
 * Business Rules:
 * - Location code is optional (can be null)
 * - If provided, must not be empty after trimming
 * - Maximum length: 100 characters
 */
public final class LocationCode {
    private final String value;

    private LocationCode(String value) {
        if (value != null) {
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                this.value = null;
            } else {
                if (trimmed.length() > 100) {
                    throw new IllegalArgumentException("Location code cannot exceed 100 characters");
                }
                this.value = trimmed;
            }
        } else {
            this.value = null;
        }
    }

    /**
     * Creates a LocationCode from a string value. Returns null if value is null or empty.
     *
     * @param value Location code string (can be null or empty)
     * @return LocationCode instance or null if value is null/empty
     */
    public static LocationCode of(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return new LocationCode(value);
    }

    /**
     * Creates a LocationCode from a string value, allowing null.
     *
     * @param value Location code string (can be null)
     * @return LocationCode instance or null
     */
    public static LocationCode ofNullable(String value) {
        return value == null ? null : new LocationCode(value);
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
        LocationCode that = (LocationCode) o;
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

