package com.ccbsa.wms.picking.application.service.query.dto;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ccbsa.common.domain.valueobject.LoadNumber;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadStatus;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: LoadQueryResult
 * <p>
 * Result object returned from load queries.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor and getter returns defensive copy")
public final class LoadQueryResult {
    private final LoadId id;
    private final LoadNumber loadNumber;
    private final LoadStatus status;
    private final ZonedDateTime createdAt;
    private final ZonedDateTime plannedAt;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor")
    private final List<OrderQueryResult> orders;

    public LoadQueryResult(LoadId id, LoadNumber loadNumber, LoadStatus status, ZonedDateTime createdAt, ZonedDateTime plannedAt, List<OrderQueryResult> orders) {
        this.id = id;
        this.loadNumber = loadNumber;
        this.status = status;
        this.createdAt = createdAt;
        this.plannedAt = plannedAt;
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
