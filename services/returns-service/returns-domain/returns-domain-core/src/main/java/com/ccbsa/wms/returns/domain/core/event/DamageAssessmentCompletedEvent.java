package com.ccbsa.wms.returns.domain.core.event;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.returns.domain.core.entity.DamageAssessment;

/**
 * Domain Event: DamageAssessmentCompletedEvent
 * <p>
 * Published when a damage assessment is completed.
 * <p>
 * This event indicates that:
 * - Damage assessment has been completed
 * - Assessment is in COMPLETED status
 * - Locations have been assigned
 */
public class DamageAssessmentCompletedEvent extends ReturnsEvent<DamageAssessment> {
    private static final String AGGREGATE_TYPE = "DamageAssessment";

    private final OrderNumber orderNumber;
    private final TenantId tenantId;

    /**
     * Constructor for DamageAssessmentCompletedEvent.
     *
     * @param aggregateId Damage assessment ID (as String)
     * @param orderNumber Order number
     * @param tenantId    Tenant identifier
     */
    public DamageAssessmentCompletedEvent(String aggregateId, OrderNumber orderNumber, TenantId tenantId) {
        super(aggregateId, AGGREGATE_TYPE);
        this.orderNumber = orderNumber;
        this.tenantId = tenantId;
    }

    /**
     * Constructor for DamageAssessmentCompletedEvent with metadata.
     *
     * @param aggregateId Damage assessment ID (as String)
     * @param orderNumber Order number
     * @param tenantId    Tenant identifier
     * @param metadata    Event metadata for traceability
     */
    public DamageAssessmentCompletedEvent(String aggregateId, OrderNumber orderNumber, TenantId tenantId, EventMetadata metadata) {
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
