package com.ccbsa.wms.user.domain.core.event;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.EmailAddress;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.user.domain.core.valueobject.UserStatus;
import com.ccbsa.wms.user.domain.core.valueobject.Username;

/**
 * Domain Event: UserCreatedEvent
 * <p>
 * Published when a new user is created.
 * <p>
 * Event Version: 1.0
 */
public final class UserCreatedEvent extends UserEvent {
    private final Username username;
    private final EmailAddress emailAddress;
    private final UserStatus status;

    /**
     * Constructor for UserCreatedEvent without metadata.
     *
     * @param userId       User identifier
     * @param tenantId     Tenant identifier
     * @param username     Username
     * @param emailAddress EmailAddress address
     * @param status       User status
     */
    public UserCreatedEvent(UserId userId, TenantId tenantId, Username username, EmailAddress emailAddress, UserStatus status) {
        super(userId, tenantId);
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }
        if (emailAddress == null) {
            throw new IllegalArgumentException("EmailAddress cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("User status cannot be null");
        }
        this.username = username;
        this.emailAddress = emailAddress;
        this.status = status;
    }

    /**
     * Constructor for UserCreatedEvent with metadata.
     *
     * @param userId       User identifier
     * @param tenantId     Tenant identifier
     * @param username     Username
     * @param emailAddress EmailAddress address
     * @param status       User status
     * @param metadata     Event metadata for traceability
     */
    public UserCreatedEvent(UserId userId, TenantId tenantId, Username username, EmailAddress emailAddress, UserStatus status, EventMetadata metadata) {
        super(userId, tenantId, metadata);
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }
        if (emailAddress == null) {
            throw new IllegalArgumentException("EmailAddress cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("User status cannot be null");
        }
        this.username = username;
        this.emailAddress = emailAddress;
        this.status = status;
    }

    public Username getUsername() {
        return username;
    }

    public EmailAddress getEmail() {
        return emailAddress;
    }

    public UserStatus getStatus() {
        return status;
    }
}

