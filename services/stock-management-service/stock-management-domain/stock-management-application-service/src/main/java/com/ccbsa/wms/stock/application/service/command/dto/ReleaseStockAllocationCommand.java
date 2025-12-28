package com.ccbsa.wms.stock.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAllocationId;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Command DTO: ReleaseStockAllocationCommand
 * <p>
 * Command for releasing a stock allocation.
 */
@Getter
@Builder
@EqualsAndHashCode
public final class ReleaseStockAllocationCommand {
    private final TenantId tenantId;
    private final StockAllocationId allocationId;
}

