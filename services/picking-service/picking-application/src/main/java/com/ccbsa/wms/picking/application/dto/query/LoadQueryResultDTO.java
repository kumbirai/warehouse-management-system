package com.ccbsa.wms.picking.application.dto.query;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: LoadQueryResultDTO
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder, and getters return defensive copies")
public class LoadQueryResultDTO {
    private final String id;
    private final String loadNumber;
    private final String status;
    private final ZonedDateTime createdAt;
    private final ZonedDateTime plannedAt;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder")
    private final List<OrderQueryResultDTO> orders;

    /**
     * Returns a defensive copy of the orders list to prevent external modification.
     *
     * @return unmodifiable copy of the orders list
     */
    public List<OrderQueryResultDTO> getOrders() {
        if (orders == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(orders));
    }

    @Getter
    @Builder
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder, and getter returns defensive copy")
    public static class OrderQueryResultDTO {
        private final String orderId;
        private final String orderNumber;
        private final String customerCode;
        private final String customerName;
        private final String priority;
        private final String status;
        @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder")
        private final List<OrderLineItemQueryResultDTO> lineItems;

        /**
         * Returns a defensive copy of the line items list to prevent external modification.
         *
         * @return unmodifiable copy of the line items list
         */
        public List<OrderLineItemQueryResultDTO> getLineItems() {
            if (lineItems == null) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(new ArrayList<>(lineItems));
        }
    }

    @Getter
    @Builder
    public static class OrderLineItemQueryResultDTO {
        private final String lineItemId;
        private final String productCode;
        private final String productDescription;
        private final int quantity;
        private final String notes;
    }
}
