package com.ccbsa.wms.user.application.service.command.dto;

import java.util.List;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;

/**
 * Result: AuthenticationResult
 * <p>
 * Result of authentication operation containing tokens and user context.
 */
public final class AuthenticationResult {
    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final int expiresIn;
    private final UserContext userContext;

    public AuthenticationResult(String accessToken,
                                String refreshToken,
                                String tokenType,
                                int expiresIn,
                                UserContext userContext) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.userContext = userContext;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public UserContext getUserContext() {
        return userContext;
    }

    @Override
    public String toString() {
        return String.format("AuthenticationResult{tokenType='%s', expiresIn=%d, userContext=%s}",
                tokenType,
                expiresIn,
                userContext);
    }

    /**
     * User context information extracted from JWT token.
     * Uses value objects for type-safe IDs.
     *
     * <p>Note: TenantId may be null for SYSTEM_ADMIN users who are not associated with a tenant.
     */
    public static final class UserContext {
        private final UserId userId;
        private final String username;
        private final TenantId tenantId; // Nullable for SYSTEM_ADMIN users
        private final List<String> roles;
        private final String email;
        private final String firstName;
        private final String lastName;

        public UserContext(UserId userId,
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
            return String.format("UserContext{userId='%s', username='%s', tenantId='%s', roles=%s, email='%s', firstName='%s', lastName='%s'}",
                    userId.getValue(),
                    username,
                    tenantId != null ? tenantId.getValue() : "null",
                    roles,
                    email,
                    firstName,
                    lastName);
        }
    }
}

