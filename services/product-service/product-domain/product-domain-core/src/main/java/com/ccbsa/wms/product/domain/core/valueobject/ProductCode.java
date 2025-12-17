package com.ccbsa.wms.product.domain.core.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object: ProductCode
 *
 * Represents a unique product code identifier. Immutable and self-validating.
 *
 * Business Rules: - Product code must be alphanumeric with hyphens/underscores - Product code must be unique per tenant - Product code cannot be null or empty - Product code
 * length: 1-50 characters
 */
public final class ProductCode {
    private static final Pattern PRODUCT_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,50}$");

    private final String value;

    /**
     * Private constructor to enforce immutability.
     *
     * @param value Product code string value
     * @throws IllegalArgumentException if value is invalid
     */
    private ProductCode(String value) {
        validate(value);
        this.value = value;
    }

    /**
     * Validates the product code format according to business rules.
     *
     * @param value Product code value to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validate(String value) {
        if (value == null || value.trim()
                .isEmpty()) {
            throw new IllegalArgumentException("ProductCode cannot be null or empty");
        }

        String trimmedValue = value.trim();

        if (trimmedValue.length() > 50) {
            throw new IllegalArgumentException("ProductCode must not exceed 50 characters");
        }

        if (!PRODUCT_CODE_PATTERN.matcher(trimmedValue)
                .matches()) {
            throw new IllegalArgumentException(String.format("ProductCode must be alphanumeric with hyphens/underscores only: %s", value));
        }
    }

    /**
     * Factory method to create ProductCode instance.
     *
     * @param value Product code string value
     * @return ProductCode instance
     * @throws IllegalArgumentException if value is invalid
     */
    public static ProductCode of(String value) {
        return new ProductCode(value);
    }

    /**
     * Returns the product code value.
     *
     * @return Product code string value
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
        ProductCode that = (ProductCode) o;
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

