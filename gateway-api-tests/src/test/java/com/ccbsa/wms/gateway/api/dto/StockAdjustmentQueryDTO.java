package com.ccbsa.wms.gateway.api.dto;

import java.time.LocalDateTime;

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

