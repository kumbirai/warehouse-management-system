package com.ccbsa.wms.location.domain.core.event;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;

/**
 * Domain Event: LocationUnblockedEvent
 * <p>
 * Published when a location is unblocked.
 * <p>
 * This event indicates that:
 * - A location has been unblocked and can be used for stock assignment
 * - Location status has been updated (AVAILABLE or OCCUPIED based on current quantity)
 * - Used for tracking location status changes
 * - May trigger notifications or workflows
 */
public class LocationUnblockedEvent extends LocationManagementEvent {

    private final LocationId locationId;
    private final TenantId tenantId;
    private final UserId unblockedBy;
    private final LocationStatus newStatus;
    private final LocalDateTime unblockedAt;

    /**
     * Constructor for LocationUnblockedEvent.
     *
     * @param locationId  Location identifier
     * @param tenantId    Tenant identifier
     * @param unblockedBy User who unblocked the location
     * @param newStatus   New location status (AVAILABLE or OCCUPIED)
     * @param unblockedAt Unblock timestamp
     */
    public LocationUnblockedEvent(LocationId locationId, TenantId tenantId, UserId unblockedBy, LocationStatus newStatus, LocalDateTime unblockedAt) {
        super(locationId);
        this.locationId = locationId;
        this.tenantId = tenantId;
        this.unblockedBy = unblockedBy;
        this.newStatus = newStatus;
        this.unblockedAt = unblockedAt;
    }

    /**
     * Constructor for LocationUnblockedEvent with metadata.
     *
     * @param locationId  Location identifier
     * @param tenantId    Tenant identifier
     * @param unblockedBy User who unblocked the location
     * @param newStatus   New location status (AVAILABLE or OCCUPIED)
     * @param unblockedAt Unblock timestamp
     * @param metadata    Event metadata (correlation ID, user ID, etc.)
     */
    public LocationUnblockedEvent(LocationId locationId, TenantId tenantId, UserId unblockedBy, LocationStatus newStatus, LocalDateTime unblockedAt, EventMetadata metadata) {
        super(locationId, metadata);
        this.locationId = locationId;
        this.tenantId = tenantId;
        this.unblockedBy = unblockedBy;
        this.newStatus = newStatus;
        this.unblockedAt = unblockedAt;
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public UserId getUnblockedBy() {
        return unblockedBy;
    }

    public LocationStatus getNewStatus() {
        return newStatus;
    }

    public LocalDateTime getUnblockedAt() {
        return unblockedAt;
    }
}

