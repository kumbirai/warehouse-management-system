package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStockMovementRequest {
    private String productId;
    private String sourceLocationId;
    private String targetLocationId;
    private Integer quantity;
    private String movementType;
    private String reason;
}

