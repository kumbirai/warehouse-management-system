package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStockAdjustmentResponse {
    private UUID adjustmentId;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private LocalDateTime adjustedAt;
}

