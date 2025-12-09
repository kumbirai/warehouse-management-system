package com.ccbsa.common.keycloak.port;

import org.keycloak.representations.idm.RealmRepresentation;

import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Port interface for Keycloak realm operations.
 * Used by tenant-service for realm management.
 * <p>
 * Realms in Keycloak represent isolated security domains.
 * In a multi-tenant system, each tenant can have its own realm
 * or all tenants can share a single realm with tenant groups.
 */
public interface KeycloakRealmPort {
    /**
     * Creates a realm for a tenant.
     *
     * @param tenantId  Tenant identifier
     * @param realmName Realm name (must be unique)
     * @return Created realm representation
     * @throws IllegalArgumentException if realm name is invalid
     * @throws RuntimeException         if realm creation fails
     */
    RealmRepresentation createRealm(TenantId tenantId,
                                    String realmName);

    /**
     * Gets a realm by name.
     *
     * @param realmName Realm name
     * @return Realm representation or null if not found
     */
    RealmRepresentation getRealm(String realmName);

    /**
     * Updates realm configuration.
     *
     * @param realmName Realm name
     * @param realm     Realm representation with updates
     * @throws IllegalArgumentException if realm does not exist
     */
    void updateRealm(String realmName,
                     RealmRepresentation realm);

    /**
     * Enables a realm.
     *
     * @param realmName Realm name
     * @throws IllegalArgumentException if realm does not exist
     */
    void enableRealm(String realmName);

    /**
     * Disables a realm.
     *
     * @param realmName Realm name
     * @throws IllegalArgumentException if realm does not exist
     */
    void disableRealm(String realmName);

    /**
     * Checks if a realm exists.
     *
     * @param realmName Realm name
     * @return true if realm exists, false otherwise
     */
    boolean realmExists(String realmName);
}

