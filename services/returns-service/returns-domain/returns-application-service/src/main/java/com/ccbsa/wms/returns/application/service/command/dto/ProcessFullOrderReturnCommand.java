package com.ccbsa.wms.returns.application.service.command.dto;

import java.util.List;

import com.ccbsa.common.domain.valueobject.Notes;
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.ProductCondition;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.ReturnReason;
import com.ccbsa.common.domain.valueobject.ReturnLineItemId;

import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: ProcessFullOrderReturnCommand
 * <p>
 * Command object for processing full order returns.
 */
@Getter
public final class ProcessFullOrderReturnCommand {
    private final OrderNumber orderNumber;
    private final List<FullReturnLineItemCommand> lineItems;
    private final ReturnReason primaryReturnReason;
    private final Notes returnNotes;

    @Builder
    public ProcessFullOrderReturnCommand(OrderNumber orderNumber, List<FullReturnLineItemCommand> lineItems, ReturnReason primaryReturnReason, Notes returnNotes) {
        if (orderNumber == null) {
            throw new IllegalArgumentException("OrderNumber is required");
        }
        if (lineItems == null || lineItems.isEmpty()) {
            throw new IllegalArgumentException("At least one line item is required");
        }
        if (primaryReturnReason == null) {
            throw new IllegalArgumentException("Primary return reason is required");
        }
        this.orderNumber = orderNumber;
        this.lineItems = List.copyOf(lineItems);
        this.primaryReturnReason = primaryReturnReason;
        this.returnNotes = returnNotes;
    }

    @Getter
    public static final class FullReturnLineItemCommand {
        private final ReturnLineItemId lineItemId;
        private final ProductId productId;
        private final Quantity orderedQuantity;
        private final Quantity pickedQuantity;
        private final ProductCondition productCondition;
        private final ReturnReason returnReason;
        private final Notes lineNotes;

        @Builder
        public FullReturnLineItemCommand(ReturnLineItemId lineItemId, ProductId productId, Quantity orderedQuantity, Quantity pickedQuantity, ProductCondition productCondition,
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
            if (productCondition == null) {
                throw new IllegalArgumentException("Product condition is required for full returns");
            }
            if (returnReason == null) {
                throw new IllegalArgumentException("Return reason is required");
            }
            this.lineItemId = lineItemId;
            this.productId = productId;
            this.orderedQuantity = orderedQuantity;
            this.pickedQuantity = pickedQuantity;
            this.productCondition = productCondition;
            this.returnReason = returnReason;
            this.lineNotes = lineNotes;
        }
    }
}
