package com.ccbsa.wms.gateway.api.dto;

import java.time.LocalDateTime;
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
public class CompleteStockMovementResultDTO {
    private UUID stockMovementId;
    private String status; // MovementStatus enum value
    private LocalDateTime completedAt;
}

