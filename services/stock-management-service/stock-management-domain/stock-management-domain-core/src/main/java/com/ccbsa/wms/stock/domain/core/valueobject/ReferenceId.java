package com.ccbsa.wms.stock.domain.core.valueobject;

import java.util.Objects;

/**
 * Value Object: ReferenceId
 * <p>
 * Represents a reference identifier for stock allocations (e.g., Order ID, Picking List ID).
 * Immutable and validated on construction.
 * <p>
 * Business Rules:
 * - ReferenceId cannot be null or empty
 * - Maximum length: 100 characters
 * - Automatically trimmed
 */
public final class ReferenceId {
    private static final int MAX_LENGTH = 100;
    private final String value;

    private ReferenceId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("ReferenceId cannot be null or empty");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(String.format("ReferenceId cannot exceed %d characters", MAX_LENGTH));
        }
        this.value = trimmed;
    }

    /**
     * Creates a ReferenceId from a string value.
     *
     * @param value Reference ID string (must not be null or empty)
     * @return ReferenceId instance
     * @throws IllegalArgumentException if value is null, empty, or exceeds maximum length
     */
    public static ReferenceId of(String value) {
        return new ReferenceId(value);
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
        ReferenceId that = (ReferenceId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.format("ReferenceId{value='%s'}", value);
    }
}
