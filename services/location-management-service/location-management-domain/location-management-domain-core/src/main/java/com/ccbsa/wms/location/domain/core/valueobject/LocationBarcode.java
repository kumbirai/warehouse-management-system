package com.ccbsa.wms.location.domain.core.valueobject;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Value Object: LocationBarcode
 *
 * Represents a barcode identifier for a warehouse location. Immutable and self-validating according to CCBSA standards.
 *
 * Business Rules: - Barcode must follow CCBSA format standards - Barcode must be unique per tenant - Barcode can be auto-generated from coordinates or manually provided
 */
public final class LocationBarcode {
    private static final Pattern CCBSA_BARCODE_PATTERN = Pattern.compile("^[A-Z0-9]{8,20}$");

    private final String value;

    /**
     * Private constructor to enforce immutability.
     *
     * @param value Barcode string value
     * @throws IllegalArgumentException if value is invalid
     */
    private LocationBarcode(String value) {
        validateFormat(value);
        this.value = value;
    }

    /**
     * Validates the barcode format according to CCBSA standards.
     *
     * @param value Barcode value to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateFormat(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("LocationBarcode cannot be null or empty");
        }

        String trimmedValue = value.trim().toUpperCase(Locale.ROOT);

        // Basic CCBSA format validation: alphanumeric, 8-20 characters
        if (!CCBSA_BARCODE_PATTERN.matcher(trimmedValue).matches()) {
            throw new IllegalArgumentException(String.format("LocationBarcode must be 8-20 alphanumeric characters: %s", value));
        }
    }

    /**
     * Factory method to create LocationBarcode instance.
     *
     * @param value Barcode string value
     * @return LocationBarcode instance
     * @throws IllegalArgumentException if value is invalid
     */
    public static LocationBarcode of(String value) {
        return new LocationBarcode(value);
    }

    /**
     * Generates a barcode from location coordinates. Format: {ZONE}{AISLE}{RACK}{LEVEL} (e.g., "A010101")
     *
     * @param coordinates Location coordinates
     * @return Generated LocationBarcode instance
     * @throws IllegalArgumentException if coordinates is null
     */
    public static LocationBarcode generate(LocationCoordinates coordinates) {
        return generate(coordinates, null);
    }

    /**
     * Generates a barcode from location coordinates with optional unique identifier.
     * Format: {ZONE}{AISLE}{RACK}{LEVEL}{UNIQUE_SUFFIX} (e.g., "A010101AB")
     * The unique suffix ensures barcode uniqueness when coordinates might collide.
     * Ensures the final barcode is within 8-20 characters as per CCBSA standards.
     *
     * @param coordinates Location coordinates
     * @param uniqueId    Optional unique identifier (e.g., location ID) to ensure uniqueness
     * @return Generated LocationBarcode instance
     * @throws IllegalArgumentException if coordinates is null or cannot generate valid barcode
     */
    public static LocationBarcode generate(LocationCoordinates coordinates, UUID uniqueId) {
        if (coordinates == null) {
            throw new IllegalArgumentException("LocationCoordinates cannot be null");
        }

        // Calculate suffix length (2 characters if uniqueId provided, 0 otherwise)
        int suffixLength = (uniqueId != null) ? 2 : 0;
        // Maximum length for coordinate parts (20 max - suffix length)
        int maxCoordinateLength = 20 - suffixLength;

        // Generate barcode from coordinates: ZONE + AISLE + RACK + LEVEL
        // Try to pad numeric values with zeros, otherwise use as-is
        String zone = coordinates.getZone();
        String aisle = padIfNumeric(coordinates.getAisle());
        String rack = padIfNumeric(coordinates.getRack());
        String level = padIfNumeric(coordinates.getLevel());

        // Concatenate coordinate parts
        String barcodeValue = String.format("%s%s%s%s", zone, aisle, rack, level);

        // Truncate if exceeds maximum length, prioritizing zone and aisle
        if (barcodeValue.length() > maxCoordinateLength) {
            barcodeValue = truncateToFit(barcodeValue, zone, aisle, rack, level, maxCoordinateLength);
        }

        // If uniqueId is provided, append a short hash to ensure uniqueness
        // This prevents collisions when sanitized codes are the same (e.g., "WH-18" and "WH18" both become "WH18")
        if (uniqueId != null) {
            // Generate a 2-character alphanumeric suffix from UUID hash
            int hash = uniqueId.hashCode();
            // Use absolute value and modulo to get a positive number, then convert to base-36 (0-9, A-Z)
            String suffix = Integer.toString(Math.abs(hash) % 1296, 36).toUpperCase();
            // Pad to 2 characters if needed
            if (suffix.length() < 2) {
                suffix = String.format("0%s", suffix);
            } else if (suffix.length() > 2) {
                suffix = suffix.substring(0, 2);
            }
            barcodeValue = String.format("%s%s", barcodeValue, suffix);
        }

        // Ensure minimum length (pad with zeros if needed, but this should rarely happen)
        if (barcodeValue.length() < 8) {
            // Pad with zeros to reach minimum length
            int paddingNeeded = 8 - barcodeValue.length();
            barcodeValue = String.format("%s%s", barcodeValue, "0".repeat(paddingNeeded));
        }

        return new LocationBarcode(barcodeValue);
    }

    /**
     * Pads a string with zeros if it's numeric, otherwise returns as-is.
     *
     * @param value String value to pad
     * @return Padded string if numeric, original string otherwise
     */
    private static String padIfNumeric(String value) {
        try {
            int numValue = Integer.parseInt(value);
            return String.format("%02d", numValue);
        } catch (NumberFormatException e) {
            // Not numeric, return as-is
            return value;
        }
    }

    /**
     * Truncates coordinate parts to fit within maximum length while preserving as much information as possible.
     * Prioritizes zone and aisle over rack and level.
     *
     * @param currentValue Current concatenated value
     * @param zone         Zone identifier
     * @param aisle        Aisle identifier
     * @param rack         Rack identifier
     * @param level        Level identifier
     * @param maxLength    Maximum allowed length
     * @return Truncated barcode value
     */
    private static String truncateToFit(String currentValue, String zone, String aisle, String rack, String level, int maxLength) {
        // Strategy: Preserve zone and aisle as much as possible, truncate rack and level if needed
        int zoneLen = zone.length();
        int aisleLen = aisle.length();
        int rackLen = rack.length();
        int levelLen = level.length();

        // Calculate available space after zone and aisle
        int remainingSpace = maxLength - zoneLen - aisleLen;

        // If zone + aisle already exceed maxLength, truncate them
        if (remainingSpace < 0) {
            // Truncate zone and aisle proportionally to fit maxLength
            int totalZoneAisleLen = zoneLen + aisleLen;
            int zoneSpace = Math.max((zoneLen * maxLength) / totalZoneAisleLen, 1);
            int aisleSpace = maxLength - zoneSpace;
            String truncatedZone = zone.length() > zoneSpace ? zone.substring(0, zoneSpace) : zone;
            String truncatedAisle = aisle.length() > aisleSpace ? aisle.substring(0, aisleSpace) : aisle;
            return String.format("%s%s", truncatedZone, truncatedAisle);
        }

        // If we can fit everything, return truncated to maxLength
        if (remainingSpace >= rackLen + levelLen) {
            return currentValue.substring(0, Math.min(currentValue.length(), maxLength));
        }

        // Prioritize rack over level
        if (remainingSpace >= rackLen) {
            // Fit zone + aisle + rack, truncate level
            int levelSpace = Math.max(remainingSpace - rackLen, 0);
            String truncatedLevel = level.length() > levelSpace ? level.substring(0, levelSpace) : level;
            return String.format("%s%s%s%s", zone, aisle, rack, truncatedLevel);
        } else if (remainingSpace > 0) {
            // Only fit zone + aisle, truncate rack and level proportionally
            int rackSpace = Math.max(remainingSpace / 2, 1);
            int levelSpace = Math.max(remainingSpace - rackSpace, 0);
            String truncatedRack = rack.length() > rackSpace ? rack.substring(0, rackSpace) : rack;
            String truncatedLevel = level.length() > levelSpace ? level.substring(0, levelSpace) : level;
            return String.format("%s%s%s%s", zone, aisle, truncatedRack, truncatedLevel);
        } else {
            // No space for rack and level, return just zone + aisle
            return String.format("%s%s", zone, aisle);
        }
    }

    /**
     * Returns the barcode value.
     *
     * @return Barcode string value
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
        LocationBarcode that = (LocationBarcode) o;
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

