package com.ccbsa.wms.product.domain.core.valueobject;

import java.util.Objects;

/**
 * Value Object: ProductBrand
 * <p>
 * Represents a product brand. Immutable and validated on construction.
 * <p>
 * Business Rules:
 * - Product brand is optional (can be null)
 * - If provided, must not be empty after trimming
 * - Maximum length: 100 characters
 */
public final class ProductBrand {
    private final String value;

    private ProductBrand(String value) {
        if (value != null) {
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                this.value = null;
            } else {
                if (trimmed.length() > 100) {
                    throw new IllegalArgumentException("Product brand cannot exceed 100 characters");
                }
                this.value = trimmed;
            }
        } else {
            this.value = null;
        }
    }

    /**
     * Creates a ProductBrand from a string value. Returns null if value is null or empty.
     *
     * @param value Product brand string (can be null or empty)
     * @return ProductBrand instance or null if value is null/empty
     */
    public static ProductBrand of(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return new ProductBrand(value);
    }

    /**
     * Creates a ProductBrand from a string value, allowing null.
     *
     * @param value Product brand string (can be null)
     * @return ProductBrand instance or null
     */
    public static ProductBrand ofNullable(String value) {
        return value == null ? null : new ProductBrand(value);
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
        ProductBrand that = (ProductBrand) o;
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

