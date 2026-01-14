package com.ccbsa.wms.gateway.api.dto;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for handling partial order acceptance.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandlePartialOrderAcceptanceRequest {
    private String orderNumber;
    private List<PartialReturnLineItemRequest> lineItems;
    private String signatureData;
    private Instant signedAt;

    @Getter
@Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartialReturnLineItemRequest {
        private String productId;
        private Integer orderedQuantity;
        private Integer pickedQuantity;
        private Integer acceptedQuantity;
        private String returnReason;
        private String lineNotes;
    }
}
