package com.ccbsa.wms.stock.application.service.query.dto;

import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: GetStockLevelsQueryResult
 * <p>
 * Result object returned from stock level queries.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Defensive copy made in constructor and getter returns immutable view.")
public final class GetStockLevelsQueryResult {
    private final List<StockLevelResult> stockLevels;

    public GetStockLevelsQueryResult(List<StockLevelResult> stockLevels) {
        // Defensive copy to prevent external modification
        this.stockLevels = stockLevels != null ? List.copyOf(stockLevels) : List.of();
    }

    /**
     * Returns an unmodifiable view of the stock levels list.
     *
     * @return Unmodifiable list of stock levels
     */
    public List<StockLevelResult> getStockLevels() {
        return Collections.unmodifiableList(stockLevels);
    }

    /**
     * Individual stock level result.
     */
    @Getter
    @Builder
    public static final class StockLevelResult {
        private final String productId;
        private final String locationId;
        private final Integer availableQuantity;
        private final Integer allocatedQuantity;
        private final Integer totalQuantity;
        private final Integer minimumQuantity;
        private final Integer maximumQuantity;

        public StockLevelResult(String productId, String locationId, Integer availableQuantity, Integer allocatedQuantity, Integer totalQuantity, Integer minimumQuantity,
                                Integer maximumQuantity) {
            this.productId = productId;
            this.locationId = locationId;
            this.availableQuantity = availableQuantity != null ? availableQuantity : 0;
            this.allocatedQuantity = allocatedQuantity != null ? allocatedQuantity : 0;
            this.totalQuantity = totalQuantity != null ? totalQuantity : 0;
            this.minimumQuantity = minimumQuantity;
            this.maximumQuantity = maximumQuantity;
        }
    }
}
