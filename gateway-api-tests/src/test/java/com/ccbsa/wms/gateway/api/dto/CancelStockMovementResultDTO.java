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
public class CancelStockMovementResultDTO {
    private UUID stockMovementId;
    private String status; // MovementStatus enum value
    private LocalDateTime cancelledAt;
    private String cancellationReason;
}

