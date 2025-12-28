package com.ccbsa.wms.stock.domain.core.event;

import java.math.BigDecimal;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.entity.StockLevelThreshold;
import com.ccbsa.wms.stock.domain.core.valueobject.StockLevelThresholdId;

/**
 * Domain Event: StockLevelBelowMinimumEvent
 * <p>
 * Published when stock level falls below minimum threshold.
 * <p>
 * This event indicates that:
 * - Stock level has dropped below the configured minimum
 * - May trigger auto-restock if enabled
 * - Used for notifications and alerts
 */
public class StockLevelBelowMinimumEvent extends StockManagementEvent<StockLevelThreshold> {

    private final StockLevelThresholdId thresholdId;
    private final TenantId tenantId;
    private final ProductId productId;
    private final LocationId locationId;
    private final BigDecimal currentQuantity;
    private final BigDecimal minimumQuantity;
    private final boolean enableAutoRestock;

    /**
     * Constructor for StockLevelBelowMinimumEvent.
     *
     * @param thresholdId       Threshold identifier
     * @param tenantId          Tenant identifier
     * @param productId         Product identifier
     * @param locationId        Location identifier (optional)
     * @param currentQuantity   Current stock quantity
     * @param minimumQuantity   Minimum threshold quantity
     * @param enableAutoRestock Whether auto-restock is enabled
     */
    public StockLevelBelowMinimumEvent(StockLevelThresholdId thresholdId, TenantId tenantId, ProductId productId, LocationId locationId, BigDecimal currentQuantity,
                                       BigDecimal minimumQuantity, boolean enableAutoRestock) {
        super(thresholdId.getValueAsString(), "StockLevelThreshold");
        this.thresholdId = thresholdId;
        this.tenantId = tenantId;
        this.productId = productId;
        this.locationId = locationId;
        this.currentQuantity = currentQuantity;
        this.minimumQuantity = minimumQuantity;
        this.enableAutoRestock = enableAutoRestock;
    }

    /**
     * Constructor for StockLevelBelowMinimumEvent with metadata.
     *
     * @param thresholdId       Threshold identifier
     * @param tenantId          Tenant identifier
     * @param productId         Product identifier
     * @param locationId        Location identifier (optional)
     * @param currentQuantity   Current stock quantity
     * @param minimumQuantity   Minimum threshold quantity
     * @param enableAutoRestock Whether auto-restock is enabled
     * @param metadata          Event metadata (correlation ID, user ID, etc.)
     */
    public StockLevelBelowMinimumEvent(StockLevelThresholdId thresholdId, TenantId tenantId, ProductId productId, LocationId locationId, BigDecimal currentQuantity,
                                       BigDecimal minimumQuantity, boolean enableAutoRestock, EventMetadata metadata) {
        super(thresholdId.getValueAsString(), "StockLevelThreshold", metadata);
        this.thresholdId = thresholdId;
        this.tenantId = tenantId;
        this.productId = productId;
        this.locationId = locationId;
        this.currentQuantity = currentQuantity;
        this.minimumQuantity = minimumQuantity;
        this.enableAutoRestock = enableAutoRestock;
    }

    public StockLevelThresholdId getThresholdId() {
        return thresholdId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public BigDecimal getCurrentQuantity() {
        return currentQuantity;
    }

    public BigDecimal getMinimumQuantity() {
        return minimumQuantity;
    }

    public boolean isEnableAutoRestock() {
        return enableAutoRestock;
    }
}

