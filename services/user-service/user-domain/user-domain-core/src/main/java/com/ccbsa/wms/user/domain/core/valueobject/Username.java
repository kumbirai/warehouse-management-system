package com.ccbsa.wms.user.domain.core.valueobject;

import java.util.Objects;

/**
 * Value Object: Username
 * <p>
 * Represents a username for user authentication. Immutable and validated on construction.
 */
public final class Username {
    private final String value;

    private Username(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException("Username cannot exceed 100 characters");
        }
        if (trimmed.length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters");
        }
        // Username validation: alphanumeric, underscore, hyphen, dot
        if (!trimmed.matches("^[a-zA-Z0-9._-]+$")) {
            throw new IllegalArgumentException("Username can only contain alphanumeric characters, underscore, hyphen, and dot");
        }
        this.value = trimmed;
    }

    public static Username of(String value) {
        return new Username(value);
    }

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
        Username username = (Username) o;
        return Objects.equals(value, username.value);
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

