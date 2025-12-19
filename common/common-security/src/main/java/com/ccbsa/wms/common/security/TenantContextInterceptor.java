package com.ccbsa.wms.common.security;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor that extracts tenant context from HTTP headers and sets it in ThreadLocal.
 * <p>
 * Extracts: - X-Tenant-Id header -> TenantContext.tenantId - X-User-Id header -> TenantContext.userId
 * <p>
 * Validates that tenant ID is present for all requests.
 */
@Component
public class TenantContextInterceptor implements HandlerInterceptor {
    private static final String X_TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String X_USER_ID_HEADER = "X-User-Id";
    private static final String X_ROLE_HEADER = "X-Role";
    private static final String SYSTEM_ADMIN_ROLE = "SYSTEM_ADMIN";

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {

        // Check if user has SYSTEM_ADMIN role
        boolean isSystemAdmin = isSystemAdmin(request);

        // SYSTEM_ADMIN users don't need tenant_id for any endpoint
        if (isSystemAdmin) {
            // SYSTEM_ADMIN users don't need tenant_id
            String userIdValue = request.getHeader(X_USER_ID_HEADER);
            if (userIdValue != null && !userIdValue.trim()
                    .isEmpty()) {
                TenantContext.setUserId(UserId.of(userIdValue));
            }
            // Don't set tenant context for SYSTEM_ADMIN
            return true;
        }

        // For non-SYSTEM_ADMIN users, tenant_id is required
        String tenantIdValue = request.getHeader(X_TENANT_ID_HEADER);
        if (tenantIdValue == null || tenantIdValue.trim()
                .isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            try {
                response.getWriter()
                        .write("{\"error\":{\"code\":\"MISSING_TENANT_ID\",\"message\":\"X-Tenant-Id header is required\"}}");
            } catch (IOException e) {
                // Ignore
            }
            return false;
        }

        // Extract user ID from header
        String userIdValue = request.getHeader(X_USER_ID_HEADER);

        // Set tenant context
        TenantContext.setTenantId(TenantId.of(tenantIdValue));
        if (userIdValue != null && !userIdValue.trim()
                .isEmpty()) {
            TenantContext.setUserId(UserId.of(userIdValue));
        }

        return true;
    }

    /**
     * Checks if the request is from a SYSTEM_ADMIN user. Checks both the X-Role header and the JWT token in SecurityContext.
     *
     * @param request The HTTP request
     * @return true if the user has SYSTEM_ADMIN role, false otherwise
     */
    private boolean isSystemAdmin(HttpServletRequest request) {
        return hasRole(request, SYSTEM_ADMIN_ROLE);
    }

    /**
     * Checks if the request is from a user with a specific role. Checks both the X-Role header and the JWT token in SecurityContext.
     *
     * @param request The HTTP request
     * @param role    The role to check for
     * @return true if the user has the role, false otherwise
     */
    private boolean hasRole(HttpServletRequest request, String role) {
        // Check X-Role header first (set by gateway)
        String rolesHeader = request.getHeader(X_ROLE_HEADER);
        if (rolesHeader != null && !rolesHeader.trim().isEmpty()) {
            // Split comma-separated roles and check for exact match
            String[] roles = rolesHeader.split(",");
            for (String r : roles) {
                if (r.trim().equals(role)) {
                    return true;
                }
            }
        }

        // Check JWT token in SecurityContext as fallback
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            Object realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> realmAccessMap = (Map<String, Object>) realmAccess;
                Object rolesObj = realmAccessMap.get("roles");
                if (rolesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) rolesObj;
                    return roles.contains(role);
                }
            }
        }

        return false;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            @Nullable Exception ex) {
        // Clear tenant context to prevent memory leaks
        TenantContext.clear();
    }
}

