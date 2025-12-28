package com.ccbsa.wms.stock.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ccbsa.wms.stock.dataaccess.entity.StockAllocationViewEntity;

/**
 * JPA Repository: StockAllocationViewJpaRepository
 * <p>
 * Spring Data JPA repository for StockAllocationViewEntity read model queries.
 * <p>
 * Provides optimized read-only queries for allocation views.
 */
@Repository
public interface StockAllocationViewJpaRepository extends JpaRepository<StockAllocationViewEntity, UUID> {

    /**
     * Finds an allocation view by tenant ID and allocation ID.
     *
     * @param tenantId     Tenant ID
     * @param allocationId Allocation ID
     * @return Optional StockAllocationViewEntity
     */
    Optional<StockAllocationViewEntity> findByTenantIdAndId(String tenantId, UUID allocationId);

    /**
     * Finds all allocation views for a tenant with pagination, ordered by allocated date descending.
     *
     * @param tenantId Tenant ID
     * @param pageable Pagination parameters
     * @return List of StockAllocationViewEntity
     */
    List<StockAllocationViewEntity> findByTenantIdOrderByAllocatedAtDesc(String tenantId, Pageable pageable);

    /**
     * Counts all allocation views for a tenant.
     *
     * @param tenantId Tenant ID
     * @return Total count
     */
    long countByTenantId(String tenantId);
}

