package com.ccbsa.common.domain.valueobject;

import java.util.Objects;

/**
 * Value Object: Message
 * <p>
 * Represents message content for notifications, events, or other domain concepts. Immutable and self-validating.
 * <p>
 * This is a shared value object used across multiple services. Business Rules: - Message cannot be null or empty - Maximum length: 1000 characters - Automatically trimmed
 */
public final class Message {
    private final String value;

    /**
     * Private constructor to enforce immutability.
     *
     * @param value String value
     * @throws IllegalArgumentException if value is invalid
     */
    private Message(String value) {
        validate(value);
        this.value = value.trim();
    }

    /**
     * Validates the value according to business rules.
     *
     * @param value Value to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
        if (value.trim().length() > 1000) {
            throw new IllegalArgumentException("Message cannot exceed 1000 characters");
        }
    }

    /**
     * Factory method to create Message instance.
     *
     * @param value String value
     * @return Message instance
     * @throws IllegalArgumentException if value is invalid
     */
    public static Message of(String value) {
        return new Message(value);
    }

    /**
     * Returns the value.
     *
     * @return String value
     */
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
        Message message = (Message) o;
        return Objects.equals(value, message.value);
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

