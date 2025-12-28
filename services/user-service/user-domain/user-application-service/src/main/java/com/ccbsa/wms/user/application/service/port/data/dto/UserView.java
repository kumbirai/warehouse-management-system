package com.ccbsa.wms.user.application.service.port.data.dto;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.user.domain.core.valueobject.UserStatus;

/**
 * Read Model DTO: UserView
 * <p>
 * Optimized read model representation of User aggregate for query operations.
 * <p>
 * This is a denormalized view optimized for read queries, separate from the write model (User aggregate).
 * <p>
 * Fields are flattened and optimized for query performance.
 */
public final class UserView {
    private final UserId userId;
    private final TenantId tenantId;
    private final String username;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final UserStatus status;
    private final String keycloakUserId;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastModifiedAt;

    private UserView(Builder builder) {
        this.userId = builder.userId;
        this.tenantId = builder.tenantId;
        this.username = builder.username;
        this.email = builder.email;
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.status = builder.status;
        this.keycloakUserId = builder.keycloakUserId;
        this.createdAt = builder.createdAt;
        this.lastModifiedAt = builder.lastModifiedAt;
    }

    public static Builder builder() {
        return new Builder();
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public static class Builder {
        private UserId userId;
        private TenantId tenantId;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private UserStatus status;
        private String keycloakUserId;
        private LocalDateTime createdAt;
        private LocalDateTime lastModifiedAt;

        public Builder userId(UserId userId) {
            this.userId = userId;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder status(UserStatus status) {
            this.status = status;
            return this;
        }

        public Builder keycloakUserId(String keycloakUserId) {
            this.keycloakUserId = keycloakUserId;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder lastModifiedAt(LocalDateTime lastModifiedAt) {
            this.lastModifiedAt = lastModifiedAt;
            return this;
        }

        public UserView build() {
            if (userId == null) {
                throw new IllegalArgumentException("UserId is required");
            }
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (username == null) {
                throw new IllegalArgumentException("Username is required");
            }
            if (email == null) {
                throw new IllegalArgumentException("Email is required");
            }
            if (status == null) {
                throw new IllegalArgumentException("Status is required");
            }
            return new UserView(this);
        }
    }
}

