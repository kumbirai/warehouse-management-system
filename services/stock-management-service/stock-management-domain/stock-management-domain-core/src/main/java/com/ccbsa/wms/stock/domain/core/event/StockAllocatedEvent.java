package com.ccbsa.wms.stock.domain.core.event;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.AllocationType;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.entity.StockAllocation;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAllocationId;

/**
 * Domain Event: StockAllocatedEvent
 * <p>
 * Published when stock is successfully allocated.
 * <p>
 * This event indicates that:
 * - Stock has been allocated for picking order or reservation
 * - Used for updating stock item allocated quantity
 * - May trigger location capacity reservation
 */
public class StockAllocatedEvent extends StockManagementEvent<StockAllocation> {

    private final StockAllocationId allocationId;
    private final TenantId tenantId;
    private final ProductId productId;
    private final LocationId locationId;
    private final StockItemId stockItemId;
    private final Quantity quantity;
    private final AllocationType allocationType;
    private final String referenceId;
    private final UserId allocatedBy;
    private final LocalDateTime allocatedAt;

    /**
     * Constructor for StockAllocatedEvent.
     *
     * @param allocationId   Allocation identifier
     * @param tenantId       Tenant identifier
     * @param productId      Product identifier
     * @param locationId     Location identifier (optional)
     * @param stockItemId    Stock item identifier
     * @param quantity       Allocated quantity
     * @param allocationType Allocation type
     * @param referenceId    Reference ID (order ID, picking list ID, etc.)
     * @param allocatedBy    User who allocated the stock
     * @param allocatedAt    Allocation timestamp
     */
    public StockAllocatedEvent(StockAllocationId allocationId, TenantId tenantId, ProductId productId, LocationId locationId, StockItemId stockItemId, Quantity quantity,
                               AllocationType allocationType, String referenceId, UserId allocatedBy, LocalDateTime allocatedAt) {
        super(allocationId.getValueAsString(), "StockAllocation");
        this.allocationId = allocationId;
        this.tenantId = tenantId;
        this.productId = productId;
        this.locationId = locationId;
        this.stockItemId = stockItemId;
        this.quantity = quantity;
        this.allocationType = allocationType;
        this.referenceId = referenceId;
        this.allocatedBy = allocatedBy;
        this.allocatedAt = allocatedAt;
    }

    /**
     * Constructor for StockAllocatedEvent with metadata.
     *
     * @param allocationId   Allocation identifier
     * @param tenantId       Tenant identifier
     * @param productId      Product identifier
     * @param locationId     Location identifier (optional)
     * @param stockItemId    Stock item identifier
     * @param quantity       Allocated quantity
     * @param allocationType Allocation type
     * @param referenceId    Reference ID (order ID, picking list ID, etc.)
     * @param allocatedBy    User who allocated the stock
     * @param allocatedAt    Allocation timestamp
     * @param metadata       Event metadata (correlation ID, user ID, etc.)
     */
    public StockAllocatedEvent(StockAllocationId allocationId, TenantId tenantId, ProductId productId, LocationId locationId, StockItemId stockItemId, Quantity quantity,
                               AllocationType allocationType, String referenceId, UserId allocatedBy, LocalDateTime allocatedAt, EventMetadata metadata) {
        super(allocationId.getValueAsString(), "StockAllocation", metadata);
        this.allocationId = allocationId;
        this.tenantId = tenantId;
        this.productId = productId;
        this.locationId = locationId;
        this.stockItemId = stockItemId;
        this.quantity = quantity;
        this.allocationType = allocationType;
        this.referenceId = referenceId;
        this.allocatedBy = allocatedBy;
        this.allocatedAt = allocatedAt;
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

    public AllocationType getAllocationType() {
        return allocationType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public UserId getAllocatedBy() {
        return allocatedBy;
    }

    public LocalDateTime getAllocatedAt() {
        return allocatedAt;
    }
}

