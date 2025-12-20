package com.ccbsa.wms.user.application.api.security;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.user.application.service.port.security.SecurityContextPort;

/**
 * Service for extracting security context information from Spring Security.
 * <p>
 * Provides methods to extract current user ID, roles, and tenant ID from JWT tokens.
 * Implements SecurityContextPort interface for dependency injection.
 */
@Service
public class SecurityContextService implements SecurityContextPort {
    private static final Logger logger = LoggerFactory.getLogger(SecurityContextService.class);

    /**
     * Gets the current user ID from the security context.
     *
     * @return Current user ID
     * @throws IllegalStateException if user is not authenticated
     */
    public UserId getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new IllegalStateException("User is not authenticated");
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getSubject();
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalStateException("JWT token does not contain subject (user ID)");
        }

        return UserId.of(userId);
    }

    /**
     * Gets the current user's tenant ID from TenantContext.
     *
     * @return Current tenant ID, or null if not set
     */
    public TenantId getCurrentUserTenantId() {
        return TenantContext.getTenantId();
    }

    /**
     * Checks if the current user has any of the specified roles.
     *
     * @param roles Role names to check
     * @return true if user has any of the roles
     */
    public boolean hasAnyRole(String... roles) {
        List<String> userRoles = getCurrentUserRoles();
        for (String role : roles) {
            if (userRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the current user's roles from the JWT token.
     *
     * @return List of role names
     */
    public List<String> getCurrentUserRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            logger.warn("No authentication found in security context");
            return Collections.emptyList();
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        return extractRolesFromJwt(jwt);
    }

    /**
     * Extracts roles from JWT token.
     * <p>
     * Roles are expected in the format:
     * {
     * "realm_access": {
     * "roles": ["USER", "PICKER", ...]
     * }
     * }
     *
     * @param jwt JWT token
     * @return List of role names
     */
    private List<String> extractRolesFromJwt(Jwt jwt) {
        try {
            Object realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof Map) {
                @SuppressWarnings("unchecked") Map<String, Object> realmAccessMap = (Map<String, Object>) realmAccess;
                Object rolesObj = realmAccessMap.get("roles");
                if (rolesObj instanceof List) {
                    @SuppressWarnings("unchecked") List<String> roles = (List<String>) rolesObj;
                    return roles.stream().filter(role -> role != null && !role.trim().isEmpty()).collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract roles from JWT token: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Checks if the current user is SYSTEM_ADMIN.
     *
     * @return true if user is SYSTEM_ADMIN
     */
    public boolean isSystemAdmin() {
        return hasRole("SYSTEM_ADMIN");
    }

    /**
     * Checks if the current user has a specific role.
     *
     * @param role Role name to check
     * @return true if user has the role
     */
    public boolean hasRole(String role) {
        List<String> roles = getCurrentUserRoles();
        return roles.contains(role);
    }

    /**
     * Checks if the current user is TENANT_ADMIN.
     *
     * @return true if user is TENANT_ADMIN
     */
    public boolean isTenantAdmin() {
        return hasRole("TENANT_ADMIN");
    }
}

