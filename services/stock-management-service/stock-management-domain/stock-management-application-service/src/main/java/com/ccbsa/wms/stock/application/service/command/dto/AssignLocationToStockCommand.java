package com.ccbsa.wms.stock.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: AssignLocationToStockCommand
 * <p>
 * Command object for assigning a location to a stock item.
 */
@Getter
@Builder
public final class AssignLocationToStockCommand {
    private final StockItemId stockItemId;
    private final LocationId locationId;
    private final Quantity quantity;
    private final TenantId tenantId;

    public AssignLocationToStockCommand(StockItemId stockItemId, LocationId locationId, Quantity quantity, TenantId tenantId) {
        if (stockItemId == null) {
            throw new IllegalArgumentException("StockItemId is required");
        }
        if (locationId == null) {
            throw new IllegalArgumentException("LocationId is required");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        this.stockItemId = stockItemId;
        this.locationId = locationId;
        this.quantity = quantity;
        this.tenantId = tenantId;
    }
}

