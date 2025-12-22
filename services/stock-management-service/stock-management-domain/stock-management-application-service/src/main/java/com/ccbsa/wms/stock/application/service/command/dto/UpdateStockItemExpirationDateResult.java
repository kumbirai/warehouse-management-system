package com.ccbsa.wms.stock.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.wms.stock.domain.core.valueobject.StockItemId;

/**
 * Result DTO: UpdateStockItemExpirationDateResult
 * <p>
 * Result object returned after updating a stock item's expiration date.
 */
public final class UpdateStockItemExpirationDateResult {
    private final StockItemId stockItemId;
    private final StockClassification classification;

    private UpdateStockItemExpirationDateResult(Builder builder) {
        this.stockItemId = builder.stockItemId;
        this.classification = builder.classification;
    }

    public static Builder builder() {
        return new Builder();
    }

    public StockItemId getStockItemId() {
        return stockItemId;
    }

    public StockClassification getClassification() {
        return classification;
    }

    public static class Builder {
        private StockItemId stockItemId;
        private StockClassification classification;

        public Builder stockItemId(StockItemId stockItemId) {
            this.stockItemId = stockItemId;
            return this;
        }

        public Builder classification(StockClassification classification) {
            this.classification = classification;
            return this;
        }

        public UpdateStockItemExpirationDateResult build() {
            if (stockItemId == null) {
                throw new IllegalArgumentException("StockItemId is required");
            }
            if (classification == null) {
                throw new IllegalArgumentException("Classification is required");
            }
            return new UpdateStockItemExpirationDateResult(this);
        }
    }
}

