package com.ccbsa.wms.picking.domain.core.valueobject;

import java.util.Objects;

/**
 * Value Object: PickingListReference
 * <p>
 * Represents a unique human-readable reference for a picking list. Immutable and self-validating.
 * <p>
 * Business Rules:
 * - Picking list reference must be unique per tenant
 * - Cannot be null or empty
 * - Maximum length: 50 characters
 * - Format: PICK-{YYYYMMDD}-{sequence} (e.g., PICK-20250107-001)
 */
public final class PickingListReference {
    private static final int MAX_LENGTH = 50;

    private final String value;

    private PickingListReference(String value) {
        validate(value);
        this.value = value.trim();
    }

    /**
     * Validates the picking list reference.
     *
     * @param value Picking list reference value
     * @throws IllegalArgumentException if validation fails
     */
    private void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("PickingListReference cannot be null or empty");
        }
        if (value.trim().length() > MAX_LENGTH) {
            throw new IllegalArgumentException(String.format("PickingListReference cannot exceed %d characters", MAX_LENGTH));
        }
    }

    /**
     * Factory method to create PickingListReference instance.
     *
     * @param value Picking list reference string value
     * @return PickingListReference instance
     * @throws IllegalArgumentException if value is invalid
     */
    public static PickingListReference of(String value) {
        return new PickingListReference(value);
    }

    /**
     * Returns the picking list reference value.
     *
     * @return Picking list reference string value
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
        PickingListReference that = (PickingListReference) o;
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
