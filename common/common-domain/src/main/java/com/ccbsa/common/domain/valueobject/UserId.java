package com.ccbsa.common.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a user identifier.
 * User IDs are immutable and can be UUIDs or string identifiers.
 */
public final class UserId {
    private final String value;

    private UserId(String value) {
        if (value == null || value.trim()
                .isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        this.value = value.trim();
    }

    public static UserId of(String value) {
        return new UserId(value);
    }

    public static UserId of(UUID uuid) {
        return new UserId(uuid.toString());
    }

    public String getValue() {
        return value;
    }

    public UUID getUuid() {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(String.format("User ID is not a valid UUID: %s",
                    value),
                    e);
        }
    }

    public boolean isUuid() {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserId userId = (UserId) o;
        return Objects.equals(value,
                userId.value);
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

