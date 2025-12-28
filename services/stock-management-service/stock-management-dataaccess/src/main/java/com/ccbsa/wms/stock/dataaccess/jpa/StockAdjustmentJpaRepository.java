package com.ccbsa.wms.stock.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ccbsa.wms.stock.dataaccess.entity.StockAdjustmentEntity;

/**
 * JPA Repository: StockAdjustmentJpaRepository
 * <p>
 * Spring Data JPA repository for StockAdjustmentEntity. Provides database access methods with multi-tenant support.
 */
public interface StockAdjustmentJpaRepository extends JpaRepository<StockAdjustmentEntity, UUID> {

    /**
     * Finds a stock adjustment by tenant ID and adjustment ID.
     *
     * @param tenantId Tenant identifier
     * @param id       Stock adjustment identifier
     * @return Optional StockAdjustmentEntity if found
     */
    Optional<StockAdjustmentEntity> findByTenantIdAndId(String tenantId, UUID id);

    /**
     * Finds all stock adjustments for a tenant.
     *
     * @param tenantId Tenant identifier
     * @return List of StockAdjustmentEntity for the tenant
     */
    List<StockAdjustmentEntity> findByTenantId(String tenantId);

    /**
     * Finds stock adjustments by tenant ID and product ID.
     *
     * @param tenantId  Tenant identifier
     * @param productId Product identifier
     * @return List of StockAdjustmentEntity matching the criteria
     */
    List<StockAdjustmentEntity> findByTenantIdAndProductId(String tenantId, UUID productId);

    /**
     * Finds stock adjustments by tenant ID, product ID, and location ID.
     *
     * @param tenantId   Tenant identifier
     * @param productId  Product identifier
     * @param locationId Location identifier
     * @return List of StockAdjustmentEntity matching the criteria
     */
    List<StockAdjustmentEntity> findByTenantIdAndProductIdAndLocationId(String tenantId, UUID productId, UUID locationId);

    /**
     * Finds stock adjustments by tenant ID and stock item ID.
     *
     * @param tenantId    Tenant identifier
     * @param stockItemId Stock item identifier
     * @return List of StockAdjustmentEntity matching the criteria
     */
    List<StockAdjustmentEntity> findByTenantIdAndStockItemId(String tenantId, UUID stockItemId);
}

