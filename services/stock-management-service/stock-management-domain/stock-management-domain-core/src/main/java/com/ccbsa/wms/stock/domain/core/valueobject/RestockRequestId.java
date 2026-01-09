package com.ccbsa.wms.stock.domain.core.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: RestockRequestId
 * <p>
 * Represents a unique identifier for a restock request. Immutable and self-validating.
 */
public final class RestockRequestId {
    private final UUID value;

    private RestockRequestId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("RestockRequestId cannot be null");
        }
        this.value = value;
    }

    /**
     * Factory method to create RestockRequestId from UUID.
     *
     * @param value UUID value
     * @return RestockRequestId instance
     * @throws IllegalArgumentException if value is null
     */
    public static RestockRequestId of(UUID value) {
        return new RestockRequestId(value);
    }

    /**
     * Factory method to create RestockRequestId from string.
     *
     * @param value UUID string value
     * @return RestockRequestId instance
     * @throws IllegalArgumentException if value is null or invalid UUID format
     */
    public static RestockRequestId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("RestockRequestId string cannot be null or empty");
        }
        try {
            return new RestockRequestId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format for RestockRequestId: %s", value), e);
        }
    }

    /**
     * Creates a new RestockRequestId with random UUID.
     *
     * @return New RestockRequestId instance
     */
    public static RestockRequestId generate() {
        return new RestockRequestId(UUID.randomUUID());
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
        RestockRequestId that = (RestockRequestId) o;
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
