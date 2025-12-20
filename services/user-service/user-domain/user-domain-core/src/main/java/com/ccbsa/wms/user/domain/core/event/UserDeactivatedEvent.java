package com.ccbsa.wms.user.domain.core.event;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;

/**
 * Domain Event: UserDeactivatedEvent
 * <p>
 * Published when a user is deactivated.
 * <p>
 * Event Version: 1.0
 */
public final class UserDeactivatedEvent extends UserEvent {

    /**
     * Constructor for UserDeactivatedEvent without metadata.
     *
     * @param userId   User identifier
     * @param tenantId Tenant identifier
     */
    public UserDeactivatedEvent(UserId userId, TenantId tenantId) {
        super(userId, tenantId);
    }

    /**
     * Constructor for UserDeactivatedEvent with metadata.
     *
     * @param userId   User identifier
     * @param tenantId Tenant identifier
     * @param metadata Event metadata for traceability
     */
    public UserDeactivatedEvent(UserId userId, TenantId tenantId, EventMetadata metadata) {
        super(userId, tenantId, metadata);
    }
}
