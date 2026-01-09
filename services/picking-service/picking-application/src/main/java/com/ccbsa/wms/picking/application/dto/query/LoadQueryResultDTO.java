package com.ccbsa.wms.picking.application.dto.query;

import java.time.ZonedDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: LoadQueryResultDTO
 */
@Getter
@Builder
public class LoadQueryResultDTO {
    private final String id;
    private final String loadNumber;
    private final String status;
    private final ZonedDateTime createdAt;
    private final ZonedDateTime plannedAt;
    private final List<OrderQueryResultDTO> orders;

    @Getter
    @Builder
    public static class OrderQueryResultDTO {
        private final String orderId;
        private final String orderNumber;
        private final String customerCode;
        private final String customerName;
        private final String priority;
        private final String status;
        private final List<OrderLineItemQueryResultDTO> lineItems;
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
