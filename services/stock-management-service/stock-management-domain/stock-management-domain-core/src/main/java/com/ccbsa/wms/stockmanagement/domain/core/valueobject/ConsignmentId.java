package com.ccbsa.wms.stockmanagement.domain.core.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: ConsignmentId
 * <p>
 * Represents a unique identifier for a stock consignment. Immutable and self-validating.
 */
public final class ConsignmentId {
    private final UUID value;

    private ConsignmentId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("ConsignmentId cannot be null");
        }
        this.value = value;
    }

    /**
     * Factory method to create ConsignmentId from UUID.
     *
     * @param value UUID value
     * @return ConsignmentId instance
     * @throws IllegalArgumentException if value is null
     */
    public static ConsignmentId of(UUID value) {
        return new ConsignmentId(value);
    }

    /**
     * Factory method to create ConsignmentId from string.
     *
     * @param value UUID string value
     * @return ConsignmentId instance
     * @throws IllegalArgumentException if value is null or invalid UUID format
     */
    public static ConsignmentId of(String value) {
        if (value == null || value.trim()
                .isEmpty()) {
            throw new IllegalArgumentException("ConsignmentId string cannot be null or empty");
        }
        try {
            return new ConsignmentId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format for ConsignmentId: %s", value), e);
        }
    }

    /**
     * Creates a new ConsignmentId with random UUID.
     *
     * @return New ConsignmentId instance
     */
    public static ConsignmentId generate() {
        return new ConsignmentId(UUID.randomUUID());
    }

    /**
     * Returns the UUID value.
     *
     * @return UUID value
     */
    public UUID getValue() {
        return value;
    }

    /**
     * Returns the UUID value as string.
     *
     * @return UUID string value
     */
    public String getValueAsString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConsignmentId that = (ConsignmentId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

