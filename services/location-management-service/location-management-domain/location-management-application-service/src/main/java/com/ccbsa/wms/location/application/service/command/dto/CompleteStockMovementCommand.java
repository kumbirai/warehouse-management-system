package com.ccbsa.wms.location.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.domain.core.valueobject.StockMovementId;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Command DTO: CompleteStockMovementCommand
 * <p>
 * Command for completing a stock movement.
 */
@Getter
@Builder
@EqualsAndHashCode
public final class CompleteStockMovementCommand {
    private final TenantId tenantId;
    private final StockMovementId stockMovementId;
    private final UserId completedBy;
}

