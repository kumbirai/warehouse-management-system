package com.ccbsa.common.domain.valueobject;

import java.io.IOException;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Custom deserializer for ReturnReason enum.
 * <p>
 * Provides case-insensitive deserialization and better error messages for invalid values.
 * <p>
 * This deserializer handles:
 * - Standard enum values (DEFECTIVE, WRONG_ITEM, etc.)
 * - Case-insensitive matching
 * - Common human-readable variations
 */
public class ReturnReasonDeserializer extends JsonDeserializer<ReturnReason> {

    @Override
    public ReturnReason deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null || value.trim().isEmpty()) {
            throw ctxt.weirdStringException(value, ReturnReason.class,
                    "ReturnReason cannot be null or empty. Valid values: DEFECTIVE, WRONG_ITEM, DAMAGED, EXPIRED, CUSTOMER_REQUEST, OTHER");
        }

        // Try exact match first (case-sensitive)
        try {
            return ReturnReason.valueOf(value);
        } catch (IllegalArgumentException e) {
            // Try case-insensitive match and normalization
            String upperValue = value.toUpperCase(Locale.ROOT).trim();
            String normalizedValue = normalizeValue(upperValue);

            // If normalization changed the value, try again
            if (!normalizedValue.equals(upperValue)) {
                try {
                    return ReturnReason.valueOf(normalizedValue);
                } catch (IllegalArgumentException ex) {
                    // Fall through to error
                }
            } else {
                // Try direct uppercase match
                try {
                    return ReturnReason.valueOf(upperValue);
                } catch (IllegalArgumentException ex) {
                    // Fall through to error
                }
            }

            // Use Jackson's proper exception for better error handling
            throw ctxt.weirdStringException(value, ReturnReason.class, String.format(
                    "Cannot deserialize value of type `%s` from String \"%s\": not one of the values accepted for Enum class: [DEFECTIVE, WRONG_ITEM, DAMAGED, EXPIRED, "
                            + "CUSTOMER_REQUEST, OTHER]",
                    ReturnReason.class.getSimpleName(), value));
        }
    }

    /**
     * Normalizes common human-readable variations to enum values.
     *
     * @param value The input value (already uppercased)
     * @return Normalized enum value name
     */
    private String normalizeValue(String value) {
        // Handle common variations
        if (value.contains("DEFECT") || value.contains("FAULTY") || value.contains("BROKEN")) {
            return "DEFECTIVE";
        }
        if (value.contains("WRONG") || value.contains("INCORRECT")) {
            return "WRONG_ITEM";
        }
        if (value.contains("DAMAGE") || value.contains("DAMAGED")) {
            return "DAMAGED";
        }
        if (value.contains("EXPIR") || value.contains("EXPIRED")) {
            return "EXPIRED";
        }
        if (value.contains("CUSTOMER") || value.contains("REQUEST")) {
            return "CUSTOMER_REQUEST";
        }
        if (value.contains("OTHER")) {
            return "OTHER";
        }

        // Return as-is if no normalization needed
        return value.replace(" ", "_");
    }
}
