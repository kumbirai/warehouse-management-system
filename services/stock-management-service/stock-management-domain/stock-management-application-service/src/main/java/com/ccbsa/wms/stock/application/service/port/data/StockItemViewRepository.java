package com.ccbsa.wms.stock.application.service.port.data;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.stock.application.service.port.data.dto.StockItemView;

/**
 * Data Port: StockItemViewRepository
 * <p>
 * Read model repository for stock item queries. Provides optimized read access to stock item data.
 * <p>
 * This is a data port (read model) used by query handlers, not a repository port (write model).
 * <p>
 * Responsibilities:
 * - Provide optimized read model queries
 * - Support eventual consistency (read model may lag behind write model)
 * - Enable query performance optimization through denormalization
 */
public interface StockItemViewRepository {

    /**
     * Finds a stock item view by tenant ID and stock item ID.
     *
     * @param tenantId    Tenant ID
     * @param stockItemId Stock item ID
     * @return Optional StockItemView
     */
    Optional<StockItemView> findByTenantIdAndId(TenantId tenantId, StockItemId stockItemId);

    /**
     * Finds all stock item views for a tenant.
     *
     * @param tenantId Tenant ID
     * @return List of StockItemView
     */
    List<StockItemView> findByTenantId(TenantId tenantId);

    /**
     * Finds stock item views by tenant ID and classification.
     *
     * @param tenantId       Tenant ID
     * @param classification Stock classification
     * @return List of StockItemView
     */
    List<StockItemView> findByTenantIdAndClassification(TenantId tenantId, StockClassification classification);
}

