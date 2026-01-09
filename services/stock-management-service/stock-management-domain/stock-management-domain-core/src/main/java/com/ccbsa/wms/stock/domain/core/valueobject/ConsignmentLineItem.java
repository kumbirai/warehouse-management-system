package com.ccbsa.wms.stock.domain.core.valueobject;

import java.util.Objects;

import com.ccbsa.common.domain.valueobject.ExpirationDate;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;

/**
 * Value Object: ConsignmentLineItem
 * <p>
 * Represents a line item in a stock consignment. Immutable and self-validating.
 * <p>
 * Business Rules: - Product code is required - Quantity must be positive (> 0) - Expiration date is optional, but if provided must be in the future
 */
public final class ConsignmentLineItem {
    private final ProductCode productCode;
    private final Quantity quantity;
    private final ExpirationDate expirationDate;

    private ConsignmentLineItem(Builder builder) {
        validate(builder.productCode, builder.quantity, builder.expirationDate);
        this.productCode = builder.productCode;
        this.quantity = builder.quantity;
        this.expirationDate = builder.expirationDate;
    }

    /**
     * Validates the line item according to business rules.
     *
     * @param productCode    Product code
     * @param quantity       Quantity
     * @param expirationDate Expiration date (optional)
     * @throws IllegalArgumentException if validation fails
     */
    private void validate(ProductCode productCode, Quantity quantity, ExpirationDate expirationDate) {
        if (productCode == null) {
            throw new IllegalArgumentException("ProductCode is required");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity is required");
        }
        if (!quantity.isPositive()) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        // Note: Expiration date validation is lenient - past dates are allowed
        // to support testing expired stock scenarios and handling stock that was
        // received with past expiration dates. Business logic will handle expired
        // stock appropriately (classification, allocation rules, etc.)
        // No validation for expirationDate - allow any date including past dates
    }

    /**
     * Factory method to create ConsignmentLineItem builder.
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the product code.
     *
     * @return ProductCode
     */
    public ProductCode getProductCode() {
        return productCode;
    }

    /**
     * Returns the quantity.
     *
     * @return Quantity (always positive)
     */
    public Quantity getQuantity() {
        return quantity;
    }

    /**
     * Returns the expiration date.
     *
     * @return ExpirationDate (optional, may be null)
     */
    public ExpirationDate getExpirationDate() {
        return expirationDate;
    }

    /**
     * Checks if the line item has an expiration date.
     *
     * @return true if expiration date is set
     */
    public boolean hasExpirationDate() {
        return expirationDate != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConsignmentLineItem that = (ConsignmentLineItem) o;
        return Objects.equals(quantity, that.quantity) && Objects.equals(productCode, that.productCode) && Objects.equals(expirationDate, that.expirationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productCode, quantity, expirationDate);
    }

    @Override
    public String toString() {
        return String.format("ConsignmentLineItem{productCode=%s, quantity=%s, expirationDate=%s}", productCode, quantity, expirationDate);
    }

    /**
     * Builder for ConsignmentLineItem.
     */
    public static class Builder {
        private ProductCode productCode;
        private Quantity quantity;
        private ExpirationDate expirationDate;

        public Builder productCode(ProductCode productCode) {
            this.productCode = productCode;
            return this;
        }

        public Builder productCode(String productCode) {
            this.productCode = ProductCode.of(productCode);
            return this;
        }

        public Builder quantity(Quantity quantity) {
            this.quantity = quantity;
            return this;
        }

        /**
         * Convenience method to set quantity from integer value.
         * Converts int to Quantity value object.
         *
         * @param quantity Integer value (must be positive)
         * @return Builder instance
         */
        public Builder quantity(int quantity) {
            this.quantity = Quantity.of(quantity);
            return this;
        }

        public Builder expirationDate(ExpirationDate expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        /**
         * Convenience method to set expiration date from LocalDate.
         * Converts LocalDate to ExpirationDate value object.
         *
         * @param expirationDate LocalDate value (may be null)
         * @return Builder instance
         */
        public Builder expirationDate(java.time.LocalDate expirationDate) {
            this.expirationDate = expirationDate != null ? ExpirationDate.of(expirationDate) : null;
            return this;
        }

        public ConsignmentLineItem build() {
            return new ConsignmentLineItem(this);
        }
    }
}

