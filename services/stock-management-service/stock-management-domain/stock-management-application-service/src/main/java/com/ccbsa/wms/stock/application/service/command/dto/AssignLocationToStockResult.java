package com.ccbsa.wms.stock.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.Builder;
import lombok.Getter;

/**
 * Result DTO: AssignLocationToStockResult
 * <p>
 * Result object returned after assigning a location to a stock item.
 */
@Getter
@Builder
public final class AssignLocationToStockResult {
    private final StockItemId stockItemId;
    private final LocationId locationId;

    public AssignLocationToStockResult(StockItemId stockItemId, LocationId locationId) {
        if (stockItemId == null) {
            throw new IllegalArgumentException("StockItemId is required");
        }
        if (locationId == null) {
            throw new IllegalArgumentException("LocationId is required");
        }
        this.stockItemId = stockItemId;
        this.locationId = locationId;
    }
}

