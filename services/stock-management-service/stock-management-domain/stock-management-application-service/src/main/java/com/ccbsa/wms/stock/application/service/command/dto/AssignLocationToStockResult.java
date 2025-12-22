package com.ccbsa.wms.stock.application.service.command.dto;

import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.valueobject.StockItemId;

/**
 * Result DTO: AssignLocationToStockResult
 * <p>
 * Result object returned after assigning a location to a stock item.
 */
public final class AssignLocationToStockResult {
    private final StockItemId stockItemId;
    private final LocationId locationId;

    private AssignLocationToStockResult(Builder builder) {
        this.stockItemId = builder.stockItemId;
        this.locationId = builder.locationId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public StockItemId getStockItemId() {
        return stockItemId;
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public static class Builder {
        private StockItemId stockItemId;
        private LocationId locationId;

        public Builder stockItemId(StockItemId stockItemId) {
            this.stockItemId = stockItemId;
            return this;
        }

        public Builder locationId(LocationId locationId) {
            this.locationId = locationId;
            return this;
        }

        public AssignLocationToStockResult build() {
            if (stockItemId == null) {
                throw new IllegalArgumentException("StockItemId is required");
            }
            if (locationId == null) {
                throw new IllegalArgumentException("LocationId is required");
            }
            return new AssignLocationToStockResult(this);
        }
    }
}

