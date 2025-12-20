package com.ccbsa.wms.tenant.dataaccess.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ccbsa.wms.tenant.dataaccess.entity.TenantEntity;
import com.ccbsa.wms.tenant.domain.core.valueobject.TenantStatus;

/**
 * JPA Repository: TenantJpaRepository
 * <p>
 * Spring Data JPA repository for TenantEntity. Provides standard CRUD operations and custom query methods.
 */
@Repository
public interface TenantJpaRepository extends JpaRepository<TenantEntity, String> {
    /**
     * Finds tenants by status.
     *
     * @param status Tenant status
     * @return List of tenants with the specified status
     */
    List<TenantEntity> findByStatus(TenantStatus status);

    /**
     * Checks if a tenant exists by keycloak realm name.
     *
     * @param keycloakRealmName Keycloak realm name
     * @return true if tenant exists with the specified realm name
     */
    boolean existsByKeycloakRealmName(String keycloakRealmName);

    /**
     * Finds a tenant by keycloak realm name.
     *
     * @param keycloakRealmName Keycloak realm name
     * @return Tenant if found, empty otherwise
     */
    Optional<TenantEntity> findByKeycloakRealmName(String keycloakRealmName);

    /**
     * Searches tenants with optional status and search text filters.
     * <p>
     * Note: Status parameter is String (not TenantStatus) to avoid PostgreSQL type inference issues with NULL parameters in native queries. The enum is converted to String in the
     * adapter.
     *
     * @param status   Tenant status filter as String (nullable, e.g., "ACTIVE", "PENDING")
     * @param search   Search text filter (nullable)
     * @param pageable Pagination information
     * @return Paginated tenants matching filters
     */
    @Query(value = """
            SELECT t.* FROM tenants t
            WHERE (:status IS NULL OR t.status = :status)
            AND (
                :search IS NULL
                OR LOWER(t.tenant_id::text) LIKE LOWER('%' || :search || '%')
                OR LOWER(t.name) LIKE LOWER('%' || :search || '%')
            )
            ORDER BY t.created_at DESC
            """, countQuery = """
            SELECT COUNT(*) FROM tenants t
            WHERE (:status IS NULL OR t.status = :status)
            AND (
                :search IS NULL
                OR LOWER(t.tenant_id::text) LIKE LOWER('%' || :search || '%')
                OR LOWER(t.name) LIKE LOWER('%' || :search || '%')
            )
            """, nativeQuery = true)
    Page<TenantEntity> searchTenants(@Param("status") String status, @Param("search") String search, Pageable pageable);
}

