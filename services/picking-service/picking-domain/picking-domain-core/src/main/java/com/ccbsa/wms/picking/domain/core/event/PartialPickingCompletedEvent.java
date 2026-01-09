package com.ccbsa.wms.picking.domain.core.event;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.domain.core.entity.PickingTask;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;
import com.ccbsa.wms.picking.domain.core.valueobject.LocationId;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;
import com.ccbsa.wms.picking.domain.core.valueobject.ProductCode;

/**
 * Domain Event: PartialPickingCompletedEvent
 * <p>
 * Published when a picking task is partially completed.
 * <p>
 * This event indicates that:
 * - A picking task has been executed with less than required quantity
 * - Stock has been picked from the location
 * - Stock levels need to be updated
 * - Stock movement needs to be recorded
 * - A reason for partial picking is provided
 */
public class PartialPickingCompletedEvent extends PickingEvent<PickingTask> {
    private static final String AGGREGATE_TYPE = "PickingTask";

    private final TenantId tenantId;
    private final PickingListId pickingListId;
    private final LoadId loadId;
    private final OrderId orderId;
    private final ProductCode productCode;
    private final LocationId locationId;
    private final int requiredQuantity;
    private final int pickedQuantity;
    private final String partialReason;
    private final String pickedByUserId;

    /**
     * Constructor for PartialPickingCompletedEvent.
     *
     * @param pickingTaskId    Picking task ID (as String)
     * @param tenantId         Tenant identifier
     * @param pickingListId    Picking list ID
     * @param loadId           Load ID
     * @param orderId          Order ID
     * @param productCode      Product code
     * @param locationId       Location ID
     * @param requiredQuantity Required quantity
     * @param pickedQuantity   Picked quantity
     * @param partialReason    Reason for partial picking
     * @param pickedByUserId   User ID who picked the stock
     */
    public PartialPickingCompletedEvent(String pickingTaskId, TenantId tenantId, PickingListId pickingListId, LoadId loadId, OrderId orderId, ProductCode productCode,
                                        LocationId locationId, int requiredQuantity, int pickedQuantity, String partialReason, String pickedByUserId) {
        super(pickingTaskId, AGGREGATE_TYPE);
        this.tenantId = tenantId;
        this.pickingListId = pickingListId;
        this.loadId = loadId;
        this.orderId = orderId;
        this.productCode = productCode;
        this.locationId = locationId;
        this.requiredQuantity = requiredQuantity;
        this.pickedQuantity = pickedQuantity;
        this.partialReason = partialReason;
        this.pickedByUserId = pickedByUserId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public PickingListId getPickingListId() {
        return pickingListId;
    }

    public LoadId getLoadId() {
        return loadId;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public ProductCode getProductCode() {
        return productCode;
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public int getRequiredQuantity() {
        return requiredQuantity;
    }

    public int getPickedQuantity() {
        return pickedQuantity;
    }

    public String getPartialReason() {
        return partialReason;
    }

    public String getPickedByUserId() {
        return pickedByUserId;
    }
}
