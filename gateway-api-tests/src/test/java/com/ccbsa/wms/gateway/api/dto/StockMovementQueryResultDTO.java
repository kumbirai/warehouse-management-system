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
public class StockMovementQueryResultDTO {
    private UUID stockMovementId;
    private String stockItemId;
    private UUID productId;
    private UUID sourceLocationId;
    private UUID destinationLocationId;
    private Integer quantity;
    private String movementType;
    private String reason;
    private String status;
    private UUID initiatedBy;
    private LocalDateTime initiatedAt;
    private UUID completedBy;
    private LocalDateTime completedAt;
    private UUID cancelledBy;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
}

