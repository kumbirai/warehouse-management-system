package com.ccbsa.wms.stock.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.common.domain.valueobject.StockItemId;

import lombok.Builder;
import lombok.Getter;

/**
 * Result DTO: CreateStockItemResult
 * <p>
 * Result object returned after creating a stock item.
 */
@Getter
@Builder
public final class CreateStockItemResult {
    private final StockItemId stockItemId;
    private final StockClassification classification;

    public CreateStockItemResult(StockItemId stockItemId, StockClassification classification) {
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

