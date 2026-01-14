package com.ccbsa.wms.gateway.api.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for processing full order return.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessFullOrderReturnRequest {
    private String orderNumber;
    private List<FullReturnLineItemRequest> lineItems;
    private String primaryReturnReason;
    private String returnNotes;

    @Getter
@Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FullReturnLineItemRequest {
        private String productId;
        private Integer orderedQuantity;
        private Integer pickedQuantity;
        private String productCondition;
        private String returnReason;
        private String lineNotes;
    }
}
