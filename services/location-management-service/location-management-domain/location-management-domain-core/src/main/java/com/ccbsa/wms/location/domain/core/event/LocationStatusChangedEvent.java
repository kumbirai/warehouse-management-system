package com.ccbsa.wms.location.domain.core.event;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;

/**
 * Domain Event: LocationStatusChangedEvent
 * <p>
 * Published when location status changes.
 * <p>
 * This event indicates that:
 * - Location status has changed (e.g., AVAILABLE -> OCCUPIED)
 * - Used for tracking location state changes
 * - May trigger notifications or workflows
 */
public class LocationStatusChangedEvent extends LocationManagementEvent {

    private final LocationId locationId;
    private final TenantId tenantId;
    private final LocationStatus oldStatus;
    private final LocationStatus newStatus;

    /**
     * Constructor for LocationStatusChangedEvent.
     *
     * @param locationId Location identifier
     * @param tenantId   Tenant identifier
     * @param oldStatus  Previous status
     * @param newStatus  New status
     */
    public LocationStatusChangedEvent(LocationId locationId, TenantId tenantId, LocationStatus oldStatus, LocationStatus newStatus) {
        super(locationId);
        this.locationId = locationId;
        this.tenantId = tenantId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    /**
     * Constructor for LocationStatusChangedEvent with metadata.
     *
     * @param locationId Location identifier
     * @param tenantId   Tenant identifier
     * @param oldStatus  Previous status
     * @param newStatus  New status
     * @param metadata   Event metadata (correlation ID, user ID, etc.)
     */
    public LocationStatusChangedEvent(LocationId locationId, TenantId tenantId, LocationStatus oldStatus, LocationStatus newStatus, EventMetadata metadata) {
        super(locationId, metadata);
        this.locationId = locationId;
        this.tenantId = tenantId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public LocationStatus getOldStatus() {
        return oldStatus;
    }

    public LocationStatus getNewStatus() {
        return newStatus;
    }
}

