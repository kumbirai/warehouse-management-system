package com.ccbsa.wms.location.domain.core.valueobject;

import java.util.Locale;
import java.util.Objects;
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
        if (value == null || value.trim()
                .isEmpty()) {
            throw new IllegalArgumentException("LocationBarcode cannot be null or empty");
        }

        String trimmedValue = value.trim()
                .toUpperCase(Locale.ROOT);

        // Basic CCBSA format validation: alphanumeric, 8-20 characters
        if (!CCBSA_BARCODE_PATTERN.matcher(trimmedValue)
                .matches()) {
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
        if (coordinates == null) {
            throw new IllegalArgumentException("LocationCoordinates cannot be null");
        }

        // Generate barcode from coordinates: ZONE + AISLE + RACK + LEVEL
        // Try to pad numeric values with zeros, otherwise use as-is
        String zone = coordinates.getZone();
        String aisle = padIfNumeric(coordinates.getAisle());
        String rack = padIfNumeric(coordinates.getRack());
        String level = padIfNumeric(coordinates.getLevel());

        String barcodeValue = zone + aisle + rack + level;
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

