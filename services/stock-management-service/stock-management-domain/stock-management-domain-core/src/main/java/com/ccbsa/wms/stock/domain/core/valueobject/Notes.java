package com.ccbsa.wms.stock.domain.core.valueobject;

import java.util.Objects;

/**
 * Value Object: Notes
 * <p>
 * Represents optional notes for stock allocations or other domain concepts.
 * Immutable and validated on construction.
 * <p>
 * Business Rules:
 * - Notes can be null (optional field)
 * - If provided, cannot be empty after trimming
 * - Maximum length: 1000 characters
 * - Automatically trimmed
 */
public final class Notes {
    private static final int MAX_LENGTH = 1000;
    private final String value;

    private Notes(String value) {
        if (value != null) {
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                this.value = null;
            } else {
                if (trimmed.length() > MAX_LENGTH) {
                    throw new IllegalArgumentException(String.format("Notes cannot exceed %d characters", MAX_LENGTH));
                }
                this.value = trimmed;
            }
        } else {
            this.value = null;
        }
    }

    /**
     * Creates Notes from a string value. Returns null if value is null or empty.
     *
     * @param value Notes string (can be null or empty)
     * @return Notes instance or null if value is null/empty
     * @throws IllegalArgumentException if value exceeds maximum length
     */
    public static Notes of(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return new Notes(value);
    }

    /**
     * Creates Notes from a string value, allowing null.
     *
     * @param value Notes string (can be null)
     * @return Notes instance or null
     * @throws IllegalArgumentException if value exceeds maximum length
     */
    public static Notes ofNullable(String value) {
        return value == null ? null : new Notes(value);
    }

    /**
     * Returns the string value.
     *
     * @return String value (can be null)
     */
    public String getValue() {
        return value;
    }

    /**
     * Checks if notes are empty or null.
     *
     * @return true if notes are null or empty
     */
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
        Notes notes = (Notes) o;
        return Objects.equals(value, notes.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value == null ? "Notes{null}" : String.format("Notes{value='%s'}", value);
    }
}
