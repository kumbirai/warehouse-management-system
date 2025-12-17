package com.ccbsa.wms.user.domain.core.valueobject;

import java.util.Objects;

/**
 * Value Object: KeycloakUserId
 * <p>
 * Represents a Keycloak user identifier for IAM integration. Immutable and validated on construction.
 */
public final class KeycloakUserId {
    private final String value;

    private KeycloakUserId(String value) {
        if (value == null || value.trim()
                .isEmpty()) {
            throw new IllegalArgumentException("Keycloak user ID cannot be null or empty");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException("Keycloak user ID cannot exceed 100 characters");
        }
        this.value = trimmed;
    }

    public static KeycloakUserId of(String value) {
        return new KeycloakUserId(value);
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
        KeycloakUserId that = (KeycloakUserId) o;
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

