package com.ccbsa.wms.user.application.api.dto;

import java.util.List;

/**
 * DTO: LoginResponse
 * <p>
 * Response DTO for successful login containing tokens and user context.
 */
public final class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private int expiresIn;
    private UserContext userContext;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken,
                         String refreshToken,
                         String tokenType,
                         int expiresIn,
                         UserContext userContext) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.userContext = copyUserContext(userContext);
    }

    private static UserContext copyUserContext(UserContext source) {
        if (source == null) {
            return null;
        }
        UserContext copy = new UserContext();
        copy.setUserId(source.getUserId());
        copy.setUsername(source.getUsername());
        copy.setTenantId(source.getTenantId());
        copy.setRoles(source.getRoles());
        copy.setEmail(source.getEmail());
        copy.setFirstName(source.getFirstName());
        copy.setLastName(source.getLastName());
        return copy;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserContext getUserContext() {
        return copyUserContext(userContext);
    }

    public void setUserContext(UserContext userContext) {
        this.userContext = copyUserContext(userContext);
    }

    @Override
    public String toString() {
        return String.format("LoginResponse{tokenType='%s', expiresIn=%d, userContext=%s}",
                tokenType,
                expiresIn,
                userContext);
    }

    /**
     * User context information extracted from JWT token.
     */
    public static final class UserContext {
        private String userId;
        private String username;
        private String tenantId;
        private List<String> roles = List.of();
        private String email;
        private String firstName;
        private String lastName;

        public UserContext() {
        }

        public UserContext(String userId,
                           String username,
                           String tenantId,
                           List<String> roles,
                           String email,
                           String firstName,
                           String lastName) {
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
            return String.format("UserContext{userId='%s', username='%s', tenantId='%s', roles=%s, email='%s', firstName='%s', lastName='%s'}",
                    userId,
                    username,
                    tenantId,
                    roles,
                    email,
                    firstName,
                    lastName);
        }
    }
}

