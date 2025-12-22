package com.ccbsa.wms.stock.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.valueobject.Quantity;
import com.ccbsa.wms.stock.domain.core.valueobject.StockItemId;

/**
 * Command DTO: AssignLocationToStockCommand
 * <p>
 * Command object for assigning a location to a stock item.
 */
public final class AssignLocationToStockCommand {
    private final StockItemId stockItemId;
    private final LocationId locationId;
    private final Quantity quantity;
    private final TenantId tenantId;

    private AssignLocationToStockCommand(Builder builder) {
        this.stockItemId = builder.stockItemId;
        this.locationId = builder.locationId;
        this.quantity = builder.quantity;
        this.tenantId = builder.tenantId;
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

    public Quantity getQuantity() {
        return quantity;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public static class Builder {
        private StockItemId stockItemId;
        private LocationId locationId;
        private Quantity quantity;
        private TenantId tenantId;

        public Builder stockItemId(StockItemId stockItemId) {
            this.stockItemId = stockItemId;
            return this;
        }

        public Builder locationId(LocationId locationId) {
            this.locationId = locationId;
            return this;
        }

        public Builder quantity(Quantity quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public AssignLocationToStockCommand build() {
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
            return new AssignLocationToStockCommand(this);
        }
    }
}

