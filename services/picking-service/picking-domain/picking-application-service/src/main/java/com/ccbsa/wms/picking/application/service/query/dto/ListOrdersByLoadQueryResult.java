package com.ccbsa.wms.picking.application.service.query.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: ListOrdersByLoadQueryResult
 * <p>
 * Result object returned from listing orders by load query.
 */
@Getter
@Builder
public final class ListOrdersByLoadQueryResult {
    private final String loadId;
    private final List<OrderQueryResult> orders;

    public ListOrdersByLoadQueryResult(String loadId, List<OrderQueryResult> orders) {
        this.loadId = loadId;
        this.orders = orders != null ? List.copyOf(orders) : List.of();
    }

    /**
     * Query Result DTO: OrderQueryResult
     */
    @Getter
    @Builder
    public static final class OrderQueryResult {
        private final String orderId;
        private final String orderNumber;
        private final String customerCode;
        private final String customerName;
        private final String priority;
        private final String status;
        private final List<OrderLineItemQueryResult> lineItems;
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
