package com.ccbsa.wms.user.domain.core.event;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.Description;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.user.domain.core.valueobject.UserStatus;

/**
 * Domain Event: UserUpdatedEvent
 * <p>
 * Published when a user is updated (profile, status, etc.).
 * <p>
 * Event Version: 1.0
 */
public final class UserUpdatedEvent extends UserEvent {
    private final UserStatus status;
    private final Description description;

    /**
     * Constructor for UserUpdatedEvent without metadata.
     *
     * @param userId      User identifier
     * @param tenantId    Tenant identifier
     * @param status      Current user status
     * @param description Description of the update
     * @throws IllegalArgumentException if status or description is null
     */
    public UserUpdatedEvent(UserId userId, TenantId tenantId, UserStatus status, Description description) {
        super(userId, tenantId);
        if (status == null) {
            throw new IllegalArgumentException("User status cannot be null");
        }
        if (description == null) {
            throw new IllegalArgumentException("Description cannot be null");
        }
        this.status = status;
        this.description = description;
    }

    /**
     * Constructor for UserUpdatedEvent with metadata.
     *
     * @param userId      User identifier
     * @param tenantId    Tenant identifier
     * @param status      Current user status
     * @param description Description of the update
     * @param metadata    Event metadata for traceability
     * @throws IllegalArgumentException if status or description is null
     */
    public UserUpdatedEvent(UserId userId, TenantId tenantId, UserStatus status, Description description, EventMetadata metadata) {
        super(userId, tenantId, metadata);
        if (status == null) {
            throw new IllegalArgumentException("User status cannot be null");
        }
        if (description == null) {
            throw new IllegalArgumentException("Description cannot be null");
        }
        this.status = status;
        this.description = description;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Description getDescription() {
        return description;
    }
}

