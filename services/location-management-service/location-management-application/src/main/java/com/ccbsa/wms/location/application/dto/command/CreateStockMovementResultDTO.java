package com.ccbsa.wms.location.application.dto.command;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ccbsa.wms.location.domain.core.valueobject.MovementStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Result DTO: CreateStockMovementResultDTO
 * <p>
 * Response DTO for creating a stock movement.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStockMovementResultDTO {
    private UUID stockMovementId;
    private MovementStatus status;
    private LocalDateTime initiatedAt;
}

