package com.ccbsa.wms.picking.domain.core.entity;

import java.util.Objects;

import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.wms.picking.domain.core.valueobject.Notes;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderLineItemId;
import com.ccbsa.wms.picking.domain.core.valueobject.ProductCode;

/**
 * Entity: OrderLineItem
 * <p>
 * Represents a line item within an order. Contains product code, quantity, and optional notes.
 * <p>
 * Business Rules:
 * - Product code is required
 * - Quantity must be positive (> 0)
 * - Notes are optional
 */
public class OrderLineItem {
    private OrderLineItemId id;
    private ProductCode productCode;
    private Quantity quantity;
    private Notes notes;

    /**
     * Private constructor for builder pattern. Prevents direct instantiation.
     */
    private OrderLineItem() {
    }

    /**
     * Factory method to create builder instance.
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Business logic method: Validates the line item.
     *
     * @throws IllegalStateException if validation fails
     */
    public void validateLineItem() {
        if (productCode == null) {
            throw new IllegalStateException("Order line item must have a product code");
        }
        if (quantity == null) {
            throw new IllegalStateException("Order line item must have a quantity");
        }
        if (!quantity.isPositive()) {
            throw new IllegalStateException("Order line item quantity must be positive");
        }
    }

    // Getters

    public OrderLineItemId getId() {
        return id;
    }

    public ProductCode getProductCode() {
        return productCode;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public Notes getNotes() {
        return notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OrderLineItem that = (OrderLineItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("OrderLineItem{id=%s, productCode=%s, quantity=%s}", id, productCode, quantity);
    }

    /**
     * Builder class for constructing OrderLineItem instances.
     */
    public static class Builder {
        private OrderLineItem lineItem = new OrderLineItem();

        public Builder id(OrderLineItemId id) {
            lineItem.id = id;
            return this;
        }

        public Builder productCode(ProductCode productCode) {
            lineItem.productCode = productCode;
            return this;
        }

        public Builder quantity(Quantity quantity) {
            lineItem.quantity = quantity;
            return this;
        }

        public Builder notes(Notes notes) {
            lineItem.notes = notes;
            return this;
        }

        public Builder notes(String notes) {
            lineItem.notes = Notes.ofNullable(notes);
            return this;
        }

        public OrderLineItem build() {
            if (lineItem.id == null) {
                lineItem.id = OrderLineItemId.generate();
            }
            lineItem.validateLineItem();
            return lineItem;
        }
    }
}
