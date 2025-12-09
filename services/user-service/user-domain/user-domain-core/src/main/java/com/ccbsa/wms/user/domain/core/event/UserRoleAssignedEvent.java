package com.ccbsa.wms.user.domain.core.event;

import java.util.Objects;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Domain Event: UserRoleAssignedEvent
 * <p>
 * Published when a role is assigned to a user.
 */
public class UserRoleAssignedEvent extends UserEvent {
    private final String roleName;

    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW",
            justification = "User events must fail fast if required fields are null")
    public UserRoleAssignedEvent(UserId userId, TenantId tenantId, String roleName) {
        super(userId, tenantId);
        this.roleName = Objects.requireNonNull(roleName, "RoleName cannot be null");
    }

    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW",
            justification = "User events must fail fast if required fields are null")
    public UserRoleAssignedEvent(UserId userId, TenantId tenantId, String roleName, EventMetadata metadata) {
        super(userId, tenantId, metadata);
        this.roleName = Objects.requireNonNull(roleName, "RoleName cannot be null");
    }

    @SuppressFBWarnings(value = "UUF_UNUSED_FIELD",
            justification = "Field is accessed via getRoleName() getter method")
    public String getRoleName() {
        return roleName;
    }
}

