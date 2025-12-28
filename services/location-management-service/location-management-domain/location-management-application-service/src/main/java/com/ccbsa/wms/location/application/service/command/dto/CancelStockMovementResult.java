package com.ccbsa.wms.location.application.service.command.dto;

import java.time.LocalDateTime;

import com.ccbsa.wms.location.domain.core.valueobject.MovementStatus;
import com.ccbsa.wms.location.domain.core.valueobject.StockMovementId;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Result DTO: CancelStockMovementResult
 * <p>
 * Result for cancelling a stock movement.
 */
@Getter
@Builder
@EqualsAndHashCode
public final class CancelStockMovementResult {
    private final StockMovementId stockMovementId;
    private final MovementStatus status;
    private final LocalDateTime cancelledAt;
}

