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
public class CreateStockMovementResponse {
    private UUID stockMovementId;
    private String status; // INITIATED, IN_PROGRESS, COMPLETED, CANCELLED
    private LocalDateTime initiatedAt;
}

