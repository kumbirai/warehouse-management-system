package com.ccbsa.wms.stock.domain.core.event;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.entity.StockAllocation;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAllocationId;

/**
 * Domain Event: StockAllocationReleasedEvent
 * <p>
 * Published when stock allocation is released.
 * <p>
 * This event indicates that:
 * - Stock allocation has been released
 * - Used for updating stock item allocated quantity
 * - May trigger location capacity updates
 */
public class StockAllocationReleasedEvent extends StockManagementEvent<StockAllocation> {

    private final StockAllocationId allocationId;
    private final TenantId tenantId;
    private final ProductId productId;
    private final LocationId locationId;
    private final StockItemId stockItemId;
    private final Quantity quantity;

    /**
     * Constructor for StockAllocationReleasedEvent.
     *
     * @param allocationId Allocation identifier
     * @param tenantId     Tenant identifier
     * @param productId    Product identifier
     * @param locationId   Location identifier (optional)
     * @param stockItemId  Stock item identifier
     * @param quantity     Released quantity
     */
    public StockAllocationReleasedEvent(StockAllocationId allocationId, TenantId tenantId, ProductId productId, LocationId locationId, StockItemId stockItemId, Quantity quantity) {
        super(allocationId.getValueAsString(), "StockAllocation");
        this.allocationId = allocationId;
        this.tenantId = tenantId;
        this.productId = productId;
        this.locationId = locationId;
        this.stockItemId = stockItemId;
        this.quantity = quantity;
    }

    /**
     * Constructor for StockAllocationReleasedEvent with metadata.
     *
     * @param allocationId Allocation identifier
     * @param tenantId     Tenant identifier
     * @param productId    Product identifier
     * @param locationId   Location identifier (optional)
     * @param stockItemId  Stock item identifier
     * @param quantity     Released quantity
     * @param metadata     Event metadata (correlation ID, user ID, etc.)
     */
    public StockAllocationReleasedEvent(StockAllocationId allocationId, TenantId tenantId, ProductId productId, LocationId locationId, StockItemId stockItemId, Quantity quantity,
                                        EventMetadata metadata) {
        super(allocationId.getValueAsString(), "StockAllocation", metadata);
        this.allocationId = allocationId;
        this.tenantId = tenantId;
        this.productId = productId;
        this.locationId = locationId;
        this.stockItemId = stockItemId;
        this.quantity = quantity;
    }

    public StockAllocationId getAllocationId() {
        return allocationId;
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

    public StockItemId getStockItemId() {
        return stockItemId;
    }

    public Quantity getQuantity() {
        return quantity;
    }
}

