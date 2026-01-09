package com.ccbsa.wms.picking.domain.core.event;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.domain.core.entity.PickingTask;

/**
 * Domain Event: PickingTaskCreatedEvent
 * <p>
 * Published when a picking task is created during load planning.
 * <p>
 * This event indicates that:
 * - A picking task has been created for a specific location/product
 * - Task is in PENDING status
 * - Task is ready for picking execution
 */
public class PickingTaskCreatedEvent extends PickingEvent<PickingTask> {
    private static final String AGGREGATE_TYPE = "PickingTask";

    private final TenantId tenantId;
    private final String loadId;
    private final String orderId;
    private final String productCode;
    private final String locationId;
    private final int quantity;
    private final int sequence;

    /**
     * Constructor for PickingTaskCreatedEvent.
     *
     * @param aggregateId Picking task ID (as String)
     * @param tenantId    Tenant identifier
     * @param loadId      Load ID (as String)
     * @param orderId     Order ID (as String)
     * @param productCode Product code
     * @param locationId  Location ID (as String)
     * @param quantity    Quantity to pick
     * @param sequence    Picking sequence number
     */
    public PickingTaskCreatedEvent(String aggregateId, TenantId tenantId, String loadId, String orderId, String productCode, String locationId, int quantity, int sequence) {
        super(aggregateId, AGGREGATE_TYPE);
        this.tenantId = tenantId;
        this.loadId = loadId;
        this.orderId = orderId;
        this.productCode = productCode;
        this.locationId = locationId;
        this.quantity = quantity;
        this.sequence = sequence;
    }

    /**
     * Constructor for PickingTaskCreatedEvent with metadata.
     *
     * @param aggregateId Picking task ID (as String)
     * @param tenantId    Tenant identifier
     * @param loadId      Load ID (as String)
     * @param orderId     Order ID (as String)
     * @param productCode Product code
     * @param locationId  Location ID (as String)
     * @param quantity    Quantity to pick
     * @param sequence    Picking sequence number
     * @param metadata    Event metadata for traceability
     */
    public PickingTaskCreatedEvent(String aggregateId, TenantId tenantId, String loadId, String orderId, String productCode, String locationId, int quantity, int sequence,
                                   EventMetadata metadata) {
        super(aggregateId, AGGREGATE_TYPE, metadata);
        this.tenantId = tenantId;
        this.loadId = loadId;
        this.orderId = orderId;
        this.productCode = productCode;
        this.locationId = locationId;
        this.quantity = quantity;
        this.sequence = sequence;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public String getLoadId() {
        return loadId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getLocationId() {
        return locationId;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getSequence() {
        return sequence;
    }
}
