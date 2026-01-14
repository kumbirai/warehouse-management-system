package com.ccbsa.wms.returns.domain.core.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import com.ccbsa.common.domain.valueobject.Notes;
import com.ccbsa.common.domain.valueobject.ProductCondition;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.ReturnReason;
import com.ccbsa.common.domain.valueobject.ReturnLineItemId;

/**
 * Entity: ReturnLineItem
 * <p>
 * Represents a line item within a return. Immutable entity within Return aggregate.
 * <p>
 * Business Rules:
 * - Product ID must not be null
 * - Ordered quantity must be > 0
 * - Picked quantity must be > 0
 * - Accepted quantity must be >= 0 and <= picked quantity
 * - Returned quantity = picked quantity - accepted quantity
 * - If returned quantity > 0, return reason is required
 * - Product condition is required for full returns
 */
public class ReturnLineItem {
    private final ReturnLineItemId id;
    private final ProductId productId;
    private final Quantity orderedQuantity;
    private final Quantity pickedQuantity;
    private final Quantity acceptedQuantity;
    private final Quantity returnedQuantity;
    private final ProductCondition productCondition;
    private final ReturnReason returnReason;
    private final Notes lineNotes;
    private final LocalDateTime createdAt;

    private ReturnLineItem(ReturnLineItemId id, ProductId productId, Quantity orderedQuantity, Quantity pickedQuantity, Quantity acceptedQuantity, Quantity returnedQuantity,
                           ProductCondition productCondition, ReturnReason returnReason, Notes lineNotes, LocalDateTime createdAt) {
        if (id == null) {
            throw new IllegalArgumentException("ReturnLineItemId cannot be null");
        }
        if (productId == null) {
            throw new IllegalArgumentException("ProductId cannot be null");
        }
        if (orderedQuantity == null || !orderedQuantity.isPositive()) {
            throw new IllegalArgumentException("Ordered quantity must be positive");
        }
        if (pickedQuantity == null || !pickedQuantity.isPositive()) {
            throw new IllegalArgumentException("Picked quantity must be positive");
        }
        if (acceptedQuantity == null || acceptedQuantity.getValue() < 0) {
            throw new IllegalArgumentException("Accepted quantity cannot be negative");
        }
        if (acceptedQuantity.isGreaterThan(pickedQuantity)) {
            throw new IllegalArgumentException("Accepted quantity cannot exceed picked quantity");
        }
        if (returnedQuantity == null || returnedQuantity.getValue() < 0) {
            throw new IllegalArgumentException("Returned quantity cannot be negative");
        }
        // Validate returned quantity calculation
        int calculatedReturned = pickedQuantity.getValue() - acceptedQuantity.getValue();
        if (returnedQuantity.getValue() != calculatedReturned) {
            throw new IllegalArgumentException("Returned quantity must equal picked quantity minus accepted quantity");
        }
        // If returned quantity > 0, return reason is required
        if (returnedQuantity.isPositive() && returnReason == null) {
            throw new IllegalArgumentException("Return reason is required when returned quantity is greater than zero");
        }
        // Notes validation is handled by Notes value object

        this.id = id;
        this.productId = productId;
        this.orderedQuantity = orderedQuantity;
        this.pickedQuantity = pickedQuantity;
        this.acceptedQuantity = acceptedQuantity;
        this.returnedQuantity = returnedQuantity;
        this.productCondition = productCondition;
        this.returnReason = returnReason;
        this.lineNotes = lineNotes;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    /**
     * Factory method to create ReturnLineItem for partial return.
     *
     * @param id               Return line item ID
     * @param productId        Product ID
     * @param orderedQuantity  Ordered quantity
     * @param pickedQuantity   Picked quantity
     * @param acceptedQuantity Accepted quantity
     * @param returnReason     Return reason (required if returned quantity > 0)
     * @param lineNotes        Optional line notes
     * @return ReturnLineItem instance
     */
    public static ReturnLineItem createPartial(ReturnLineItemId id, ProductId productId, Quantity orderedQuantity, Quantity pickedQuantity, Quantity acceptedQuantity,
                                               ReturnReason returnReason, Notes lineNotes) {
        Quantity returnedQuantity = Quantity.of(pickedQuantity.getValue() - acceptedQuantity.getValue());
        Notes notes = lineNotes != null ? lineNotes : Notes.forLineItem(null);
        return new ReturnLineItem(id, productId, orderedQuantity, pickedQuantity, acceptedQuantity, returnedQuantity, null, returnReason, notes, null);
    }

    /**
     * Factory method to create ReturnLineItem for full return.
     *
     * @param id               Return line item ID
     * @param productId        Product ID
     * @param orderedQuantity  Ordered quantity
     * @param pickedQuantity   Picked quantity
     * @param productCondition Product condition
     * @param returnReason     Return reason
     * @param lineNotes        Optional line notes
     * @return ReturnLineItem instance
     */
    public static ReturnLineItem createFull(ReturnLineItemId id, ProductId productId, Quantity orderedQuantity, Quantity pickedQuantity, ProductCondition productCondition,
                                            ReturnReason returnReason, Notes lineNotes) {
        Quantity acceptedQuantity = Quantity.of(0);
        Quantity returnedQuantity = pickedQuantity;
        Notes notes = lineNotes != null ? lineNotes : Notes.forLineItem(null);
        return new ReturnLineItem(id, productId, orderedQuantity, pickedQuantity, acceptedQuantity, returnedQuantity, productCondition, returnReason, notes, null);
    }

    public ReturnLineItemId getId() {
        return id;
    }

    public ProductId getProductId() {
        return productId;
    }

    public Quantity getOrderedQuantity() {
        return orderedQuantity;
    }

    public Quantity getPickedQuantity() {
        return pickedQuantity;
    }

    public Quantity getAcceptedQuantity() {
        return acceptedQuantity;
    }

    public Quantity getReturnedQuantity() {
        return returnedQuantity;
    }

    public ProductCondition getProductCondition() {
        return productCondition;
    }

    public ReturnReason getReturnReason() {
        return returnReason;
    }

    public Notes getLineNotes() {
        return lineNotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReturnLineItem that = (ReturnLineItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("ReturnLineItem{id=%s, productId=%s, returnedQuantity=%s}", id, productId, returnedQuantity);
    }
}
