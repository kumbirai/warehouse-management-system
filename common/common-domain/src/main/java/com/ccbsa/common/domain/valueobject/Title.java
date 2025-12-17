package com.ccbsa.common.domain.valueobject;

import java.util.Objects;

/**
 * Value Object: Title
 * <p>
 * Represents a title for notifications, events, or other domain concepts. Immutable and self-validating.
 * <p>
 * This is a shared value object used across multiple services. Business Rules: - Title cannot be null or empty - Maximum length: 200 characters - Automatically trimmed
 */
public final class Title {
    private final String value;

    /**
     * Private constructor to enforce immutability.
     *
     * @param value String value
     * @throws IllegalArgumentException if value is invalid
     */
    private Title(String value) {
        validate(value);
        this.value = value.trim();
    }

    /**
     * Validates the value according to business rules.
     *
     * @param value Value to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validate(String value) {
        if (value == null || value.trim()
                .isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        if (value.trim()
                .length() > 200) {
            throw new IllegalArgumentException("Title cannot exceed 200 characters");
        }
    }

    /**
     * Factory method to create Title instance.
     *
     * @param value String value
     * @return Title instance
     * @throws IllegalArgumentException if value is invalid
     */
    public static Title of(String value) {
        return new Title(value);
    }

    /**
     * Returns the value.
     *
     * @return String value
     */
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
        Title title = (Title) o;
        return Objects.equals(value, title.value);
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

