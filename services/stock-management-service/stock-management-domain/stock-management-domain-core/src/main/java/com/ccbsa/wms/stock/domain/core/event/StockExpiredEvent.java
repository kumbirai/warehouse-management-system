package com.ccbsa.wms.stock.domain.core.event;

import java.time.LocalDate;

import com.ccbsa.wms.product.domain.core.valueobject.ProductId;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;
import com.ccbsa.wms.stock.domain.core.valueobject.StockItemId;

/**
 * Domain Event: StockExpiredEvent
 * <p>
 * Published when stock item has expired.
 * <p>
 * This event indicates that:
 * - Stock item expiration date is in the past
 * - Stock cannot be picked
 * - May trigger removal or disposal workflows
 */
public class StockExpiredEvent extends StockManagementEvent<StockItem> {
    private static final String AGGREGATE_TYPE = "StockItem";

    private final StockItemId stockItemId;
    private final ProductId productId;
    private final LocalDate expirationDate;

    /**
     * Constructor for StockExpiredEvent.
     *
     * @param stockItemId    Stock item identifier
     * @param productId      Product identifier
     * @param expirationDate Expiration date
     */
    public StockExpiredEvent(StockItemId stockItemId, ProductId productId, LocalDate expirationDate) {
        super(stockItemId.getValueAsString(), AGGREGATE_TYPE);
        this.stockItemId = stockItemId;
        this.productId = productId;
        this.expirationDate = expirationDate;
    }

    public StockItemId getStockItemId() {
        return stockItemId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }
}

