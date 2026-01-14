package com.ccbsa.common.domain.valueobject;

import java.util.Objects;

/**
 * Value Object: Notes
 * <p>
 * Represents optional notes text with length validation.
 * Immutable and self-validating.
 */
public final class Notes {
    private final String value;
    private final int maxLength;

    private Notes(String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(String.format("Notes cannot exceed %d characters", maxLength));
        }
        this.value = value;
        this.maxLength = maxLength;
    }

    /**
     * Factory method to create Notes with default max length (2000).
     *
     * @param value Notes text (can be null for optional notes)
     * @return Notes instance
     * @throws IllegalArgumentException if value exceeds max length
     */
    public static Notes of(String value) {
        return new Notes(value, 2000);
    }

    /**
     * Factory method to create Notes with custom max length.
     *
     * @param value     Notes text (can be null for optional notes)
     * @param maxLength Maximum allowed length
     * @return Notes instance
     * @throws IllegalArgumentException if value exceeds max length
     */
    public static Notes of(String value, int maxLength) {
        if (maxLength <= 0) {
            throw new IllegalArgumentException("Max length must be positive");
        }
        return new Notes(value, maxLength);
    }

    /**
     * Factory method to create Notes for line items (max 1000 characters).
     *
     * @param value Notes text (can be null for optional notes)
     * @return Notes instance
     * @throws IllegalArgumentException if value exceeds max length
     */
    public static Notes forLineItem(String value) {
        return new Notes(value, 1000);
    }

    /**
     * Returns the notes value, or null if not set.
     *
     * @return Notes text or null
     */
    public String getValue() {
        return value;
    }

    /**
     * Checks if notes are present.
     *
     * @return true if notes are not null and not empty
     */
    public boolean isPresent() {
        return value != null && !value.trim().isEmpty();
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
        return String.format("Notes{value=%s}", value != null && value.length() > 50 ? value.substring(0, 50) + "..." : value);
    }
}
