package com.ccbsa.wms.location.domain.core.event;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

/**
 * Domain Event: LocationBlockedEvent
 * <p>
 * Published when a location is blocked.
 * <p>
 * This event indicates that:
 * - A location has been blocked and cannot be used for stock assignment
 * - Used for tracking location status changes
 * - May trigger notifications or workflows
 */
public class LocationBlockedEvent extends LocationManagementEvent {

    private final LocationId locationId;
    private final TenantId tenantId;
    private final UserId blockedBy;
    private final String reason;
    private final LocalDateTime blockedAt;

    /**
     * Constructor for LocationBlockedEvent.
     *
     * @param locationId Location identifier
     * @param tenantId   Tenant identifier
     * @param blockedBy  User who blocked the location
     * @param reason     Reason for blocking
     * @param blockedAt  Block timestamp
     */
    public LocationBlockedEvent(LocationId locationId, TenantId tenantId, UserId blockedBy, String reason, LocalDateTime blockedAt) {
        super(locationId);
        this.locationId = locationId;
        this.tenantId = tenantId;
        this.blockedBy = blockedBy;
        this.reason = reason;
        this.blockedAt = blockedAt;
    }

    /**
     * Constructor for LocationBlockedEvent with metadata.
     *
     * @param locationId Location identifier
     * @param tenantId   Tenant identifier
     * @param blockedBy  User who blocked the location
     * @param reason     Reason for blocking
     * @param blockedAt  Block timestamp
     * @param metadata   Event metadata (correlation ID, user ID, etc.)
     */
    public LocationBlockedEvent(LocationId locationId, TenantId tenantId, UserId blockedBy, String reason, LocalDateTime blockedAt, EventMetadata metadata) {
        super(locationId, metadata);
        this.locationId = locationId;
        this.tenantId = tenantId;
        this.blockedBy = blockedBy;
        this.reason = reason;
        this.blockedAt = blockedAt;
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public UserId getBlockedBy() {
        return blockedBy;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getBlockedAt() {
        return blockedAt;
    }
}

