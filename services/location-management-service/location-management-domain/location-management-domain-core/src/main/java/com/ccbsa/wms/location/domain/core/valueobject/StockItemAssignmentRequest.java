package com.ccbsa.wms.location.domain.core.valueobject;

import java.math.BigDecimal;
import java.util.Objects;

import com.ccbsa.common.domain.valueobject.ExpirationDate;
import com.ccbsa.common.domain.valueobject.StockClassification;

/**
 * Value Object: StockItemAssignmentRequest
 * <p>
 * Represents a request to assign a location to a stock item.
 * <p>
 * This value object is used by FEFOAssignmentService to match stock items to locations.
 * <p>
 * Business Rules:
 * - StockItemId cannot be null or empty
 * - Quantity must be positive
 * - ExpirationDate can be null (for non-perishable items)
 * - StockClassification cannot be null
 */
public final class StockItemAssignmentRequest {
    private final String stockItemId; // Cross-service reference (String)
    private final BigDecimal quantity;
    private final ExpirationDate expirationDate; // May be null for non-perishable
    private final StockClassification classification;

    private StockItemAssignmentRequest(Builder builder) {
        validate(builder.stockItemId, builder.quantity, builder.classification);
        this.stockItemId = builder.stockItemId;
        this.quantity = builder.quantity;
        this.expirationDate = builder.expirationDate;
        this.classification = builder.classification;
    }

    /**
     * Validates the assignment request according to business rules.
     *
     * @param stockItemId    Stock item ID
     * @param quantity       Quantity
     * @param classification Classification
     * @throws IllegalArgumentException if validation fails
     */
    private void validate(String stockItemId, BigDecimal quantity, StockClassification classification) {
        if (stockItemId == null || stockItemId.trim().isEmpty()) {
            throw new IllegalArgumentException("StockItemId is required");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (classification == null) {
            throw new IllegalArgumentException("StockClassification is required");
        }
    }

    /**
     * Factory method to create StockItemAssignmentRequest builder.
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getStockItemId() {
        return stockItemId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public ExpirationDate getExpirationDate() {
        return expirationDate;
    }

    public StockClassification getClassification() {
        return classification;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StockItemAssignmentRequest that = (StockItemAssignmentRequest) o;
        return Objects.equals(stockItemId, that.stockItemId) && Objects.equals(quantity, that.quantity) && Objects.equals(expirationDate, that.expirationDate)
                && classification == that.classification;
    }

    @Override
    public int hashCode() {
        return Objects.hash(stockItemId, quantity, expirationDate, classification);
    }

    @Override
    public String toString() {
        return String.format("StockItemAssignmentRequest{stockItemId='%s', quantity=%s, expirationDate=%s, classification=%s}", stockItemId, quantity, expirationDate,
                classification);
    }

    /**
     * Builder for StockItemAssignmentRequest.
     */
    public static class Builder {
        private String stockItemId;
        private BigDecimal quantity;
        private ExpirationDate expirationDate;
        private StockClassification classification;

        public Builder stockItemId(String stockItemId) {
            this.stockItemId = stockItemId;
            return this;
        }

        public Builder quantity(BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder expirationDate(ExpirationDate expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public Builder classification(StockClassification classification) {
            this.classification = classification;
            return this;
        }

        public StockItemAssignmentRequest build() {
            return new StockItemAssignmentRequest(this);
        }
    }
}

