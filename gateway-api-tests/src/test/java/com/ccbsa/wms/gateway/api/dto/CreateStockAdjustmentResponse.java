package com.ccbsa.wms.gateway.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

