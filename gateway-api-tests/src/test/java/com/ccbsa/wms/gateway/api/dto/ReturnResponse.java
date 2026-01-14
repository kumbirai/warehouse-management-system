package com.ccbsa.wms.gateway.api.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for return query.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnResponse {
    private String returnId;
    private String orderNumber;
    private String returnType;
    private String status;
    private List<ReturnLineItemResponse> lineItems;
    private String primaryReturnReason;
    private String returnNotes;
    private LocalDateTime returnedAt;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    @Getter
@Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnLineItemResponse {
        private String lineItemId;
        private String productId;
        private Integer orderedQuantity;
        private Integer pickedQuantity;
        private Integer acceptedQuantity;
        private Integer returnedQuantity;
        private String productCondition;
        private String returnReason;
        private String lineNotes;
    }
}
