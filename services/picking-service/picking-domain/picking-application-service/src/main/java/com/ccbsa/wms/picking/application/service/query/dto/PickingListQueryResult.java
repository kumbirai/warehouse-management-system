package com.ccbsa.wms.picking.application.service.query.dto;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListReference;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListStatus;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: PickingListQueryResult
 * <p>
 * Result object returned from picking list queries.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor and getters return defensive copies")
public final class PickingListQueryResult {
    private final PickingListId id;
    private final PickingListReference pickingListReference;
    private final PickingListStatus status;
    private final ZonedDateTime receivedAt;
    private final ZonedDateTime processedAt;
    private final int loadCount;
    private final int totalOrderCount;
    private final String notes;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor")
    private final List<LoadQueryResult> loads;

    public PickingListQueryResult(PickingListId id, PickingListReference pickingListReference, PickingListStatus status, ZonedDateTime receivedAt, ZonedDateTime processedAt,
                                  int loadCount, int totalOrderCount, String notes, List<LoadQueryResult> loads) {
        this.id = id;
        this.pickingListReference = pickingListReference;
        this.status = status;
        this.receivedAt = receivedAt;
        this.processedAt = processedAt;
        this.loadCount = loadCount;
        this.totalOrderCount = totalOrderCount;
        this.notes = notes;
        this.loads = loads != null ? List.copyOf(loads) : List.of();
    }

    /**
     * Returns a defensive copy of the loads list to prevent external modification.
     *
     * @return unmodifiable copy of the loads list
     */
    public List<LoadQueryResult> getLoads() {
        return Collections.unmodifiableList(loads);
    }

    /**
     * Query Result DTO: LoadQueryResult
     */
    @Getter
    @Builder
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder, and getter returns defensive copy")
    public static final class LoadQueryResult {
        private final String loadId;
        private final String loadNumber;
        private final String status;
        private final int orderCount;
        @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder")
        private final List<OrderQueryResult> orders;

        /**
         * Returns a defensive copy of the orders list to prevent external modification.
         *
         * @return unmodifiable copy of the orders list
         */
        public List<OrderQueryResult> getOrders() {
            if (orders == null) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(new ArrayList<>(orders));
        }
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
