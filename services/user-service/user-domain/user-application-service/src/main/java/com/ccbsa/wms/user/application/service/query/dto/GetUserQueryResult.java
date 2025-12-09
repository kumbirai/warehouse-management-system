package com.ccbsa.wms.user.application.service.query.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.user.domain.core.valueobject.UserStatus;

/**
 * Query Result DTO for user queries.
 */
public class GetUserQueryResult {
    private final UserId userId;
    private final TenantId tenantId;
    private final String username;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final UserStatus status;
    private final String keycloakUserId;
    private final List<String> roles;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastModifiedAt;

    public GetUserQueryResult(UserId userId, TenantId tenantId, String username, String email,
                              String firstName, String lastName, UserStatus status,
                              String keycloakUserId, List<String> roles,
                              LocalDateTime createdAt, LocalDateTime lastModifiedAt) {
        this.userId = Objects.requireNonNull(userId, "UserId is required");
        this.tenantId = Objects.requireNonNull(tenantId, "TenantId is required");
        this.username = Objects.requireNonNull(username, "Username is required");
        this.email = Objects.requireNonNull(email, "EmailAddress is required");
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = Objects.requireNonNull(status, "Status is required");
        this.keycloakUserId = keycloakUserId;
        this.roles = roles != null ? List.copyOf(roles) : List.of();
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt is required");
        this.lastModifiedAt = lastModifiedAt;
    }

    public UserId getUserId() {
        return userId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public String getUsername() {
        return username;
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

    public UserStatus getStatus() {
        return status;
    }

    public String getKeycloakUserId() {
        return keycloakUserId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }
}

