package com.ccbsa.wms.gateway.api.dto;

import java.util.UUID;

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
public class CreateStockAdjustmentRequest {
    private UUID productId;
    private UUID locationId; // Optional - null for product-wide adjustment
    private UUID stockItemId; // Optional - null for product/location adjustment
    private String adjustmentType; // INCREASE, DECREASE, CORRECTION
    private Integer quantity;
    private String reason; // STOCK_COUNT, DAMAGE, CORRECTION, THEFT, EXPIRATION, OTHER
    private String notes;
    private String authorizationCode; // For large adjustments
}

