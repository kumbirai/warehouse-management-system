package com.ccbsa.wms.notification.domain.core.valueobject;

import java.util.UUID;

/**
 * Value Object: NotificationId
 * <p>
 * Represents the unique identifier for Notification. Immutable and validated on construction.
 */
public final class NotificationId {
    private final UUID value;

    /**
     * Private constructor to enforce immutability.
     *
     * @param value UUID value
     * @throws IllegalArgumentException if value is null
     */
    private NotificationId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("NotificationId value cannot be null");
        }
        this.value = value;
    }

    /**
     * Factory method to create NotificationId from UUID.
     *
     * @param value UUID value
     * @return NotificationId instance
     * @throws IllegalArgumentException if value is null
     */
    public static NotificationId of(UUID value) {
        return new NotificationId(value);
    }

    /**
     * Factory method to create NotificationId from String.
     *
     * @param value UUID string representation
     * @return NotificationId instance
     * @throws IllegalArgumentException if value is invalid
     */
    public static NotificationId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("NotificationId string cannot be null or empty");
        }
        try {
            return new NotificationId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format: %s", value), e);
        }
    }

    /**
     * Generates a new NotificationId with random UUID.
     *
     * @return New NotificationId instance
     */
    public static NotificationId generate() {
        return new NotificationId(UUID.randomUUID());
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
     * Returns the string representation of the UUID.
     *
     * @return UUID string
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
        NotificationId that = (NotificationId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

