package com.ccbsa.wms.user.dataaccess.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ccbsa.wms.user.dataaccess.entity.UserViewEntity;
import com.ccbsa.wms.user.domain.core.valueobject.UserStatus;

/**
 * JPA Repository: UserViewJpaRepository
 * <p>
 * Spring Data JPA repository for UserViewEntity read model queries.
 * <p>
 * Provides optimized read-only queries for user views.
 * <p>
 * Note: Cross-tenant queries are handled in the adapter using JdbcTemplate,
 * as JPA repositories are schema-bound.
 */
@Repository
public interface UserViewJpaRepository extends JpaRepository<UserViewEntity, String> {

    /**
     * Finds a user view by tenant ID and user ID.
     *
     * @param tenantId Tenant ID
     * @param userId   User ID
     * @return Optional UserViewEntity
     */
    Optional<UserViewEntity> findByTenantIdAndUserId(String tenantId, String userId);

    /**
     * Finds a user view by user ID (within current tenant schema).
     *
     * @param userId User ID
     * @return Optional UserViewEntity
     */
    Optional<UserViewEntity> findByUserId(String userId);

    /**
     * Finds all user views for a tenant.
     *
     * @param tenantId Tenant ID
     * @return List of UserViewEntity
     */
    List<UserViewEntity> findByTenantId(String tenantId);

    /**
     * Finds user views by tenant ID and status.
     *
     * @param tenantId Tenant ID
     * @param status   User status
     * @return List of UserViewEntity
     */
    List<UserViewEntity> findByTenantIdAndStatus(String tenantId, UserStatus status);

    /**
     * Finds users by tenant ID where username or email contains the search term (case-insensitive).
     *
     * @param tenantId   Tenant identifier
     * @param searchTerm Search term to match against username or email
     * @return List of users matching the search term
     */
    @Query("SELECT u FROM UserViewEntity u WHERE u.tenantId = :tenantId AND " + "(LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
            + "LOWER(u.emailAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " + "LOWER(COALESCE(u.firstName, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
            + "LOWER(COALESCE(u.lastName, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<UserViewEntity> findByTenantIdAndSearchTerm(@Param("tenantId") String tenantId, @Param("searchTerm") String searchTerm);

    /**
     * Finds users by tenant ID and status where username or email contains the search term (case-insensitive).
     *
     * @param tenantId   Tenant identifier
     * @param status     User status
     * @param searchTerm Search term to match against username or email
     * @return List of users matching the search term and status
     */
    @Query("SELECT u FROM UserViewEntity u WHERE u.tenantId = :tenantId AND u.status = :status AND " + "(LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
            + "LOWER(u.emailAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " + "LOWER(COALESCE(u.firstName, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
            + "LOWER(COALESCE(u.lastName, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<UserViewEntity> findByTenantIdAndStatusAndSearchTerm(@Param("tenantId") String tenantId, @Param("status") UserStatus status, @Param("searchTerm") String searchTerm);
}

