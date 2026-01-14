package com.ccbsa.wms.location.domain.core.event;

import java.util.Map;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.ReturnId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

/**
 * Domain Event: ReturnLocationAssignedEvent
 * <p>
 * Published when locations have been assigned to return line items.
 * <p>
 * This event indicates that:
 * - Return line items have been assigned to locations based on product condition
 * - Used for event-driven choreography with Returns Service
 */
public class ReturnLocationAssignedEvent extends LocationManagementEvent {

    private final ReturnId returnId;
    private final TenantId tenantId;
    /**
     * Map of return line item ID to assigned location ID.
     */
    private final Map<String, LocationId> lineItemAssignments;

    /**
     * Constructor for ReturnLocationAssignedEvent.
     *
     * @param returnId            Return identifier
     * @param tenantId            Tenant identifier
     * @param lineItemAssignments Map of line item ID to location ID
     * @param locationId          Primary location ID (for aggregate root)
     */
    public ReturnLocationAssignedEvent(ReturnId returnId, TenantId tenantId, Map<String, LocationId> lineItemAssignments, LocationId locationId) {
        super(locationId);
        this.returnId = returnId;
        this.tenantId = tenantId;
        this.lineItemAssignments = lineItemAssignments;
    }

    /**
     * Constructor for ReturnLocationAssignedEvent with metadata.
     *
     * @param returnId            Return identifier
     * @param tenantId            Tenant identifier
     * @param lineItemAssignments Map of line item ID to location ID
     * @param locationId          Primary location ID (for aggregate root)
     * @param metadata            Event metadata (correlation ID, user ID, etc.)
     */
    public ReturnLocationAssignedEvent(ReturnId returnId, TenantId tenantId, Map<String, LocationId> lineItemAssignments, LocationId locationId, EventMetadata metadata) {
        super(locationId, metadata);
        this.returnId = returnId;
        this.tenantId = tenantId;
        this.lineItemAssignments = lineItemAssignments;
    }

    public ReturnId getReturnId() {
        return returnId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public Map<String, LocationId> getLineItemAssignments() {
        return lineItemAssignments;
    }
}
