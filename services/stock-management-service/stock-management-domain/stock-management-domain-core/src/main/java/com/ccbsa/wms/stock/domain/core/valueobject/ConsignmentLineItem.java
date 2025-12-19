package com.ccbsa.wms.stock.domain.core.valueobject;

import java.time.LocalDate;
import java.util.Objects;

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
    private final int quantity;
    private final LocalDate expirationDate;

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
    private void validate(ProductCode productCode, int quantity, LocalDate expirationDate) {
        if (productCode == null) {
            throw new IllegalArgumentException("ProductCode is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (expirationDate != null && expirationDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("ExpirationDate cannot be in the past");
        }
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
    public int getQuantity() {
        return quantity;
    }

    /**
     * Returns the expiration date.
     *
     * @return ExpirationDate (optional, may be null)
     */
    public LocalDate getExpirationDate() {
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
        return quantity == that.quantity && Objects.equals(productCode, that.productCode) && Objects.equals(expirationDate, that.expirationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productCode, quantity, expirationDate);
    }

    @Override
    public String toString() {
        return String.format("ConsignmentLineItem{productCode=%s, quantity=%d, expirationDate=%s}", productCode, quantity, expirationDate);
    }

    /**
     * Builder for ConsignmentLineItem.
     */
    public static class Builder {
        private ProductCode productCode;
        private int quantity;
        private LocalDate expirationDate;

        public Builder productCode(ProductCode productCode) {
            this.productCode = productCode;
            return this;
        }

        public Builder productCode(String productCode) {
            this.productCode = ProductCode.of(productCode);
            return this;
        }

        public Builder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder expirationDate(LocalDate expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public ConsignmentLineItem build() {
            return new ConsignmentLineItem(this);
        }
    }
}

