package com.ccbsa.wms.returns.domain.core.event;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.returns.domain.core.entity.Return;

/**
 * Domain Event: ReturnProcessedEvent
 * <p>
 * Published when a return has been processed (full order return processed).
 * <p>
 * This event indicates that:
 * - Return has been processed and validated
 * - Return is in PROCESSED status
 * - Product conditions have been assessed
 */
public class ReturnProcessedEvent extends ReturnsEvent<Return> {
    private static final String AGGREGATE_TYPE = "Return";

    private final OrderNumber orderNumber;
    private final TenantId tenantId;

    /**
     * Constructor for ReturnProcessedEvent.
     *
     * @param aggregateId Return ID (as String)
     * @param orderNumber Order number
     * @param tenantId    Tenant identifier
     */
    public ReturnProcessedEvent(String aggregateId, OrderNumber orderNumber, TenantId tenantId) {
        super(aggregateId, AGGREGATE_TYPE);
        this.orderNumber = orderNumber;
        this.tenantId = tenantId;
    }

    /**
     * Constructor for ReturnProcessedEvent with metadata.
     *
     * @param aggregateId Return ID (as String)
     * @param orderNumber Order number
     * @param tenantId    Tenant identifier
     * @param metadata    Event metadata for traceability
     */
    public ReturnProcessedEvent(String aggregateId, OrderNumber orderNumber, TenantId tenantId, EventMetadata metadata) {
        super(aggregateId, AGGREGATE_TYPE, metadata);
        this.orderNumber = orderNumber;
        this.tenantId = tenantId;
    }

    public OrderNumber getOrderNumber() {
        return orderNumber;
    }

    public TenantId getTenantId() {
        return tenantId;
    }
}
