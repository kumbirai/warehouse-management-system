package com.ccbsa.wms.user.application.service.command.dto;

import java.util.Objects;

import com.ccbsa.common.domain.valueobject.UserId;

/**
 * Command DTO for assigning a role to a user.
 */
public class AssignUserRoleCommand {
    private final UserId userId;
    private final String roleName;

    public AssignUserRoleCommand(UserId userId, String roleName) {
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

