package com.ccbsa.wms.picking.application.service.query.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: ListOrdersByLoadQueryResult
 * <p>
 * Result object returned from listing orders by load query.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor and getters return defensive copies")
public final class ListOrdersByLoadQueryResult {
    private final String loadId;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor")
    private final List<OrderQueryResult> orders;

    public ListOrdersByLoadQueryResult(String loadId, List<OrderQueryResult> orders) {
        this.loadId = loadId;
        this.orders = orders != null ? List.copyOf(orders) : List.of();
    }

    /**
     * Returns a defensive copy of the orders list to prevent external modification.
     *
     * @return unmodifiable copy of the orders list
     */
    public List<OrderQueryResult> getOrders() {
        return Collections.unmodifiableList(orders);
    }

    /**
     * Query Result DTO: OrderQueryResult
     */
    @Getter
    @Builder
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder, and getter returns defensive copy")
    public static final class OrderQueryResult {
        private final String orderId;
        private final String orderNumber;
        private final String customerCode;
        private final String customerName;
        private final String priority;
        private final String status;
        @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder")
        private final List<OrderLineItemQueryResult> lineItems;

        /**
         * Returns a defensive copy of the line items list to prevent external modification.
         *
         * @return unmodifiable copy of the line items list
         */
        public List<OrderLineItemQueryResult> getLineItems() {
            if (lineItems == null) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(new ArrayList<>(lineItems));
        }
    }

    /**
     * Query Result DTO: OrderLineItemQueryResult
     */
    @Getter
    @Builder
    public static final class OrderLineItemQueryResult {
        private final String lineItemId;
        private final String productCode;
        private final String productDescription;
        private final int quantity;
        private final String notes;
    }
}
