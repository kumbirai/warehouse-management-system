package com.ccbsa.wms.picking.application.dto.query;

import java.time.ZonedDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: PickingListQueryResultDTO
 * <p>
 * DTO for picking list query results.
 */
@Getter
@Builder
public class PickingListQueryResultDTO {
    private final String id;
    private final String pickingListReference;
    private final String status;
    private final ZonedDateTime receivedAt;
    private final ZonedDateTime processedAt;
    private final int loadCount;
    private final int totalOrderCount;
    private final String notes;
    private final List<LoadQueryResultDTO> loads;

    @Getter
    @Builder
    public static class LoadQueryResultDTO {
        private final String loadId;
        private final String loadNumber;
        private final String status;
        private final int orderCount;
        private final List<OrderQueryResultDTO> orders;
    }

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
