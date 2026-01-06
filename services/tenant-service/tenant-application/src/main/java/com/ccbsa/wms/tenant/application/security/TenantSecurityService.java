package com.ccbsa.wms.tenant.application.security;

import org.springframework.stereotype.Service;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.security.TenantContext;

import lombok.extern.slf4j.Slf4j;

/**
 * Security service for tenant validation in method security expressions.
 * <p>
 * Used by @PreAuthorize annotations to check if the current user belongs to a specific tenant.
 * Uses TenantContext which is set by TenantContextInterceptor from the X-Tenant-Id header.
 */
@Slf4j
@Service("tenantSecurityService")
public class TenantSecurityService {

    /**
     * Checks if the current authenticated user belongs to the specified tenant.
     * <p>
     * This method is used in Spring Security method security expressions:
     * <pre>
     * @PreAuthorize("hasRole('USER') and @tenantSecurityService.isUserTenant(#id)")
     * </pre>
     * <p>
     * The tenant ID is extracted from TenantContext, which is set by TenantContextInterceptor
     * from the X-Tenant-Id header (injected by the gateway from the JWT token).
     *
     * @param tenantId The tenant ID to check (from path variable)
     * @return true if the current user's tenant ID matches the requested tenant ID, false otherwise
     */
    public boolean isUserTenant(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            log.debug("Tenant ID is null or empty");
            return false;
        }

        try {
            TenantId currentTenantId = TenantContext.getTenantId();
            if (currentTenantId == null) {
                log.debug("Tenant context is not set (user may be SYSTEM_ADMIN or tenant context not extracted)");
                return false;
            }

            boolean matches = currentTenantId.getValue().equals(tenantId);

            log.debug("Tenant ID check: currentTenantId={}, requestedTenantId={}, matches={}", currentTenantId.getValue(), tenantId, matches);

            return matches;
        } catch (Exception e) {
            log.error("Error checking tenant membership: {}", e.getMessage(), e);
            return false;
        }
    }
}

