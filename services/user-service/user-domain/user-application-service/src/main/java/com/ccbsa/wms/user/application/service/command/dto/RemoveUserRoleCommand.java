package com.ccbsa.wms.user.application.service.command.dto;

import java.util.Objects;

import com.ccbsa.common.domain.valueobject.UserId;

/**
 * Command DTO for removing a role from a user.
 */
public class RemoveUserRoleCommand {
    private final UserId userId;
    private final String roleName;

    public RemoveUserRoleCommand(UserId userId, String roleName) {
        this.userId = Objects.requireNonNull(userId, "UserId is required");
        this.roleName = Objects.requireNonNull(roleName, "RoleName is required");
    }

    public UserId getUserId() {
        return userId;
    }

    public String getRoleName() {
        return roleName;
    }
}

