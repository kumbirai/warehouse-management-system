package com.ccbsa.common.domain.valueobject;

import java.util.Objects;

/**
 * Value Object: Description
 * <p>
 * Represents a description text for domain events or other domain concepts.
 * Immutable and validated on construction.
 * <p>
 * This is a shared value object used across multiple services.
 * Business Rules:
 * - Description cannot be null or empty
 * - Maximum length: 500 characters
 * - Automatically trimmed
 */
public final class Description {
    private final String value;

    private Description(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 500) {
            throw new IllegalArgumentException("Description cannot exceed 500 characters");
        }
        this.value = trimmed;
    }

    /**
     * Creates a Description from a string value.
     *
     * @param value Description string (must not be null or empty)
     * @return Description instance
     * @throws IllegalArgumentException if value is null or empty
     */
    public static Description of(String value) {
        return new Description(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Description that = (Description) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

