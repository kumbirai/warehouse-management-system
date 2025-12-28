package com.ccbsa.wms.stock.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.ExpirationDate;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;

import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: UpdateStockItemExpirationDateCommand
 * <p>
 * Command object for updating a stock item's expiration date.
 */
@Getter
@Builder
public final class UpdateStockItemExpirationDateCommand {
    private final StockItemId stockItemId;
    private final ExpirationDate expirationDate;
    private final TenantId tenantId;

    public UpdateStockItemExpirationDateCommand(StockItemId stockItemId, ExpirationDate expirationDate, TenantId tenantId) {
        if (stockItemId == null) {
            throw new IllegalArgumentException("StockItemId is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        // ExpirationDate can be null for non-perishable items
        this.stockItemId = stockItemId;
        this.expirationDate = expirationDate;
        this.tenantId = tenantId;
    }
}

