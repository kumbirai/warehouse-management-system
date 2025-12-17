package com.ccbsa.common.keycloak.port;

import org.keycloak.representations.idm.GroupRepresentation;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;

/**
 * Port interface for Keycloak group operations. Used by tenant-service for tenant group management.
 * <p>
 * Groups can be used to organize users within a realm. In a single-realm multi-tenant approach, each tenant can have its own group.
 */
public interface KeycloakGroupPort {
    /**
     * Creates a group for a tenant.
     *
     * @param realmName Realm name where the group will be created
     * @param tenantId  Tenant identifier
     * @param groupName Group name
     * @return Created group representation
     * @throws IllegalArgumentException if realm does not exist or group name is invalid
     */
    GroupRepresentation createGroup(String realmName, TenantId tenantId, String groupName);

    /**
     * Gets a group by name.
     *
     * @param realmName Realm name
     * @param groupName Group name
     * @return Group representation or null if not found
     */
    GroupRepresentation getGroup(String realmName, String groupName);

    /**
     * Adds a user to a group.
     *
     * @param realmName Realm name
     * @param groupId   Group ID
     * @param userId    User ID
     * @throws IllegalArgumentException if realm, group, or user does not exist
     */
    void addUserToGroup(String realmName, String groupId, UserId userId);

    /**
     * Removes a user from a group.
     *
     * @param realmName Realm name
     * @param groupId   Group ID
     * @param userId    User ID
     * @throws IllegalArgumentException if realm, group, or user does not exist
     */
    void removeUserFromGroup(String realmName, String groupId, UserId userId);

    /**
     * Checks if a group exists.
     *
     * @param realmName Realm name
     * @param groupName Group name
     * @return true if group exists, false otherwise
     */
    boolean groupExists(String realmName, String groupName);
}

