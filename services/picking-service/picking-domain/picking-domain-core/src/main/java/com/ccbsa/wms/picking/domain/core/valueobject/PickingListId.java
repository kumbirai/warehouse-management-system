package com.ccbsa.wms.picking.domain.core.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: PickingListId
 * <p>
 * Represents a unique identifier for a picking list. Immutable and self-validating.
 */
public final class PickingListId {
    private final UUID value;

    private PickingListId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("PickingListId cannot be null");
        }
        this.value = value;
    }

    /**
     * Factory method to create PickingListId from UUID.
     *
     * @param value UUID value
     * @return PickingListId instance
     * @throws IllegalArgumentException if value is null
     */
    public static PickingListId of(UUID value) {
        return new PickingListId(value);
    }

    /**
     * Factory method to create PickingListId from string.
     *
     * @param value UUID string value
     * @return PickingListId instance
     * @throws IllegalArgumentException if value is null or invalid UUID format
     */
    public static PickingListId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("PickingListId string cannot be null or empty");
        }
        try {
            return new PickingListId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format for PickingListId: %s", value), e);
        }
    }

    /**
     * Creates a new PickingListId with random UUID.
     *
     * @return New PickingListId instance
     */
    public static PickingListId generate() {
        return new PickingListId(UUID.randomUUID());
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
        PickingListId that = (PickingListId) o;
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
