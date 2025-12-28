package com.ccbsa.wms.stock.domain.core.event;

import java.math.BigDecimal;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.entity.StockLevelThreshold;
import com.ccbsa.wms.stock.domain.core.valueobject.StockLevelThresholdId;

/**
 * Domain Event: StockLevelAboveMaximumEvent
 * <p>
 * Published when stock level exceeds maximum threshold.
 * <p>
 * This event indicates that:
 * - Stock level has exceeded the configured maximum
 * - Used for notifications and alerts
 * - May trigger overflow handling workflows
 */
public class StockLevelAboveMaximumEvent extends StockManagementEvent<StockLevelThreshold> {

    private final StockLevelThresholdId thresholdId;
    private final TenantId tenantId;
    private final ProductId productId;
    private final LocationId locationId;
    private final BigDecimal currentQuantity;
    private final BigDecimal maximumQuantity;

    /**
     * Constructor for StockLevelAboveMaximumEvent.
     *
     * @param thresholdId     Threshold identifier
     * @param tenantId        Tenant identifier
     * @param productId       Product identifier
     * @param locationId      Location identifier (optional)
     * @param currentQuantity Current stock quantity
     * @param maximumQuantity Maximum threshold quantity
     */
    public StockLevelAboveMaximumEvent(StockLevelThresholdId thresholdId, TenantId tenantId, ProductId productId, LocationId locationId, BigDecimal currentQuantity,
                                       BigDecimal maximumQuantity) {
        super(thresholdId.getValueAsString(), "StockLevelThreshold");
        this.thresholdId = thresholdId;
        this.tenantId = tenantId;
        this.productId = productId;
        this.locationId = locationId;
        this.currentQuantity = currentQuantity;
        this.maximumQuantity = maximumQuantity;
    }

    /**
     * Constructor for StockLevelAboveMaximumEvent with metadata.
     *
     * @param thresholdId     Threshold identifier
     * @param tenantId        Tenant identifier
     * @param productId       Product identifier
     * @param locationId      Location identifier (optional)
     * @param currentQuantity Current stock quantity
     * @param maximumQuantity Maximum threshold quantity
     * @param metadata        Event metadata (correlation ID, user ID, etc.)
     */
    public StockLevelAboveMaximumEvent(StockLevelThresholdId thresholdId, TenantId tenantId, ProductId productId, LocationId locationId, BigDecimal currentQuantity,
                                       BigDecimal maximumQuantity, EventMetadata metadata) {
        super(thresholdId.getValueAsString(), "StockLevelThreshold", metadata);
        this.thresholdId = thresholdId;
        this.tenantId = tenantId;
        this.productId = productId;
        this.locationId = locationId;
        this.currentQuantity = currentQuantity;
        this.maximumQuantity = maximumQuantity;
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

    public BigDecimal getMaximumQuantity() {
        return maximumQuantity;
    }
}

