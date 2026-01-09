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
public class CancelStockMovementResultDTO {
    private UUID stockMovementId;
    private String status; // MovementStatus enum value
    private LocalDateTime cancelledAt;
    private String cancellationReason;
}

