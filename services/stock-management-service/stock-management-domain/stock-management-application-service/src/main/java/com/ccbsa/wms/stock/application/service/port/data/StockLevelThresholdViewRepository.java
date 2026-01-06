package com.ccbsa.wms.stock.application.service.port.data;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.application.service.port.data.dto.StockLevelThresholdView;
import com.ccbsa.wms.stock.domain.core.valueobject.StockLevelThresholdId;

/**
 * Data Port: StockLevelThresholdViewRepository
 * <p>
 * Read model repository for stock level threshold queries. Provides optimized read access to threshold data.
 * <p>
 * This is a data port (read model) used by query handlers, not a repository port (write model).
 */
public interface StockLevelThresholdViewRepository {

    /**
     * Finds a threshold view by tenant ID and threshold ID.
     *
     * @param tenantId    Tenant ID
     * @param thresholdId Threshold ID
     * @return Optional StockLevelThresholdView
     */
    Optional<StockLevelThresholdView> findByTenantIdAndId(TenantId tenantId, StockLevelThresholdId thresholdId);

    /**
     * Finds stock level threshold by tenant, product, and location.
     * <p>
     * If locationId is null, finds warehouse-wide threshold for the product.
     *
     * @param tenantId   Tenant ID
     * @param productId  Product ID
     * @param locationId Location ID (can be null for warehouse-wide)
     * @return Optional stock level threshold
     */
    Optional<StockLevelThresholdView> findByTenantIdAndProductIdAndLocationId(TenantId tenantId, ProductId productId, LocationId locationId);

    /**
     * Finds all stock level thresholds for a tenant.
     *
     * @param tenantId Tenant ID
     * @return List of StockLevelThresholdView
     */
    List<StockLevelThresholdView> findByTenantId(TenantId tenantId);

    /**
     * Finds stock level thresholds by tenant and product.
     *
     * @param tenantId  Tenant ID
     * @param productId Product ID
     * @return List of StockLevelThresholdView
     */
    List<StockLevelThresholdView> findByTenantIdAndProductId(TenantId tenantId, ProductId productId);
}
