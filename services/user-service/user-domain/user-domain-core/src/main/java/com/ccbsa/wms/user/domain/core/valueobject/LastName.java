package com.ccbsa.wms.user.domain.core.valueobject;

import java.util.Objects;

/**
 * Value Object: LastName
 * <p>
 * Represents a person's last name. Immutable and validated on construction.
 * <p>
 * Business Rules: - Last name is optional (can be null) - If provided, must not be empty after trimming - Maximum length: 100 characters
 */
public final class LastName {
    private final String value;

    private LastName(String value) {
        if (value != null) {
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                this.value = null;
            } else {
                if (trimmed.length() > 100) {
                    throw new IllegalArgumentException("Last name cannot exceed 100 characters");
                }
                this.value = trimmed;
            }
        } else {
            this.value = null;
        }
    }

    /**
     * Creates a LastName from a string value. Returns null if value is null or empty.
     *
     * @param value Last name string (can be null or empty)
     * @return LastName instance or null if value is null/empty
     */
    public static LastName of(String value) {
        if (value == null || value.trim()
                .isEmpty()) {
            return null;
        }
        return new LastName(value);
    }

    /**
     * Creates a LastName from a string value, allowing null.
     *
     * @param value Last name string (can be null)
     * @return LastName instance or null
     */
    public static LastName ofNullable(String value) {
        return value == null ? null : new LastName(value);
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
        LastName lastName = (LastName) o;
        return Objects.equals(value, lastName.value);
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

