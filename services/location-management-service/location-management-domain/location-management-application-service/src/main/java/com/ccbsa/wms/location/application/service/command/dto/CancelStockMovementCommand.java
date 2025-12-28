package com.ccbsa.wms.location.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.domain.core.valueobject.StockMovementId;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Command DTO: CancelStockMovementCommand
 * <p>
 * Command for cancelling a stock movement.
 */
@Getter
@Builder
@EqualsAndHashCode
public final class CancelStockMovementCommand {
    private final TenantId tenantId;
    private final StockMovementId stockMovementId;
    private final UserId cancelledBy;
    private final String cancellationReason;
}

