package com.ccbsa.wms.stock.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ccbsa.wms.stock.dataaccess.entity.StockLevelThresholdEntity;

/**
 * JPA Repository: StockLevelThresholdJpaRepository
 * <p>
 * Spring Data JPA repository for StockLevelThresholdEntity. Provides database access methods with multi-tenant support.
 */
public interface StockLevelThresholdJpaRepository extends JpaRepository<StockLevelThresholdEntity, UUID> {

    /**
     * Finds a stock level threshold by tenant ID and threshold ID.
     *
     * @param tenantId Tenant identifier
     * @param id       Stock level threshold identifier
     * @return Optional StockLevelThresholdEntity if found
     */
    Optional<StockLevelThresholdEntity> findByTenantIdAndId(String tenantId, UUID id);

    /**
     * Finds stock level threshold by tenant ID, product ID, and location ID.
     * <p>
     * If locationId is null, finds warehouse-wide threshold for the product.
     *
     * @param tenantId   Tenant identifier
     * @param productId  Product identifier
     * @param locationId Location identifier (can be null for warehouse-wide)
     * @return Optional StockLevelThresholdEntity if found
     */
    Optional<StockLevelThresholdEntity> findByTenantIdAndProductIdAndLocationId(String tenantId, UUID productId, UUID locationId);

    /**
     * Finds all stock level thresholds for a tenant.
     *
     * @param tenantId Tenant identifier
     * @return List of StockLevelThresholdEntity for the tenant
     */
    List<StockLevelThresholdEntity> findByTenantId(String tenantId);

    /**
     * Finds stock level thresholds by tenant ID and product ID.
     *
     * @param tenantId  Tenant identifier
     * @param productId Product identifier
     * @return List of StockLevelThresholdEntity matching the criteria
     */
    List<StockLevelThresholdEntity> findByTenantIdAndProductId(String tenantId, UUID productId);
}

