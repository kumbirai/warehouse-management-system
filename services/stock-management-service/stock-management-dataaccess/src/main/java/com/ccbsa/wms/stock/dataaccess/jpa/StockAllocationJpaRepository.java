package com.ccbsa.wms.stock.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ccbsa.wms.stock.dataaccess.entity.StockAllocationEntity;
import com.ccbsa.wms.stock.domain.core.valueobject.AllocationStatus;

/**
 * JPA Repository: StockAllocationJpaRepository
 * <p>
 * Spring Data JPA repository for StockAllocationEntity. Provides database access methods with multi-tenant support.
 */
public interface StockAllocationJpaRepository extends JpaRepository<StockAllocationEntity, UUID> {

    /**
     * Finds a stock allocation by tenant ID and allocation ID.
     *
     * @param tenantId Tenant identifier
     * @param id       Stock allocation identifier
     * @return Optional StockAllocationEntity if found
     */
    Optional<StockAllocationEntity> findByTenantIdAndId(String tenantId, UUID id);

    /**
     * Finds all stock allocations for a tenant.
     *
     * @param tenantId Tenant identifier
     * @return List of StockAllocationEntity for the tenant
     */
    List<StockAllocationEntity> findByTenantId(String tenantId);

    /**
     * Finds stock allocations by tenant ID and product ID.
     *
     * @param tenantId  Tenant identifier
     * @param productId Product identifier
     * @return List of StockAllocationEntity matching the criteria
     */
    List<StockAllocationEntity> findByTenantIdAndProductId(String tenantId, UUID productId);

    /**
     * Finds stock allocations by tenant ID, product ID, and location ID.
     *
     * @param tenantId   Tenant identifier
     * @param productId  Product identifier
     * @param locationId Location identifier
     * @return List of StockAllocationEntity matching the criteria
     */
    List<StockAllocationEntity> findByTenantIdAndProductIdAndLocationId(String tenantId, UUID productId, UUID locationId);

    /**
     * Finds stock allocations by stock item ID.
     *
     * @param stockItemId Stock item identifier
     * @return List of StockAllocationEntity matching the criteria
     */
    List<StockAllocationEntity> findByStockItemId(UUID stockItemId);

    /**
     * Finds stock allocations by stock item ID and status.
     *
     * @param stockItemId Stock item identifier
     * @param status      Allocation status
     * @return List of StockAllocationEntity matching the criteria
     */
    List<StockAllocationEntity> findByStockItemIdAndStatus(UUID stockItemId, AllocationStatus status);

    /**
     * Finds stock allocations by tenant ID and reference ID.
     *
     * @param tenantId    Tenant identifier
     * @param referenceId Reference identifier (e.g., order ID)
     * @return List of StockAllocationEntity matching the criteria
     */
    List<StockAllocationEntity> findByTenantIdAndReferenceId(String tenantId, String referenceId);
}

