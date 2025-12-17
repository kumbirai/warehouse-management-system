package com.ccbsa.common.keycloak.port;

import java.util.List;
import java.util.Optional;

import org.keycloak.representations.idm.UserRepresentation;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;

/**
 * Port interface for Keycloak user operations. Used by user-service for user management.
 * <p>
 * The realm name is determined by the calling service based on tenant configuration. The service should query Tenant Service to get the appropriate realm name.
 */
public interface KeycloakUserPort {
    /**
     * Creates a user in Keycloak.
     *
     * @param realmName Realm name where the user will be created. This should be determined by querying Tenant Service or using the default realm from configuration.
     * @param user      User representation with user details
     * @return Created user ID
     * @throws IllegalArgumentException if realm does not exist or user data is invalid
     * @throws RuntimeException         if user creation fails
     */
    UserId createUser(String realmName, UserRepresentation user);

    /**
     * Gets a user by ID.
     *
     * @param realmName Realm name
     * @param userId    User ID
     * @return User representation or empty if not found
     */
    Optional<UserRepresentation> getUser(String realmName, UserId userId);

    /**
     * Gets a user by username.
     *
     * @param realmName Realm name
     * @param username  Username
     * @return User representation or empty if not found
     */
    Optional<UserRepresentation> getUserByUsername(String realmName, String username);

    /**
     * Updates a user.
     *
     * @param realmName Realm name
     * @param userId    User ID
     * @param user      User representation with updates
     * @throws IllegalArgumentException if realm or user does not exist
     */
    void updateUser(String realmName, UserId userId, UserRepresentation user);

    /**
     * Deletes a user.
     *
     * @param realmName Realm name
     * @param userId    User ID
     * @throws IllegalArgumentException if realm or user does not exist
     */
    void deleteUser(String realmName, UserId userId);

    /**
     * Sets user password.
     *
     * @param realmName Realm name
     * @param userId    User ID
     * @param password  Password
     * @param temporary Whether password is temporary (user must change on first login)
     * @throws IllegalArgumentException if realm or user does not exist
     */
    void setPassword(String realmName, UserId userId, String password, boolean temporary);

    /**
     * Assigns a role to a user.
     *
     * @param realmName Realm name
     * @param userId    User ID
     * @param roleName  Role name
     * @throws IllegalArgumentException if realm, user, or role does not exist
     */
    void assignRole(String realmName, UserId userId, String roleName);

    /**
     * Removes a role from a user.
     *
     * @param realmName Realm name
     * @param userId    User ID
     * @param roleName  Role name
     * @throws IllegalArgumentException if realm, user, or role does not exist
     */
    void removeRole(String realmName, UserId userId, String roleName);

    /**
     * Sets user attribute (e.g., tenant_id).
     *
     * @param realmName      Realm name
     * @param userId         User ID
     * @param attributeName  Attribute name
     * @param attributeValue Attribute value
     * @throws IllegalArgumentException if realm or user does not exist
     */
    void setUserAttribute(String realmName, UserId userId, String attributeName, String attributeValue);

    /**
     * Finds users by tenant ID attribute.
     *
     * @param realmName Realm name
     * @param tenantId  Tenant ID
     * @return List of user representations with matching tenant_id attribute
     */
    List<UserRepresentation> findUsersByTenantId(String realmName, TenantId tenantId);
}

