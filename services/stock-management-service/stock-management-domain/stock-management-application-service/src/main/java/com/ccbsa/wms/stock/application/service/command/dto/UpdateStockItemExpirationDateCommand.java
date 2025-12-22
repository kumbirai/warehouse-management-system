package com.ccbsa.wms.stock.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.ExpirationDate;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.stock.domain.core.valueobject.StockItemId;

/**
 * Command DTO: UpdateStockItemExpirationDateCommand
 * <p>
 * Command object for updating a stock item's expiration date.
 */
public final class UpdateStockItemExpirationDateCommand {
    private final StockItemId stockItemId;
    private final ExpirationDate expirationDate;
    private final TenantId tenantId;

    private UpdateStockItemExpirationDateCommand(Builder builder) {
        this.stockItemId = builder.stockItemId;
        this.expirationDate = builder.expirationDate;
        this.tenantId = builder.tenantId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public StockItemId getStockItemId() {
        return stockItemId;
    }

    public ExpirationDate getExpirationDate() {
        return expirationDate;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public static class Builder {
        private StockItemId stockItemId;
        private ExpirationDate expirationDate;
        private TenantId tenantId;

        public Builder stockItemId(StockItemId stockItemId) {
            this.stockItemId = stockItemId;
            return this;
        }

        public Builder expirationDate(ExpirationDate expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public UpdateStockItemExpirationDateCommand build() {
            if (stockItemId == null) {
                throw new IllegalArgumentException("StockItemId is required");
            }
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            // ExpirationDate can be null for non-perishable items
            return new UpdateStockItemExpirationDateCommand(this);
        }
    }
}

