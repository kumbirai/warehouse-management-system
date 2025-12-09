package com.ccbsa.wms.tenant.application.service.port.service;

import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Port interface for tenant-specific Keycloak group orchestration.
 * <p>
 * Abstracts group creation/enabling/disabling regardless of the underlying Keycloak topology.
 */
public interface TenantGroupServicePort {
    /**
     * Ensures that the tenant group exists and is marked as enabled/active.
     *
     * @param tenantId Tenant identifier
     */
    void ensureTenantGroupEnabled(TenantId tenantId);

    /**
     * Marks the tenant group as disabled (used during deactivate/suspend flows).
     *
     * @param tenantId Tenant identifier
     */
    void disableTenantGroup(TenantId tenantId);
}

