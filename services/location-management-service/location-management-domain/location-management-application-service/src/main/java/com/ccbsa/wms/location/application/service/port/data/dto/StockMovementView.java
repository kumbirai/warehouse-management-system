package com.ccbsa.wms.location.application.service.port.data.dto;

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
import lombok.Getter;

/**
 * Read Model DTO: StockMovementView
 * <p>
 * Optimized read model representation of StockMovement aggregate for query operations.
 */
@Getter
@Builder
public final class StockMovementView {
    private final StockMovementId stockMovementId;
    private final String tenantId;
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

    public StockMovementView(StockMovementId stockMovementId, String tenantId, String stockItemId, ProductId productId, LocationId sourceLocationId,
                             LocationId destinationLocationId, Quantity quantity, MovementType movementType, MovementReason reason, MovementStatus status, UserId initiatedBy,
                             LocalDateTime initiatedAt, UserId completedBy, LocalDateTime completedAt, UserId cancelledBy, LocalDateTime cancelledAt, String cancellationReason) {
        if (stockMovementId == null) {
            throw new IllegalArgumentException("StockMovementId is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (productId == null) {
            throw new IllegalArgumentException("ProductId is required");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity is required");
        }
        if (movementType == null) {
            throw new IllegalArgumentException("MovementType is required");
        }
        if (reason == null) {
            throw new IllegalArgumentException("Reason is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
        this.stockMovementId = stockMovementId;
        this.tenantId = tenantId;
        this.stockItemId = stockItemId;
        this.productId = productId;
        this.sourceLocationId = sourceLocationId;
        this.destinationLocationId = destinationLocationId;
        this.quantity = quantity;
        this.movementType = movementType;
        this.reason = reason;
        this.status = status;
        this.initiatedBy = initiatedBy;
        this.initiatedAt = initiatedAt;
        this.completedBy = completedBy;
        this.completedAt = completedAt;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = cancelledAt;
        this.cancellationReason = cancellationReason;
    }
}

