package com.ccbsa.wms.stock.domain.core.event;

import com.ccbsa.common.domain.valueobject.ExpirationDate;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;

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
    private final ExpirationDate expirationDate;

    /**
     * Constructor for StockExpiredEvent.
     *
     * @param stockItemId    Stock item identifier
     * @param productId      Product identifier
     * @param expirationDate Expiration date
     */
    public StockExpiredEvent(StockItemId stockItemId, ProductId productId, ExpirationDate expirationDate) {
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

    public ExpirationDate getExpirationDate() {
        return expirationDate;
    }
}

