package com.ccbsa.wms.returns.domain.core.event;

import java.util.List;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.returns.domain.core.entity.Return;
import com.ccbsa.wms.returns.domain.core.entity.ReturnLineItem;
import com.ccbsa.wms.returns.domain.core.valueobject.ReturnType;

/**
 * Domain Event: ReturnInitiatedEvent
 * <p>
 * Published when a return is initiated (partial or full order return).
 * <p>
 * This event indicates that:
 * - A new return has been created
 * - Return is in INITIATED status
 * - Line items have been recorded
 */
public class ReturnInitiatedEvent extends ReturnsEvent<Return> {
    private static final String AGGREGATE_TYPE = "Return";

    private final OrderNumber orderNumber;
    private final TenantId tenantId;
    private final ReturnType returnType;
    private final List<ReturnLineItem> lineItems;

    /**
     * Constructor for ReturnInitiatedEvent.
     *
     * @param aggregateId Return ID (as String)
     * @param orderNumber Order number
     * @param tenantId    Tenant identifier
     * @param returnType  Return type (PARTIAL, FULL)
     * @param lineItems   List of return line items
     */
    public ReturnInitiatedEvent(String aggregateId, OrderNumber orderNumber, TenantId tenantId, ReturnType returnType, List<ReturnLineItem> lineItems) {
        super(aggregateId, AGGREGATE_TYPE);
        this.orderNumber = orderNumber;
        this.tenantId = tenantId;
        this.returnType = returnType;
        this.lineItems = lineItems != null ? List.copyOf(lineItems) : List.of();
    }

    /**
     * Constructor for ReturnInitiatedEvent with metadata.
     *
     * @param aggregateId Return ID (as String)
     * @param orderNumber Order number
     * @param tenantId    Tenant identifier
     * @param returnType  Return type (PARTIAL, FULL)
     * @param lineItems   List of return line items
     * @param metadata    Event metadata for traceability
     */
    public ReturnInitiatedEvent(String aggregateId, OrderNumber orderNumber, TenantId tenantId, ReturnType returnType, List<ReturnLineItem> lineItems, EventMetadata metadata) {
        super(aggregateId, AGGREGATE_TYPE, metadata);
        this.orderNumber = orderNumber;
        this.tenantId = tenantId;
        this.returnType = returnType;
        this.lineItems = lineItems != null ? List.copyOf(lineItems) : List.of();
    }

    public OrderNumber getOrderNumber() {
        return orderNumber;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public ReturnType getReturnType() {
        return returnType;
    }

    public List<ReturnLineItem> getLineItems() {
        return lineItems;
    }
}
