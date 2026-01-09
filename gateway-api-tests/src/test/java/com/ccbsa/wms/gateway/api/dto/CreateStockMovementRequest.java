package com.ccbsa.wms.gateway.api.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStockMovementRequest {
    private String stockItemId;
    private UUID productId;
    private UUID sourceLocationId;
    private UUID destinationLocationId;
    private Integer quantity;
    private String movementType; // RECEIVING_TO_STORAGE, STORAGE_TO_PICKING, INTER_STORAGE, PICKING_TO_SHIPPING, OTHER
    private String reason; // MovementReason enum value
}

