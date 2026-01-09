package com.ccbsa.wms.gateway.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestockRequestResponse {
    private String restockRequestId;
    private String productId;
    private String locationId;
    private BigDecimal currentQuantity;
    private BigDecimal minimumQuantity;
    private BigDecimal maximumQuantity;
    private BigDecimal requestedQuantity;
    private String priority; // HIGH, MEDIUM, LOW
    private String status; // PENDING, SENT_TO_D365, FULFILLED, CANCELLED
    private LocalDateTime createdAt;
    private LocalDateTime sentToD365At;
    private String d365OrderReference;
}
