package com.ccbsa.wms.picking.domain.core.event;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.domain.core.entity.Order;

/**
 * Domain Event: OrderMappedToLoadEvent
 * <p>
 * Published when an order is mapped to a load.
 * <p>
 * This event indicates that:
 * - An order has been added to a load
 * - Order-to-load relationship has been established
 */
public class OrderMappedToLoadEvent extends PickingEvent<Order> {
    private static final String AGGREGATE_TYPE = "Order";

    private final TenantId tenantId;
    private final String loadId;
    private final String orderNumber;

    /**
     * Constructor for OrderMappedToLoadEvent.
     *
     * @param aggregateId Order ID (as String)
     * @param tenantId    Tenant identifier
     * @param loadId      Load ID (as String)
     * @param orderNumber Order number
     */
    public OrderMappedToLoadEvent(String aggregateId, TenantId tenantId, String loadId, String orderNumber) {
        super(aggregateId, AGGREGATE_TYPE);
        this.tenantId = tenantId;
        this.loadId = loadId;
        this.orderNumber = orderNumber;
    }

    /**
     * Constructor for OrderMappedToLoadEvent with metadata.
     *
     * @param aggregateId Order ID (as String)
     * @param tenantId    Tenant identifier
     * @param loadId      Load ID (as String)
     * @param orderNumber Order number
     * @param metadata    Event metadata for traceability
     */
    public OrderMappedToLoadEvent(String aggregateId, TenantId tenantId, String loadId, String orderNumber, EventMetadata metadata) {
        super(aggregateId, AGGREGATE_TYPE, metadata);
        this.tenantId = tenantId;
        this.loadId = loadId;
        this.orderNumber = orderNumber;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public String getLoadId() {
        return loadId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }
}
