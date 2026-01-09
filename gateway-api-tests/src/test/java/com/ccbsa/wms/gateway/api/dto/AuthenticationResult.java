package com.ccbsa.wms.gateway.api.dto;

import org.springframework.http.ResponseCookie;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResult {
    private String accessToken;
    private ResponseCookie refreshTokenCookie;
    private UserContext userContext;
    private Long expiresIn;

    /**
     * Get tenant ID from user context.
     *
     * @return tenant ID
     */
    public String getTenantId() {
        return userContext != null ? userContext.getTenantId() : null;
    }

    /**
     * Check if user has specific role.
     *
     * @param role the role to check
     * @return true if user has role
     */
    public boolean hasRole(String role) {
        return userContext != null && userContext.getRoles() != null && userContext.getRoles().contains(role);
    }
}

