package com.ccbsa.wms.user.dataaccess.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ccbsa.wms.user.dataaccess.entity.UserEntity;
import com.ccbsa.wms.user.domain.core.valueobject.UserStatus;

/**
 * JPA Repository: UserJpaRepository
 * <p>
 * Spring Data JPA repository for UserEntity. Provides standard CRUD operations and custom query methods.
 */
@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, String> {
    /**
     * Finds a user by tenant ID and user ID.
     *
     * @param tenantId Tenant identifier
     * @param userId   User identifier
     * @return User if found, empty otherwise
     */
    Optional<UserEntity> findByTenantIdAndUserId(String tenantId, String userId);

    /**
     * Finds all users for a tenant.
     *
     * @param tenantId Tenant identifier
     * @return List of users for the tenant
     */
    List<UserEntity> findByTenantId(String tenantId);

    /**
     * Finds a user by username.
     *
     * @param username Username
     * @return User if found, empty otherwise
     */
    Optional<UserEntity> findByUsername(String username);

    /**
     * Finds a user by tenant ID and username.
     *
     * @param tenantId Tenant identifier
     * @param username Username
     * @return User if found, empty otherwise
     */
    Optional<UserEntity> findByTenantIdAndUsername(String tenantId, String username);

    /**
     * Finds users by tenant ID and status.
     *
     * @param tenantId Tenant identifier
     * @param status   User status
     * @return List of users with the specified status
     */
    List<UserEntity> findByTenantIdAndStatus(String tenantId, UserStatus status);

    /**
     * Checks if a user exists by tenant ID and user ID.
     *
     * @param tenantId Tenant identifier
     * @param userId   User identifier
     * @return true if user exists
     */
    boolean existsByTenantIdAndUserId(String tenantId, String userId);

    /**
     * Checks if a username exists.
     *
     * @param username Username
     * @return true if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Finds a user by Keycloak user ID.
     *
     * @param keycloakUserId Keycloak user identifier
     * @return User if found, empty otherwise
     */
    Optional<UserEntity> findByKeycloakUserId(String keycloakUserId);

    /**
     * Finds users by tenant ID where username or email contains the search term (case-insensitive).
     *
     * @param tenantId   Tenant identifier
     * @param searchTerm Search term to match against username or email
     * @return List of users matching the search term
     */
    @Query("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId AND " + "(LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
            + "LOWER(u.emailAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<UserEntity> findByTenantIdAndSearchTerm(@Param("tenantId") String tenantId, @Param("searchTerm") String searchTerm);

    /**
     * Finds users by tenant ID and status where username or email contains the search term (case-insensitive).
     *
     * @param tenantId   Tenant identifier
     * @param status     User status
     * @param searchTerm Search term to match against username or email
     * @return List of users matching the search term and status
     */
    @Query("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId AND u.status = :status AND " + "(LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
            + "LOWER(u.emailAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<UserEntity> findByTenantIdAndStatusAndSearchTerm(@Param("tenantId") String tenantId, @Param("status") UserStatus status, @Param("searchTerm") String searchTerm);
}

