package com.ccbsa.common.keycloak.port;

import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Port interface for querying Tenant Service to determine Keycloak realm information.
 * <p>
 * This port is used by user-service to: 1. Validate tenant exists and is ACTIVE 2. Get the Keycloak realm name for a tenant 3. Determine realm strategy (single realm vs per-tenant
 * realms)
 * <p>
 * The implementation should query the tenant-service REST API.
 */
public interface TenantServicePort {
    /**
     * Gets the Keycloak realm name for a tenant.
     * <p>
     * In a per-tenant realm strategy, each tenant has its own realm. In a single-realm strategy, all tenants share the default realm.
     *
     * @param tenantId Tenant identifier
     * @return Realm name if tenant has a specific realm configured, or empty if tenant uses the default realm
     * @throws RuntimeException if tenant service is unavailable
     */
    Optional<String> getTenantRealmName(TenantId tenantId);

    /**
     * Checks if a tenant exists and is ACTIVE.
     *
     * @param tenantId Tenant identifier
     * @return true if tenant exists and is ACTIVE, false otherwise
     * @throws RuntimeException if tenant service is unavailable
     */
    boolean isTenantActive(TenantId tenantId);

    /**
     * Gets tenant information including status.
     *
     * @param tenantId Tenant identifier
     * @return Tenant information or empty if tenant does not exist
     * @throws RuntimeException if tenant service is unavailable
     */
    Optional<TenantInfo> getTenantInfo(TenantId tenantId);

    /**
     * Tenant information DTO.
     */
    record TenantInfo(TenantId tenantId, String name, TenantStatus status, String keycloakRealmName) {
        public enum TenantStatus {
            PENDING,
            ACTIVE,
            INACTIVE,
            SUSPENDED
        }
    }
}

