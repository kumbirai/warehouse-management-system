package com.ccbsa.wms.product.domain.core.valueobject;

import java.util.Objects;

/**
 * Value Object: ProductCategory
 * <p>
 * Represents a product category. Immutable and validated on construction.
 * <p>
 * Business Rules:
 * - Product category is optional (can be null)
 * - If provided, must not be empty after trimming
 * - Maximum length: 100 characters
 */
public final class ProductCategory {
    private final String value;

    private ProductCategory(String value) {
        if (value != null) {
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                this.value = null;
            } else {
                if (trimmed.length() > 100) {
                    throw new IllegalArgumentException("Product category cannot exceed 100 characters");
                }
                this.value = trimmed;
            }
        } else {
            this.value = null;
        }
    }

    /**
     * Creates a ProductCategory from a string value. Returns null if value is null or empty.
     *
     * @param value Product category string (can be null or empty)
     * @return ProductCategory instance or null if value is null/empty
     */
    public static ProductCategory of(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return new ProductCategory(value);
    }

    /**
     * Creates a ProductCategory from a string value, allowing null.
     *
     * @param value Product category string (can be null)
     * @return ProductCategory instance or null
     */
    public static ProductCategory ofNullable(String value) {
        return value == null ? null : new ProductCategory(value);
    }

    public String getValue() {
        return value;
    }

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
        ProductCategory that = (ProductCategory) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value != null ? value : "";
    }
}

