package com.ccbsa.wms.user.application.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for assigning a role to a user.
 */
public class AssignRoleRequest {
    @NotBlank(message = "RoleId is required")
    private String roleId;

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }
}

