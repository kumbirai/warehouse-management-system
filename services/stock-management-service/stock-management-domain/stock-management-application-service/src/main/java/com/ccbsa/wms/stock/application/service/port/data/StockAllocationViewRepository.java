package com.ccbsa.wms.stock.application.service.port.data;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.application.service.port.data.dto.StockAllocationView;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAllocationId;

/**
 * Data Port: StockAllocationViewRepository
 * <p>
 * Read model repository for stock allocation queries. Provides optimized read access to allocation data.
 * <p>
 * This is a data port (read model) used by query handlers, not a repository port (write model).
 */
public interface StockAllocationViewRepository {

    /**
     * Finds an allocation view by tenant ID and allocation ID.
     *
     * @param tenantId     Tenant ID
     * @param allocationId Allocation ID
     * @return Optional StockAllocationView
     */
    Optional<StockAllocationView> findByTenantIdAndId(TenantId tenantId, StockAllocationId allocationId);

    /**
     * Finds all allocation views for a tenant with pagination.
     *
     * @param tenantId Tenant ID
     * @param page     Page number (0-based)
     * @param size     Page size
     * @return List of StockAllocationView
     */
    List<StockAllocationView> findByTenantId(TenantId tenantId, int page, int size);

    /**
     * Counts allocation views for a tenant.
     *
     * @param tenantId Tenant ID
     * @return Total count
     */
    long countByTenantId(TenantId tenantId);

    /**
     * Finds stock allocations by tenant and product.
     *
     * @param tenantId  Tenant ID
     * @param productId Product ID
     * @return List of StockAllocationView
     */
    List<StockAllocationView> findByTenantIdAndProductId(TenantId tenantId, ProductId productId);

    /**
     * Finds stock allocations by tenant, product, and location.
     *
     * @param tenantId   Tenant ID
     * @param productId  Product ID
     * @param locationId Location ID
     * @return List of StockAllocationView
     */
    List<StockAllocationView> findByTenantIdAndProductIdAndLocationId(TenantId tenantId, ProductId productId, LocationId locationId);

    /**
     * Finds stock allocations by stock item.
     *
     * @param stockItemId Stock item ID
     * @return List of StockAllocationView
     */
    List<StockAllocationView> findByStockItemId(StockItemId stockItemId);
}

