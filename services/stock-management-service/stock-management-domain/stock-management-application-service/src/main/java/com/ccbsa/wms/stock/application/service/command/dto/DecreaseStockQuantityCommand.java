package com.ccbsa.wms.stock.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: DecreaseStockQuantityCommand
 * <p>
 * Command object for decreasing stock quantity after picking task completion.
 */
@Getter
@Builder
public final class DecreaseStockQuantityCommand {
    private final TenantId tenantId;
    private final String productCode;
    private final LocationId locationId;
    private final Integer quantity;
}
