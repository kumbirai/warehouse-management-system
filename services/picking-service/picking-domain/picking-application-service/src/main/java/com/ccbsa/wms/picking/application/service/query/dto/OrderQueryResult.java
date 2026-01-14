package com.ccbsa.wms.picking.application.service.query.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: OrderQueryResult
 * <p>
 * Result object returned from order queries.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor and getter returns defensive copy")
public final class OrderQueryResult {
    private final String orderNumber;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor")
    private final List<OrderLineItemQueryResult> lineItems;

    public OrderQueryResult(String orderNumber, List<OrderLineItemQueryResult> lineItems) {
        this.orderNumber = orderNumber;
        this.lineItems = lineItems != null ? List.copyOf(lineItems) : List.of();
    }

    /**
     * Returns a defensive copy of the line items list to prevent external modification.
     *
     * @return unmodifiable copy of the line items list
     */
    public List<OrderLineItemQueryResult> getLineItems() {
        return Collections.unmodifiableList(lineItems);
    }

    /**
     * Query Result DTO: OrderLineItemQueryResult
     */
    @Getter
    @Builder
    public static final class OrderLineItemQueryResult {
        private final String productId;
        private final Integer orderedQuantity;
        private final Integer pickedQuantity;
    }
}
