package com.ccbsa.wms.user.application.service.port.data;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.user.application.service.port.data.dto.UserView;
import com.ccbsa.wms.user.domain.core.valueobject.UserStatus;

/**
 * Data Port: UserViewRepository
 * <p>
 * Read model repository for user queries. Provides optimized read access to user data.
 * <p>
 * This is a data port (read model) used by query handlers, not a repository port (write model).
 * <p>
 * Responsibilities:
 * - Provide optimized read model queries
 * - Support eventual consistency (read model may lag behind write model)
 * - Enable query performance optimization through denormalization
 * - Support cross-tenant queries for SYSTEM_ADMIN users
 */
public interface UserViewRepository {

    /**
     * Finds a user view by user ID within the current tenant schema.
     *
     * @param userId User ID
     * @return Optional UserView
     */
    Optional<UserView> findById(UserId userId);

    /**
     * Finds a user view by user ID across all tenant schemas.
     * <p>
     * Used by SYSTEM_ADMIN users to search across all tenants.
     *
     * @param userId User ID
     * @return Optional UserView
     */
    Optional<UserView> findByIdAcrossTenants(UserId userId);

    /**
     * Finds all user views for a tenant with optional filters.
     *
     * @param tenantId Tenant ID
     * @param status   Optional status filter (null to ignore)
     * @param search   Optional search term (searches in username, email, firstName, lastName)
     * @return List of UserView
     */
    List<UserView> findByTenantId(TenantId tenantId, UserStatus status, String search);

    /**
     * Finds all user views across all tenant schemas with optional filters.
     * <p>
     * Used by SYSTEM_ADMIN users to query across all tenants.
     *
     * @param status Optional status filter (null to ignore)
     * @param search Optional search term (searches in username, email, firstName, lastName)
     * @return List of UserView
     */
    List<UserView> findAllAcrossTenants(UserStatus status, String search);
}

