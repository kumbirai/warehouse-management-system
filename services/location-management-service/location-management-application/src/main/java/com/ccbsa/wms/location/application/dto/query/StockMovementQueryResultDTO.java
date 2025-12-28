package com.ccbsa.wms.location.application.dto.query;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ccbsa.common.domain.valueobject.MovementReason;
import com.ccbsa.wms.location.domain.core.valueobject.MovementStatus;
import com.ccbsa.wms.location.domain.core.valueobject.MovementType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Query Result DTO: StockMovementQueryResultDTO
 * <p>
 * Response DTO for stock movement queries.
 */
@Getter
@Setter
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
    private MovementType movementType;
    private MovementReason reason;
    private MovementStatus status;
    private UUID initiatedBy;
    private LocalDateTime initiatedAt;
    private UUID completedBy;
    private LocalDateTime completedAt;
    private UUID cancelledBy;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
}

