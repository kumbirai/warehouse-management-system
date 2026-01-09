package com.ccbsa.common.domain.valueobject;

import java.util.Locale;

/**
 * Enum: Priority
 * <p>
 * Represents the priority level for picking operations. Used across services (DRY principle).
 * <p>
 * Priority levels:
 * - HIGH: Urgent orders requiring immediate attention
 * - NORMAL: Standard priority orders
 * - LOW: Low priority orders that can be processed later
 */
public enum Priority {
    /**
     * High priority - Urgent orders requiring immediate attention
     */
    HIGH,

    /**
     * Normal priority - Standard priority orders
     */
    NORMAL,

    /**
     * Low priority - Low priority orders that can be processed later
     */
    LOW;

    /**
     * Parses a string value to Priority enum (case-insensitive).
     *
     * @param value String value to parse
     * @return Priority enum value
     * @throws IllegalArgumentException if value is null or does not match any enum value
     */
    public static Priority fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Priority value cannot be null or empty");
        }
        try {
            return Priority.valueOf(value.toUpperCase(Locale.ROOT).trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid priority value: %s. Must be one of: HIGH, NORMAL, LOW", value), e);
        }
    }
}
