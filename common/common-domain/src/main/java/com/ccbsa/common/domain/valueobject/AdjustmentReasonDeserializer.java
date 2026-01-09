package com.ccbsa.common.domain.valueobject;

import java.io.IOException;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Custom deserializer for AdjustmentReason enum.
 * <p>
 * Provides case-insensitive deserialization and better error messages for invalid values.
 * <p>
 * This deserializer handles:
 * - Standard enum values (STOCK_COUNT, DAMAGE, etc.)
 * - Case-insensitive matching
 * - Common human-readable variations (e.g., "Stock count correction" -> STOCK_COUNT)
 */
public class AdjustmentReasonDeserializer extends JsonDeserializer<AdjustmentReason> {

    @Override
    public AdjustmentReason deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null || value.trim().isEmpty()) {
            throw ctxt.weirdStringException(value, AdjustmentReason.class,
                    "AdjustmentReason cannot be null or empty. Valid values: STOCK_COUNT, DAMAGE, CORRECTION, THEFT, EXPIRATION, OTHER");
        }

        // Try exact match first (case-sensitive)
        try {
            return AdjustmentReason.valueOf(value);
        } catch (IllegalArgumentException e) {
            // Try case-insensitive match and normalization
            String upperValue = value.toUpperCase(Locale.ROOT).trim();
            String normalizedValue = normalizeValue(upperValue);

            // If normalization changed the value, try again
            if (!normalizedValue.equals(upperValue)) {
                try {
                    return AdjustmentReason.valueOf(normalizedValue);
                } catch (IllegalArgumentException ex) {
                    // Fall through to error
                }
            } else {
                // Try direct uppercase match
                try {
                    return AdjustmentReason.valueOf(upperValue);
                } catch (IllegalArgumentException ex) {
                    // Fall through to error
                }
            }

            // Use Jackson's proper exception for better error handling
            throw ctxt.weirdStringException(value, AdjustmentReason.class, String.format(
                    "Cannot deserialize value of type `%s` from String \"%s\": not one of the values accepted for Enum class: [OTHER, STOCK_COUNT, CORRECTION, EXPIRATION, "
                            + "DAMAGE, THEFT]", AdjustmentReason.class.getSimpleName(), value));
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
        if (value.contains("STOCK") && value.contains("COUNT")) {
            return "STOCK_COUNT";
        }
        if (value.contains("DAMAGE") || value.contains("DAMAGED")) {
            return "DAMAGE";
        }
        if (value.contains("CORRECTION") || value.contains("CORRECT")) {
            return "CORRECTION";
        }
        if (value.contains("THEFT") || value.contains("STOLEN")) {
            return "THEFT";
        }
        if (value.contains("EXPIR") || value.contains("EXPIRED")) {
            return "EXPIRATION";
        }
        if (value.contains("OTHER")) {
            return "OTHER";
        }

        // Return as-is if no normalization needed
        return value.replace(" ", "_");
    }
}
