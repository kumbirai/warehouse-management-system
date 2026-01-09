package com.ccbsa.common.domain.valueobject;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: LoadNumber
 * <p>
 * Represents a unique identifier for a load. Immutable and self-validating.
 * <p>
 * This value object is shared across services (DRY principle).
 * <p>
 * Business Rules:
 * - Load number cannot be null or empty
 * - Load number cannot exceed 50 characters
 * - Load number must be alphanumeric with dashes only
 */
public final class LoadNumber {
    private final String value;

    private LoadNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Load number cannot be null or empty");
        }
        if (value.length() > 50) {
            throw new IllegalArgumentException("Load number cannot exceed 50 characters");
        }
        if (!value.matches("^[A-Za-z0-9-]+$")) {
            throw new IllegalArgumentException("Load number must be alphanumeric with dashes only");
        }
        this.value = value;
    }

    /**
     * Factory method to create LoadNumber from string value.
     *
     * @param value String value (must be alphanumeric with dashes, max 50 chars)
     * @return LoadNumber instance
     * @throws IllegalArgumentException if value is null, empty, or invalid format
     */
    public static LoadNumber of(String value) {
        return new LoadNumber(value);
    }

    /**
     * Generates a new LoadNumber with format "LOAD-{UUID8}".
     *
     * @return New LoadNumber instance
     */
    public static LoadNumber generate() {
        return new LoadNumber("LOAD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
    }

    /**
     * Returns the string value.
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
        LoadNumber that = (LoadNumber) o;
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
