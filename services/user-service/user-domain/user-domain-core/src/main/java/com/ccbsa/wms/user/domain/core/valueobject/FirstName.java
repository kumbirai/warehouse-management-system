package com.ccbsa.wms.user.domain.core.valueobject;

import java.util.Objects;

/**
 * Value Object: FirstName
 * <p>
 * Represents a person's first name. Immutable and validated on construction.
 * <p>
 * Business Rules: - First name is optional (can be null) - If provided, must not be empty after trimming - Maximum length: 100 characters
 */
public final class FirstName {
    private final String value;

    private FirstName(String value) {
        if (value != null) {
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                this.value = null;
            } else {
                if (trimmed.length() > 100) {
                    throw new IllegalArgumentException("First name cannot exceed 100 characters");
                }
                this.value = trimmed;
            }
        } else {
            this.value = null;
        }
    }

    /**
     * Creates a FirstName from a string value. Returns null if value is null or empty.
     *
     * @param value First name string (can be null or empty)
     * @return FirstName instance or null if value is null/empty
     */
    public static FirstName of(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return new FirstName(value);
    }

    /**
     * Creates a FirstName from a string value, allowing null.
     *
     * @param value First name string (can be null)
     * @return FirstName instance or null
     */
    public static FirstName ofNullable(String value) {
        return value == null ? null : new FirstName(value);
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
        FirstName firstName = (FirstName) o;
        return Objects.equals(value, firstName.value);
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

