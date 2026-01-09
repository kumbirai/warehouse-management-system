package com.ccbsa.wms.picking.application.service.query.dto;

import java.time.ZonedDateTime;
import java.util.List;

import com.ccbsa.common.domain.valueobject.LoadNumber;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: LoadQueryResult
 * <p>
 * Result object returned from load queries.
 */
@Getter
@Builder
public final class LoadQueryResult {
    private final LoadId id;
    private final LoadNumber loadNumber;
    private final LoadStatus status;
    private final ZonedDateTime createdAt;
    private final ZonedDateTime plannedAt;
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
