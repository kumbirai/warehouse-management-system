package com.ccbsa.wms.user.application.api.dto;

import java.util.List;

/**
 * DTO: UserContextResponse
 * <p>
 * Response DTO for user context information.
 */
public final class UserContextResponse {
    private String userId;
    private String username;
    private String tenantId;
    private List<String> roles = List.of();
    private String email;
    private String firstName;
    private String lastName;

    public UserContextResponse() {
    }

    public UserContextResponse(String userId, String username, String tenantId, List<String> roles, String email, String firstName, String lastName) {
        this.userId = userId;
        this.username = username;
        this.tenantId = tenantId;
        this.roles = roles == null ? List.of() : List.copyOf(roles);
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles == null ? List.of() : List.copyOf(roles);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public String toString() {
        return String.format("UserContextResponse{userId='%s', username='%s', tenantId='%s', roles=%s, email='%s', firstName='%s', lastName='%s'}", userId, username, tenantId,
                roles, email, firstName, lastName);
    }
}

