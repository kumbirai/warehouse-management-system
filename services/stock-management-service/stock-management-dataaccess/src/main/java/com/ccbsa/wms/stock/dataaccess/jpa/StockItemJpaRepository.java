package com.ccbsa.wms.stock.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.wms.stock.dataaccess.entity.StockItemEntity;

/**
 * JPA Repository: StockItemJpaRepository
 * <p>
 * Spring Data JPA repository for StockItemEntity. Provides database access methods with multi-tenant support.
 */
public interface StockItemJpaRepository extends JpaRepository<StockItemEntity, UUID> {
    /**
     * Finds a stock item by tenant ID and stock item ID.
     *
     * @param tenantId Tenant identifier
     * @param id       Stock item identifier
     * @return Optional StockItemEntity if found
     */
    Optional<StockItemEntity> findByTenantIdAndId(String tenantId, UUID id);

    /**
     * Finds all stock items for a consignment.
     *
     * @param tenantId      Tenant identifier
     * @param consignmentId Consignment identifier
     * @return List of StockItemEntity
     */
    List<StockItemEntity> findByTenantIdAndConsignmentId(String tenantId, UUID consignmentId);

    /**
     * Finds stock items by classification.
     *
     * @param tenantId       Tenant identifier
     * @param classification Stock classification
     * @return List of StockItemEntity
     */
    List<StockItemEntity> findByTenantIdAndClassification(String tenantId, StockClassification classification);

    /**
     * Checks if a stock item exists.
     *
     * @param tenantId Tenant identifier
     * @param id       Stock item identifier
     * @return true if exists
     */
    boolean existsByTenantIdAndId(String tenantId, UUID id);

    /**
     * Finds stock items by tenant ID and product ID.
     *
     * @param tenantId  Tenant identifier
     * @param productId Product identifier
     * @return List of StockItemEntity
     */
    List<StockItemEntity> findByTenantIdAndProductId(String tenantId, UUID productId);

    /**
     * Finds stock items by tenant ID, product ID, and location ID.
     *
     * @param tenantId   Tenant identifier
     * @param productId  Product identifier
     * @param locationId Location identifier
     * @return List of StockItemEntity
     */
    List<StockItemEntity> findByTenantIdAndProductIdAndLocationId(String tenantId, UUID productId, UUID locationId);

    /**
     * Finds all stock items for a tenant.
     *
     * @param tenantId Tenant identifier
     * @return List of StockItemEntity
     */
    List<StockItemEntity> findByTenantId(String tenantId);
}

