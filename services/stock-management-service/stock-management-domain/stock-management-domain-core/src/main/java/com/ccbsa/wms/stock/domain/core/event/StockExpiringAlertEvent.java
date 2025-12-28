package com.ccbsa.wms.stock.domain.core.event;

import java.time.LocalDate;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;

/**
 * Domain Event: StockExpiringAlertEvent
 * <p>
 * Published when stock is expiring within a critical time period.
 * <p>
 * This event indicates that:
 * - Stock is expiring within 7 days (CRITICAL) or 30 days (NEAR_EXPIRY)
 * - Triggers alerts and notifications
 * - May trigger priority picking or restocking actions
 */
public class StockExpiringAlertEvent extends StockManagementEvent<StockItem> {
    private static final String AGGREGATE_TYPE = "StockItem";

    private final StockItemId stockItemId;
    private final ProductId productId;
    private final LocalDate expirationDate;
    private final int daysUntilExpiry;

    /**
     * Constructor for StockExpiringAlertEvent.
     *
     * @param stockItemId     Stock item identifier
     * @param productId       Product identifier
     * @param expirationDate  Expiration date
     * @param daysUntilExpiry Number of days until expiration (7 for CRITICAL, 30 for NEAR_EXPIRY)
     */
    public StockExpiringAlertEvent(StockItemId stockItemId, ProductId productId, LocalDate expirationDate, int daysUntilExpiry) {
        super(stockItemId.getValueAsString(), AGGREGATE_TYPE);
        this.stockItemId = stockItemId;
        this.productId = productId;
        this.expirationDate = expirationDate;
        this.daysUntilExpiry = daysUntilExpiry;
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

    public int getDaysUntilExpiry() {
        return daysUntilExpiry;
    }
}

