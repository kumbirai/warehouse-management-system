package com.ccbsa.wms.location.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ccbsa.wms.location.dataaccess.entity.LocationViewEntity;
import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;

/**
 * JPA Repository: LocationViewJpaRepository
 * <p>
 * Spring Data JPA repository for LocationViewEntity read model queries.
 * <p>
 * Provides optimized read-only queries for location views.
 */
@Repository
public interface LocationViewJpaRepository extends JpaRepository<LocationViewEntity, UUID> {

    /**
     * Finds a location view by tenant ID and location ID.
     *
     * @param tenantId   Tenant ID
     * @param locationId Location ID
     * @return Optional LocationViewEntity
     */
    Optional<LocationViewEntity> findByTenantIdAndId(String tenantId, UUID locationId);

    /**
     * Finds location views with filtering and pagination support using native SQL.
     * <p>
     * Performs database-level filtering for efficient querying.
     * All filtering (zone, status, search) is done at the database level.
     *
     * @param tenantId Tenant identifier
     * @param zone     Optional zone filter (case-insensitive, null to ignore)
     * @param status   Optional status filter (null to ignore)
     * @param search   Optional search term (searches in barcode, zone, aisle, description)
     * @param pageable Pagination information
     * @return Page of LocationViewEntity matching the criteria with pagination applied
     */
    @Query(value = "SELECT l.* FROM locations l WHERE l.tenant_id = :tenantId " + "AND (:zone IS NULL OR LOWER(l.zone) = LOWER(:zone)) "
            + "AND (:status IS NULL OR l.status = :status) " + "AND (:search IS NULL OR " + "LOWER(l.barcode) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "LOWER(l.zone) LIKE LOWER(CONCAT('%', :search, '%')) OR " + "LOWER(l.aisle) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "LOWER(COALESCE(l.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " + "ORDER BY l.code ASC NULLS LAST, l.barcode ASC", countQuery =
            "SELECT COUNT(l) FROM locations l WHERE l.tenant_id = :tenantId " + "AND (:zone IS NULL OR LOWER(l.zone) = LOWER(:zone)) "
                    + "AND (:status IS NULL OR l.status = :status) " + "AND (:search IS NULL OR " + "LOWER(l.barcode) LIKE LOWER(CONCAT('%', :search, '%')) OR "
                    + "LOWER(l.zone) LIKE LOWER(CONCAT('%', :search, '%')) OR " + "LOWER(l.aisle) LIKE LOWER(CONCAT('%', :search, '%')) OR "
                    + "LOWER(COALESCE(l.description, '')) LIKE LOWER(CONCAT('%', :search, '%')))", nativeQuery = true)
    Page<LocationViewEntity> findByTenantIdWithFilters(@Param("tenantId") String tenantId, @Param("zone") String zone, @Param("status") String status,
                                                       @Param("search") String search, Pageable pageable);

    /**
     * Counts location views matching the filter criteria using native SQL.
     *
     * @param tenantId Tenant identifier
     * @param zone     Optional zone filter (case-insensitive, null to ignore)
     * @param status   Optional status filter (null to ignore)
     * @param search   Optional search term (searches in barcode, zone, aisle, description)
     * @return Total count of location views matching the criteria
     */
    @Query(value = "SELECT COUNT(l) FROM locations l WHERE l.tenant_id = :tenantId " + "AND (:zone IS NULL OR LOWER(l.zone) = LOWER(:zone)) "
            + "AND (:status IS NULL OR l.status = :status) " + "AND (:search IS NULL OR " + "LOWER(l.barcode) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "LOWER(l.zone) LIKE LOWER(CONCAT('%', :search, '%')) OR " + "LOWER(l.aisle) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "LOWER(COALESCE(l.description, '')) LIKE LOWER(CONCAT('%', :search, '%')))", nativeQuery = true)
    long countByTenantIdWithFilters(@Param("tenantId") String tenantId, @Param("zone") String zone, @Param("status") String status, @Param("search") String search);

    /**
     * Finds available location views for a tenant (status = AVAILABLE or RESERVED).
     *
     * @param tenantId Tenant ID
     * @param statuses List of available statuses
     * @return List of LocationViewEntity
     */
    List<LocationViewEntity> findByTenantIdAndStatusIn(String tenantId, List<LocationStatus> statuses);

    /**
     * Finds location views by tenant ID and type.
     *
     * @param tenantId Tenant ID
     * @param type     Location type (WAREHOUSE, ZONE, AISLE, RACK, BIN)
     * @return List of LocationViewEntity
     */
    List<LocationViewEntity> findByTenantIdAndType(String tenantId, String type);

    /**
     * Finds location views by tenant ID and parent location ID.
     *
     * @param tenantId         Tenant ID
     * @param parentLocationId Parent location ID
     * @return List of LocationViewEntity
     */
    List<LocationViewEntity> findByTenantIdAndParentLocationId(String tenantId, UUID parentLocationId);

    /**
     * Finds location views by tenant ID, type, and parent location ID.
     *
     * @param tenantId         Tenant ID
     * @param type             Location type (WAREHOUSE, ZONE, AISLE, RACK, BIN)
     * @param parentLocationId Parent location ID
     * @return List of LocationViewEntity
     */
    List<LocationViewEntity> findByTenantIdAndTypeAndParentLocationId(String tenantId, String type, UUID parentLocationId);
}

