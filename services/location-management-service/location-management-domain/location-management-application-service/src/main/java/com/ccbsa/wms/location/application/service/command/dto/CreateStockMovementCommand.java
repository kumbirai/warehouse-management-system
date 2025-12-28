package com.ccbsa.wms.location.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.MovementReason;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.MovementType;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Command DTO: CreateStockMovementCommand
 * <p>
 * Command for initiating a stock movement.
 */
@Getter
@Builder
@EqualsAndHashCode
public final class CreateStockMovementCommand {
    private final TenantId tenantId;
    private final String stockItemId; // Cross-service reference (StockItemId as String)
    private final ProductId productId;
    private final LocationId sourceLocationId;
    private final LocationId destinationLocationId;
    private final Quantity quantity;
    private final MovementType movementType;
    private final MovementReason reason;
    private final UserId initiatedBy;
}

