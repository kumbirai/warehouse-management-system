package com.ccbsa.wms.gateway.api.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickingListQueryResult {
    private String id;
    private String status;
    private String receivedAt;
    private String processedAt;
    private int loadCount;
    private int totalOrderCount;
    private String notes;
    private List<LoadQueryResult> loads;

    @Getter
@Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoadQueryResult {
        private String loadId;
        private String loadNumber;
        private String status;
        private int orderCount;
        private List<OrderQueryResult> orders;
    }

    @Getter
@Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderQueryResult {
        private String orderId;
        private String orderNumber;
        private String customerCode;
        private String customerName;
        private String priority;
        private String status;
        private List<OrderLineItemQueryResult> lineItems;
    }

    @Getter
@Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderLineItemQueryResult {
        private String lineItemId;
        private String productCode;
        private int quantity;
        private String notes;
    }
}
