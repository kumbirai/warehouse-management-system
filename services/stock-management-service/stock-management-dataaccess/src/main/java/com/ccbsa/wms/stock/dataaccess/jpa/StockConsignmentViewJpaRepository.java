package com.ccbsa.wms.stock.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ccbsa.wms.stock.dataaccess.entity.StockConsignmentViewEntity;

/**
 * JPA Repository: StockConsignmentViewJpaRepository
 * <p>
 * Spring Data JPA repository for StockConsignmentViewEntity read model queries.
 * <p>
 * Provides optimized read-only queries for consignment views.
 */
@Repository
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
}

