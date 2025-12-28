package com.ccbsa.wms.location.domain.core.valueobject;

import java.util.Objects;

/**
 * Value Object: CancellationReason
 * <p>
 * Represents a reason for cancelling a stock movement. Immutable and validated on construction.
 * <p>
 * Business Rules:
 * - CancellationReason cannot be null or empty
 * - Maximum length: 500 characters
 * - Automatically trimmed
 */
public final class CancellationReason {
    private static final int MAX_LENGTH = 500;
    private final String value;

    private CancellationReason(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("CancellationReason cannot be null or empty");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(String.format("CancellationReason cannot exceed %d characters", MAX_LENGTH));
        }
        this.value = trimmed;
    }

    /**
     * Creates a CancellationReason from a string value.
     *
     * @param value Cancellation reason string (must not be null or empty)
     * @return CancellationReason instance
     * @throws IllegalArgumentException if value is null, empty, or exceeds maximum length
     */
    public static CancellationReason of(String value) {
        return new CancellationReason(value);
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
        CancellationReason that = (CancellationReason) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.format("CancellationReason{value='%s'}", value);
    }
}
