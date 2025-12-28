package com.ccbsa.wms.stock.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.common.domain.valueobject.StockItemId;

import lombok.Builder;
import lombok.Getter;

/**
 * Result DTO: UpdateStockItemExpirationDateResult
 * <p>
 * Result object returned after updating a stock item's expiration date.
 */
@Getter
@Builder
public final class UpdateStockItemExpirationDateResult {
    private final StockItemId stockItemId;
    private final StockClassification classification;

    public UpdateStockItemExpirationDateResult(StockItemId stockItemId, StockClassification classification) {
        if (stockItemId == null) {
            throw new IllegalArgumentException("StockItemId is required");
        }
        if (classification == null) {
            throw new IllegalArgumentException("Classification is required");
        }
        this.stockItemId = stockItemId;
        this.classification = classification;
    }
}

