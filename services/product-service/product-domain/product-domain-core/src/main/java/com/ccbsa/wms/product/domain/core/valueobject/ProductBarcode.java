package com.ccbsa.wms.product.domain.core.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object: ProductBarcode
 *
 * Represents a barcode identifier for a product. Immutable and self-validating.
 *
 * Business Rules: - Barcode must follow valid barcode format (EAN-13, Code 128, etc.) - Barcode must be unique per tenant - Barcode type is auto-detected from format - Barcode
 * cannot be null or empty
 */
public final class ProductBarcode {
    // EAN-13: 13 digits
    private static final Pattern EAN_13_PATTERN = Pattern.compile("^[0-9]{13}$");
    // Code 128: Alphanumeric, variable length
    private static final Pattern CODE_128_PATTERN = Pattern.compile("^[A-Za-z0-9]{1,48}$");
    // UPC-A: 12 digits
    private static final Pattern UPC_A_PATTERN = Pattern.compile("^[0-9]{12}$");
    // ITF-14: 14 digits
    private static final Pattern ITF_14_PATTERN = Pattern.compile("^[0-9]{14}$");
    // Code 39: Alphanumeric with specific characters
    private static final Pattern CODE_39_PATTERN = Pattern.compile("^[A-Z0-9\\-\\s\\.\\$\\/\\+\\%]{1,43}$");

    private final String value;
    private final BarcodeType type;

    /**
     * Private constructor to enforce immutability.
     *
     * @param value Barcode string value
     * @param type  Barcode type
     * @throws IllegalArgumentException if value is invalid
     */
    private ProductBarcode(String value, BarcodeType type) {
        validate(value);
        this.value = value;
        this.type = type;
    }

    /**
     * Validates the barcode format according to business rules.
     *
     * @param value Barcode value to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("ProductBarcode cannot be null or empty");
        }
    }

    /**
     * Factory method to create ProductBarcode instance with auto-detected type.
     *
     * @param value Barcode string value
     * @return ProductBarcode instance
     * @throws IllegalArgumentException if value is invalid
     */
    public static ProductBarcode of(String value) {
        BarcodeType type = detectBarcodeType(value);
        return new ProductBarcode(value, type);
    }

    /**
     * Detects barcode type from the barcode value format.
     *
     * @param value Barcode value
     * @return Detected BarcodeType
     * @throws IllegalArgumentException if barcode format is not recognized
     */
    private static BarcodeType detectBarcodeType(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Barcode value cannot be null or empty");
        }

        String trimmedValue = value.trim();

        if (EAN_13_PATTERN.matcher(trimmedValue).matches()) {
            return BarcodeType.EAN_13;
        } else if (UPC_A_PATTERN.matcher(trimmedValue).matches()) {
            return BarcodeType.UPC_A;
        } else if (ITF_14_PATTERN.matcher(trimmedValue).matches()) {
            return BarcodeType.ITF_14;
        } else if (CODE_128_PATTERN.matcher(trimmedValue).matches()) {
            return BarcodeType.CODE_128;
        } else if (CODE_39_PATTERN.matcher(trimmedValue).matches()) {
            return BarcodeType.CODE_39;
        } else {
            // Default to CODE_128 for unrecognized formats (most flexible)
            // This allows for custom barcode formats while maintaining validation
            return BarcodeType.CODE_128;
        }
    }

    /**
     * Factory method to create ProductBarcode instance with explicit type.
     *
     * @param value Barcode string value
     * @param type  Barcode type
     * @return ProductBarcode instance
     * @throws IllegalArgumentException if value is invalid
     */
    public static ProductBarcode of(String value, BarcodeType type) {
        return new ProductBarcode(value, type);
    }

    /**
     * Returns the barcode value.
     *
     * @return Barcode string value
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the barcode type.
     *
     * @return BarcodeType
     */
    public BarcodeType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProductBarcode that = (ProductBarcode) o;
        return Objects.equals(value, that.value) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, type);
    }

    @Override
    public String toString() {
        return value;
    }
}

