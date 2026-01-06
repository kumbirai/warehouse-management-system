package com.ccbsa.wms.stock.dataaccess.jpa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ccbsa.wms.stock.dataaccess.entity.StockConsignmentViewEntity;

/**
 * JPA Repository: StockConsignmentViewJpaRepository
 * <p>
 * Spring Data JPA repository for StockConsignmentViewEntity read model queries.
 * <p>
 * Provides optimized read-only queries for consignment views.
 */
public interface StockConsignmentViewJpaRepository extends JpaRepository<StockConsignmentViewEntity, UUID> {

    /**
     * Finds a consignment view by tenant ID and consignment ID.
     *
     * @param tenantId      Tenant ID
     * @param consignmentId Consignment ID
     * @return Optional StockConsignmentViewEntity
     */
    Optional<StockConsignmentViewEntity> findByTenantIdAndId(String tenantId, UUID consignmentId);

    /**
     * Finds all consignment views for a tenant with pagination, ordered by creation date descending.
     *
     * @param tenantId Tenant ID
     * @param pageable Pagination parameters
     * @return List of StockConsignmentViewEntity
     */
    List<StockConsignmentViewEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    /**
     * Counts all consignment views for a tenant.
     *
     * @param tenantId Tenant ID
     * @return Total count
     */
    long countByTenantId(String tenantId);

    /**
     * Finds distinct consignment IDs for a tenant, filtered by expiration date.
     * Returns consignment IDs where any line item has an expiration date within the specified days.
     *
     * @param tenantId                Tenant ID
     * @param expirationDateThreshold Maximum expiration date (today + expiringWithinDays)
     * @param pageSize                Page size
     * @param offset                  Offset for pagination
     * @return List of consignment IDs
     */
    @Query(value = "SELECT c.id FROM stock_consignments c " + "WHERE c.tenant_id = :tenantId " + "AND EXISTS (" + "    SELECT 1 FROM consignment_line_items li "
            + "    WHERE li.consignment_id = c.id " + "    AND li.expiration_date IS NOT NULL " + "    AND li.expiration_date <= :expirationDateThreshold" + ") "
            + "ORDER BY c.created_at DESC " + "LIMIT :pageSize OFFSET :offset", nativeQuery = true)
    List<UUID> findConsignmentIdsByTenantIdAndExpiringWithinDays(@Param("tenantId") String tenantId, @Param("expirationDateThreshold") LocalDate expirationDateThreshold,
                                                                 @Param("pageSize") int pageSize, @Param("offset") long offset);

    /**
     * Counts consignment views for a tenant, filtered by expiration date.
     * Returns count of consignments where any line item has an expiration date within the specified days.
     *
     * @param tenantId                Tenant ID
     * @param expirationDateThreshold Maximum expiration date (today + expiringWithinDays)
     * @return Total count
     */
    @Query(value = "SELECT COUNT(DISTINCT c.id) FROM stock_consignments c " + "INNER JOIN consignment_line_items li ON c.id = li.consignment_id " + "WHERE c.tenant_id = :tenantId "
            + "AND li.expiration_date IS NOT NULL " + "AND li.expiration_date <= :expirationDateThreshold", nativeQuery = true)
    long countByTenantIdAndExpiringWithinDays(@Param("tenantId") String tenantId, @Param("expirationDateThreshold") LocalDate expirationDateThreshold);
}

