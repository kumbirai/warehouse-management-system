package com.ccbsa.wms.location.application.service.query.dto;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.valueobject.MovementReason;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.MovementStatus;
import com.ccbsa.wms.location.domain.core.valueobject.MovementType;
import com.ccbsa.wms.location.domain.core.valueobject.StockMovementId;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Query Result DTO: StockMovementQueryResult
 * <p>
 * Result for stock movement queries.
 */
@Getter
@Builder
@EqualsAndHashCode
public final class StockMovementQueryResult {
    private final StockMovementId stockMovementId;
    private final String stockItemId;
    private final ProductId productId;
    private final LocationId sourceLocationId;
    private final LocationId destinationLocationId;
    private final Quantity quantity;
    private final MovementType movementType;
    private final MovementReason reason;
    private final MovementStatus status;
    private final UserId initiatedBy;
    private final LocalDateTime initiatedAt;
    private final UserId completedBy;
    private final LocalDateTime completedAt;
    private final UserId cancelledBy;
    private final LocalDateTime cancelledAt;
    private final String cancellationReason;
}

