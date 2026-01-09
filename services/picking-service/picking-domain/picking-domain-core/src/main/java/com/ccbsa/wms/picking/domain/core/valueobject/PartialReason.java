package com.ccbsa.wms.picking.domain.core.valueobject;

import java.util.Objects;

/**
 * Value Object: PartialReason
 * <p>
 * Represents the reason for partial picking of a picking task.
 * Immutable and validated on construction.
 * <p>
 * Business Rules:
 * - Partial reason cannot be null or empty
 * - Maximum length: 500 characters
 * - Automatically trimmed
 */
public final class PartialReason {
    private static final int MAX_LENGTH = 500;
    private final String value;

    private PartialReason(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Partial reason cannot be null or empty");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(String.format("Partial reason cannot exceed %d characters", MAX_LENGTH));
        }
        this.value = trimmed;
    }

    /**
     * Creates PartialReason from a string value.
     *
     * @param value Partial reason string (must not be null or empty)
     * @return PartialReason instance
     * @throws IllegalArgumentException if value is null, empty, or exceeds maximum length
     */
    public static PartialReason of(String value) {
        return new PartialReason(value);
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
        PartialReason that = (PartialReason) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.format("PartialReason{value='%s'}", value);
    }
}
