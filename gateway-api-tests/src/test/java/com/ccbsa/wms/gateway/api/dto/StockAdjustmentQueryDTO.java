package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentQueryDTO {
    private String adjustmentId;
    private String productId;
    private String locationId;
    private String stockItemId;
    private String adjustmentType;
    private Integer quantity;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private String reason;
    private String notes;
    private String adjustedBy;
    private String authorizationCode;
    private LocalDateTime adjustedAt;
}

