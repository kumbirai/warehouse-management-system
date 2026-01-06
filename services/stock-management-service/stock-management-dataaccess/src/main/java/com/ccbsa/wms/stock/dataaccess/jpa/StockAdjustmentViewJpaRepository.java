package com.ccbsa.wms.stock.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ccbsa.wms.stock.dataaccess.entity.StockAdjustmentViewEntity;

/**
 * JPA Repository: StockAdjustmentViewJpaRepository
 * <p>
 * Spring Data JPA repository for StockAdjustmentViewEntity read model queries.
 * <p>
 * Provides optimized read-only queries for adjustment views.
 */
public interface StockAdjustmentViewJpaRepository extends JpaRepository<StockAdjustmentViewEntity, UUID> {

    /**
     * Finds an adjustment view by tenant ID and adjustment ID.
     *
     * @param tenantId     Tenant ID
     * @param adjustmentId Adjustment ID
     * @return Optional StockAdjustmentViewEntity
     */
    Optional<StockAdjustmentViewEntity> findByTenantIdAndId(String tenantId, UUID adjustmentId);

    /**
     * Finds all adjustment views for a tenant with pagination, ordered by adjusted date descending.
     *
     * @param tenantId Tenant ID
     * @param pageable Pagination parameters
     * @return List of StockAdjustmentViewEntity
     */
    List<StockAdjustmentViewEntity> findByTenantIdOrderByAdjustedAtDesc(String tenantId, Pageable pageable);

    /**
     * Counts all adjustment views for a tenant.
     *
     * @param tenantId Tenant ID
     * @return Total count
     */
    long countByTenantId(String tenantId);
}

