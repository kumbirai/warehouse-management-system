package com.ccbsa.wms.gateway.api.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Response DTO for order query.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderQueryResult {
    private String orderNumber;
    private List<OrderLineItemQueryResult> lineItems;

    @Getter
@Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderLineItemQueryResult {
        private String productId;
        private Integer orderedQuantity;
        private Integer pickedQuantity;
    }
}
