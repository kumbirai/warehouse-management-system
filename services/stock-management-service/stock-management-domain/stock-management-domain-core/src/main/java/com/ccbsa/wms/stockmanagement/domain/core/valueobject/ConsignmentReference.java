package com.ccbsa.wms.stockmanagement.domain.core.valueobject;

import java.util.Objects;

/**
 * Value Object: ConsignmentReference
 * <p>
 * Represents a unique consignment reference from D365 or external system.
 * Immutable and self-validating.
 * <p>
 * Business Rules:
 * - Consignment reference must be unique per tenant
 * - Cannot be null or empty
 * - Maximum length: 100 characters
 */
public final class ConsignmentReference {
    private static final int MAX_LENGTH = 100;

    private final String value;

    private ConsignmentReference(String value) {
        validate(value);
        this.value = value.trim();
    }

    /**
     * Validates the consignment reference.
     *
     * @param value Consignment reference value
     * @throws IllegalArgumentException if validation fails
     */
    private void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("ConsignmentReference cannot be null or empty");
        }
        if (value.trim().length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("ConsignmentReference cannot exceed %d characters", MAX_LENGTH)
            );
        }
    }

    /**
     * Factory method to create ConsignmentReference instance.
     *
     * @param value Consignment reference string value
     * @return ConsignmentReference instance
     * @throws IllegalArgumentException if value is invalid
     */
    public static ConsignmentReference of(String value) {
        return new ConsignmentReference(value);
    }

    /**
     * Returns the consignment reference value.
     *
     * @return Consignment reference string value
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
        ConsignmentReference that = (ConsignmentReference) o;
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

