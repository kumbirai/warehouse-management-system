package com.ccbsa.wms.user.application.service.port.repository;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.user.domain.core.entity.User;
import com.ccbsa.wms.user.domain.core.valueobject.Username;

/**
 * Repository port for User aggregate persistence.
 * <p>
 * This port is defined in the application service layer and implemented by the data access layer (user-dataaccess).
 */
public interface UserRepository {
    /**
     * Saves a user aggregate.
     *
     * @param user User aggregate to save
     */
    void save(User user);

    /**
     * Finds a user by ID.
     *
     * @param userId User identifier
     * @return User if found, empty otherwise
     */
    Optional<User> findById(UserId userId);

    /**
     * Finds a user by tenant ID and user ID.
     *
     * @param tenantId Tenant identifier
     * @param userId   User identifier
     * @return User if found, empty otherwise
     */
    Optional<User> findByTenantIdAndId(TenantId tenantId, UserId userId);

    /**
     * Finds all users for a tenant.
     *
     * @param tenantId Tenant identifier
     * @return List of users for the tenant
     */
    List<User> findByTenantId(TenantId tenantId);

    /**
     * Finds all users (for SYSTEM_ADMIN only).
     *
     * @return List of all users
     */
    List<User> findAll();

    /**
     * Finds all users across all tenant schemas (for SYSTEM_ADMIN only).
     * <p>
     * This method queries users from all tenant schemas in the database. It is used when SYSTEM_ADMIN wants to list users from all tenants.
     *
     * @param status Optional user status filter. If null, returns all users regardless of status.
     * @return List of all users across all tenant schemas
     */
    List<User> findAllAcrossTenants(com.ccbsa.wms.user.domain.core.valueobject.UserStatus status);

    /**
     * Finds a user by ID across all tenant schemas (for SYSTEM_ADMIN only).
     * <p>
     * This method queries all tenant schemas to find a user by ID. It is used when SYSTEM_ADMIN needs to find a user without knowing which tenant they belong to.
     *
     * @param userId User identifier
     * @return User if found, empty otherwise
     */
    Optional<User> findByIdAcrossTenants(UserId userId);

    /**
     * Finds a user by username.
     *
     * @param username Username value object
     * @return User if found, empty otherwise
     */
    Optional<User> findByUsername(Username username);

    /**
     * Finds a user by tenant ID and username.
     *
     * @param tenantId Tenant identifier
     * @param username Username value object
     * @return User if found, empty otherwise
     */
    Optional<User> findByTenantIdAndUsername(TenantId tenantId, Username username);

    /**
     * Finds users by tenant ID and status.
     *
     * @param tenantId Tenant identifier
     * @param status   User status
     * @return List of users with the specified status
     */
    List<User> findByTenantIdAndStatus(TenantId tenantId, com.ccbsa.wms.user.domain.core.valueobject.UserStatus status);

    /**
     * Checks if a user exists by ID.
     *
     * @param userId User identifier
     * @return true if user exists
     */
    boolean existsById(UserId userId);

    /**
     * Checks if a user exists by tenant ID and user ID.
     *
     * @param tenantId Tenant identifier
     * @param userId   User identifier
     * @return true if user exists
     */
    boolean existsByTenantIdAndUserId(TenantId tenantId, UserId userId);

    /**
     * Deletes a user by ID.
     *
     * @param userId User identifier
     */
    void deleteById(UserId userId);

    /**
     * Finds a user by Keycloak user ID.
     *
     * @param keycloakUserId Keycloak user identifier
     * @return User if found, empty otherwise
     */
    Optional<User> findByKeycloakUserId(String keycloakUserId);
}

