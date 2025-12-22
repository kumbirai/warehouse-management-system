package com.ccbsa.wms.stock.domain.core.valueobject;

import java.util.Objects;

/**
 * Value Object: ReceivedBy
 * <p>
 * Represents the identifier of the user who received a stock consignment. Immutable and validated on construction.
 * <p>
 * Business Rules:
 * - ReceivedBy is optional (can be null)
 * - If provided, must not be empty after trimming
 * - Maximum length: 255 characters
 */
public final class ReceivedBy {
    private final String value;

    private ReceivedBy(String value) {
        if (value != null) {
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                this.value = null;
            } else {
                if (trimmed.length() > 255) {
                    throw new IllegalArgumentException("ReceivedBy cannot exceed 255 characters");
                }
                this.value = trimmed;
            }
        } else {
            this.value = null;
        }
    }

    /**
     * Creates a ReceivedBy from a string value. Returns null if value is null or empty.
     *
     * @param value ReceivedBy string (can be null or empty)
     * @return ReceivedBy instance or null if value is null/empty
     */
    public static ReceivedBy of(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return new ReceivedBy(value);
    }

    /**
     * Creates a ReceivedBy from a string value, allowing null.
     *
     * @param value ReceivedBy string (can be null)
     * @return ReceivedBy instance or null
     */
    public static ReceivedBy ofNullable(String value) {
        return value == null ? null : new ReceivedBy(value);
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
     * Checks if the value is empty or null.
     *
     * @return true if value is null or empty
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
        ReceivedBy receivedBy = (ReceivedBy) o;
        return Objects.equals(value, receivedBy.value);
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

