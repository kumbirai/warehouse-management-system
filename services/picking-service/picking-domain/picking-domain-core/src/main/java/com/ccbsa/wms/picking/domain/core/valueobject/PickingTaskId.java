package com.ccbsa.wms.picking.domain.core.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: PickingTaskId
 * <p>
 * Represents a unique identifier for a picking task. Immutable and self-validating.
 */
public final class PickingTaskId {
    private final UUID value;

    private PickingTaskId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("PickingTaskId cannot be null");
        }
        this.value = value;
    }

    /**
     * Factory method to create PickingTaskId from UUID.
     *
     * @param value UUID value
     * @return PickingTaskId instance
     * @throws IllegalArgumentException if value is null
     */
    public static PickingTaskId of(UUID value) {
        return new PickingTaskId(value);
    }

    /**
     * Factory method to create PickingTaskId from string.
     *
     * @param value UUID string value
     * @return PickingTaskId instance
     * @throws IllegalArgumentException if value is null or invalid UUID format
     */
    public static PickingTaskId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("PickingTaskId string cannot be null or empty");
        }
        try {
            return new PickingTaskId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format for PickingTaskId: %s", value), e);
        }
    }

    /**
     * Creates a new PickingTaskId with random UUID.
     *
     * @return New PickingTaskId instance
     */
    public static PickingTaskId generate() {
        return new PickingTaskId(UUID.randomUUID());
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
        PickingTaskId that = (PickingTaskId) o;
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
