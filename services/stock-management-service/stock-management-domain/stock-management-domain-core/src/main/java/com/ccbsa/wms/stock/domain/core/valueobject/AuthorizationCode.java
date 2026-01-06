package com.ccbsa.wms.stock.domain.core.valueobject;

import java.util.Objects;

/**
 * Value Object: AuthorizationCode
 * <p>
 * Represents an authorization code for large stock adjustments or other operations requiring approval.
 * Immutable and validated on construction.
 * <p>
 * Business Rules:
 * - Authorization code can be null (optional field)
 * - If provided, cannot be empty after trimming
 * - Maximum length: 50 characters
 * - Automatically trimmed
 */
public final class AuthorizationCode {
    private static final int MAX_LENGTH = 50;
    private final String value;

    private AuthorizationCode(String value) {
        if (value != null) {
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                this.value = null;
            } else {
                if (trimmed.length() > MAX_LENGTH) {
                    throw new IllegalArgumentException(String.format("AuthorizationCode cannot exceed %d characters", MAX_LENGTH));
                }
                this.value = trimmed;
            }
        } else {
            this.value = null;
        }
    }

    /**
     * Creates AuthorizationCode from a string value. Returns null if value is null or empty.
     *
     * @param value Authorization code string (can be null or empty)
     * @return AuthorizationCode instance or null if value is null/empty
     * @throws IllegalArgumentException if value exceeds maximum length
     */
    public static AuthorizationCode of(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return new AuthorizationCode(value);
    }

    /**
     * Creates AuthorizationCode from a string value, allowing null.
     *
     * @param value Authorization code string (can be null)
     * @return AuthorizationCode instance or null
     * @throws IllegalArgumentException if value exceeds maximum length
     */
    public static AuthorizationCode ofNullable(String value) {
        return value == null ? null : new AuthorizationCode(value);
    }

    /**
     * Returns the string value.
     *
     * @return String value (can be null)
     */
    public String getValue() {
        return value;
    }

    /**
     * Checks if authorization code is empty or null.
     *
     * @return true if authorization code is null or empty
     */
    public boolean isEmpty() {
        return value == null || value.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthorizationCode that = (AuthorizationCode) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value == null ? "AuthorizationCode{null}" : String.format("AuthorizationCode{value='%s'}", value);
    }
}
