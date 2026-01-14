package com.ccbsa.wms.returns.application.service.command.dto;

import java.time.Instant;
import java.util.List;

import com.ccbsa.common.domain.valueobject.Notes;
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.ReturnReason;
import com.ccbsa.common.domain.valueobject.ReturnLineItemId;

import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: HandlePartialOrderAcceptanceCommand
 * <p>
 * Command object for handling partial order acceptance.
 */
@Getter
public final class HandlePartialOrderAcceptanceCommand {
    private final OrderNumber orderNumber;
    private final List<PartialReturnLineItemCommand> lineItems;
    private final String signatureData;
    private final Instant signedAt;

    @Builder
    public HandlePartialOrderAcceptanceCommand(OrderNumber orderNumber, List<PartialReturnLineItemCommand> lineItems, String signatureData, Instant signedAt) {
        if (orderNumber == null) {
            throw new IllegalArgumentException("OrderNumber is required");
        }
        if (lineItems == null || lineItems.isEmpty()) {
            throw new IllegalArgumentException("At least one line item is required");
        }
        if (signatureData == null || signatureData.trim().isEmpty()) {
            throw new IllegalArgumentException("Signature data is required");
        }
        if (signedAt == null) {
            throw new IllegalArgumentException("Signed at timestamp is required");
        }
        this.orderNumber = orderNumber;
        this.lineItems = List.copyOf(lineItems);
        this.signatureData = signatureData;
        this.signedAt = signedAt;
    }

    @Getter
    public static final class PartialReturnLineItemCommand {
        private final ReturnLineItemId lineItemId;
        private final ProductId productId;
        private final Quantity orderedQuantity;
        private final Quantity pickedQuantity;
        private final Quantity acceptedQuantity;
        private final ReturnReason returnReason;
        private final Notes lineNotes;

        @Builder
        public PartialReturnLineItemCommand(ReturnLineItemId lineItemId, ProductId productId, Quantity orderedQuantity, Quantity pickedQuantity, Quantity acceptedQuantity,
                                            ReturnReason returnReason, Notes lineNotes) {
            if (lineItemId == null) {
                throw new IllegalArgumentException("LineItemId is required");
            }
            if (productId == null) {
                throw new IllegalArgumentException("ProductId is required");
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
            // Calculate returned quantity
            int returnedQuantity = pickedQuantity.getValue() - acceptedQuantity.getValue();
            if (returnedQuantity > 0 && returnReason == null) {
                throw new IllegalArgumentException("Return reason is required when returned quantity is greater than zero");
            }
            this.lineItemId = lineItemId;
            this.productId = productId;
            this.orderedQuantity = orderedQuantity;
            this.pickedQuantity = pickedQuantity;
            this.acceptedQuantity = acceptedQuantity;
            this.returnReason = returnReason;
            this.lineNotes = lineNotes;
        }
    }
}
