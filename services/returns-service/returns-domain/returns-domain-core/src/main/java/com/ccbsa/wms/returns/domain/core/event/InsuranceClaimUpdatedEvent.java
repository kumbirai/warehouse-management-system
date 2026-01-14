package com.ccbsa.wms.returns.domain.core.event;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.returns.domain.core.entity.DamageAssessment;
import com.ccbsa.wms.returns.domain.core.valueobject.InsuranceClaimInfo;

/**
 * Domain Event: InsuranceClaimUpdatedEvent
 * <p>
 * Published when insurance claim information is updated for a damage assessment.
 * <p>
 * This event indicates that:
 * - Insurance claim information has been added or updated
 * - Claim details are available for processing
 */
public class InsuranceClaimUpdatedEvent extends ReturnsEvent<DamageAssessment> {
    private static final String AGGREGATE_TYPE = "DamageAssessment";

    private final OrderNumber orderNumber;
    private final TenantId tenantId;
    private final InsuranceClaimInfo insuranceClaimInfo;

    /**
     * Constructor for InsuranceClaimUpdatedEvent.
     *
     * @param aggregateId        Damage assessment ID (as String)
     * @param orderNumber        Order number
     * @param tenantId           Tenant identifier
     * @param insuranceClaimInfo Insurance claim information
     */
    public InsuranceClaimUpdatedEvent(String aggregateId, OrderNumber orderNumber, TenantId tenantId, InsuranceClaimInfo insuranceClaimInfo) {
        super(aggregateId, AGGREGATE_TYPE);
        this.orderNumber = orderNumber;
        this.tenantId = tenantId;
        this.insuranceClaimInfo = insuranceClaimInfo;
    }

    /**
     * Constructor for InsuranceClaimUpdatedEvent with metadata.
     *
     * @param aggregateId        Damage assessment ID (as String)
     * @param orderNumber        Order number
     * @param tenantId           Tenant identifier
     * @param insuranceClaimInfo Insurance claim information
     * @param metadata           Event metadata for traceability
     */
    public InsuranceClaimUpdatedEvent(String aggregateId, OrderNumber orderNumber, TenantId tenantId, InsuranceClaimInfo insuranceClaimInfo, EventMetadata metadata) {
        super(aggregateId, AGGREGATE_TYPE, metadata);
        this.orderNumber = orderNumber;
        this.tenantId = tenantId;
        this.insuranceClaimInfo = insuranceClaimInfo;
    }

    public OrderNumber getOrderNumber() {
        return orderNumber;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public InsuranceClaimInfo getInsuranceClaimInfo() {
        return insuranceClaimInfo;
    }
}
