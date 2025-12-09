package com.ccbsa.wms.user.application.service.query.dto;

import java.util.List;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;

/**
 * View: UserContextView
 * <p>
 * Read model for user context information.
 * Uses value objects for type-safe IDs.
 *
 * <p>Note: TenantId may be null for SYSTEM_ADMIN users who are not associated with a tenant.
 */
public final class UserContextView {
    private final UserId userId;
    private final String username;
    private final TenantId tenantId; // Nullable for SYSTEM_ADMIN users
    private final List<String> roles;
    private final String email;
    private final String firstName;
    private final String lastName;

    public UserContextView(UserId userId,
                           String username,
                           TenantId tenantId,
                           List<String> roles,
                           String email,
                           String firstName,
                           String lastName) {
        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
        // TenantId can be null for SYSTEM_ADMIN users
        this.userId = userId;
        this.username = username;
        this.tenantId = tenantId;
        this.roles = roles == null ? List.of() : List.copyOf(roles);
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public UserId getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    /**
     * Gets the tenant ID. May be null for SYSTEM_ADMIN users.
     *
     * @return Tenant ID, or null if user is SYSTEM_ADMIN
     */
    public TenantId getTenantId() {
        return tenantId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    @Override
    public String toString() {
        return String.format("UserContextView{userId='%s', username='%s', tenantId='%s', roles=%s, email='%s', firstName='%s', lastName='%s'}",
                userId.getValue(),
                username,
                tenantId != null ? tenantId.getValue() : "null",
                roles,
                email,
                firstName,
                lastName);
    }
}

