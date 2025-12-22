package com.ccbsa.wms.stock.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.stock.domain.core.valueobject.StockItemId;

/**
 * Query DTO: GetStockItemQuery
 * <p>
 * Query object for retrieving a stock item by ID.
 */
public final class GetStockItemQuery {
    private final StockItemId stockItemId;
    private final TenantId tenantId;

    private GetStockItemQuery(Builder builder) {
        this.stockItemId = builder.stockItemId;
        this.tenantId = builder.tenantId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public StockItemId getStockItemId() {
        return stockItemId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public static class Builder {
        private StockItemId stockItemId;
        private TenantId tenantId;

        public Builder stockItemId(StockItemId stockItemId) {
            this.stockItemId = stockItemId;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public GetStockItemQuery build() {
            if (stockItemId == null) {
                throw new IllegalArgumentException("StockItemId is required");
            }
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            return new GetStockItemQuery(this);
        }
    }
}

