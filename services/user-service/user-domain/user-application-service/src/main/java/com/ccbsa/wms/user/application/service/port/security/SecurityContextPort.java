package com.ccbsa.wms.user.application.service.port.security;

import java.util.List;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;

/**
 * Port interface for extracting security context information.
 * <p>
 * Provides methods to extract current user ID, roles, and tenant ID from security context.
 */
public interface SecurityContextPort {
    /**
     * Gets the current user ID from the security context.
     *
     * @return Current user ID
     * @throws IllegalStateException if user is not authenticated
     */
    UserId getCurrentUserId();

    /**
     * Gets the current user's roles from the security context.
     *
     * @return List of role names
     */
    List<String> getCurrentUserRoles();

    /**
     * Gets the current user's tenant ID from TenantContext.
     *
     * @return Current tenant ID, or null if not set
     */
    TenantId getCurrentUserTenantId();
}

