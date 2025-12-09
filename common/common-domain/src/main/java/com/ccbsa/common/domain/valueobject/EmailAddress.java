package com.ccbsa.common.domain.valueobject;

import java.util.Locale;
import java.util.Objects;

/**
 * Value Object: EmailAddress
 * <p>
 * Represents an email address.
 * Immutable and validated on construction.
 * <p>
 * This is a shared value object used across multiple services.
 * Business Rules:
 * - EmailAddress cannot be null or empty
 * - Maximum length: 255 characters
 * - Must match RFC 5322 compliant email format (simplified)
 * - Automatically converted to lowercase
 */
public final class EmailAddress {
    private final String value;

    private EmailAddress(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("EmailAddress cannot be null or empty");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 255) {
            throw new IllegalArgumentException("EmailAddress cannot exceed 255 characters");
        }
        if (!isValidEmail(trimmed)) {
            throw new IllegalArgumentException(String.format("Invalid email format: %s", trimmed));
        }
        this.value = trimmed.toLowerCase(Locale.ROOT);
    }

    private boolean isValidEmail(String email) {
        // RFC 5322 compliant email validation (simplified)
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Creates an EmailAddress from a string value.
     *
     * @param value EmailAddress string (must not be null or empty)
     * @return EmailAddress instance
     * @throws IllegalArgumentException if value is null, empty, or invalid format
     */
    public static EmailAddress of(String value) {
        return new EmailAddress(value);
    }

    /**
     * Creates an EmailAddress from a string value, allowing null.
     * Returns null if the value is null or empty.
     *
     * @param value EmailAddress string (can be null or empty)
     * @return EmailAddress instance or null if value is null/empty
     */
    public static EmailAddress ofNullable(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return new EmailAddress(value);
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
        EmailAddress emailAddress = (EmailAddress) o;
        return Objects.equals(value, emailAddress.value);
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

