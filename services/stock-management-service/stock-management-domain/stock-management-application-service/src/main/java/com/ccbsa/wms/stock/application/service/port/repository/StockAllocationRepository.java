package com.ccbsa.wms.stock.application.service.port.repository;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.entity.StockAllocation;
import com.ccbsa.wms.stock.domain.core.valueobject.AllocationStatus;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAllocationId;

/**
 * Repository Port: StockAllocationRepository
 * <p>
 * Defines the contract for StockAllocation aggregate persistence (write model).
 * <p>
 * This port is defined in the application service layer and implemented by data access adapters.
 */
public interface StockAllocationRepository {

    /**
     * Saves a stock allocation aggregate.
     *
     * @param allocation Stock allocation to save
     * @return Saved stock allocation
     */
    StockAllocation save(StockAllocation allocation);

    /**
     * Finds a stock allocation by ID and tenant.
     *
     * @param allocationId Allocation ID
     * @param tenantId     Tenant ID
     * @return Optional stock allocation
     */
    Optional<StockAllocation> findByIdAndTenantId(StockAllocationId allocationId, TenantId tenantId);

    /**
     * Finds all stock allocations for a tenant.
     *
     * @param tenantId Tenant ID
     * @return List of stock allocations
     */
    List<StockAllocation> findByTenantId(TenantId tenantId);

    /**
     * Finds stock allocations by tenant and product.
     *
     * @param tenantId  Tenant ID
     * @param productId Product ID
     * @return List of stock allocations
     */
    List<StockAllocation> findByTenantIdAndProductId(TenantId tenantId, ProductId productId);

    /**
     * Finds stock allocations by tenant, product, and location.
     *
     * @param tenantId   Tenant ID
     * @param productId  Product ID
     * @param locationId Location ID
     * @return List of stock allocations
     */
    List<StockAllocation> findByTenantIdAndProductIdAndLocationId(TenantId tenantId, ProductId productId, LocationId locationId);

    /**
     * Finds stock allocations by stock item.
     *
     * @param stockItemId Stock item ID
     * @return List of stock allocations
     */
    List<StockAllocation> findByStockItemId(StockItemId stockItemId);

    /**
     * Finds stock allocations by stock item and status.
     *
     * @param stockItemId Stock item ID
     * @param status      Allocation status
     * @return List of stock allocations
     */
    List<StockAllocation> findByStockItemIdAndStatus(StockItemId stockItemId, AllocationStatus status);

    /**
     * Finds stock allocations by tenant and reference ID.
     *
     * @param tenantId    Tenant ID
     * @param referenceId Reference ID (e.g., order ID)
     * @return List of stock allocations
     */
    List<StockAllocation> findByTenantIdAndReferenceId(TenantId tenantId, String referenceId);
}

