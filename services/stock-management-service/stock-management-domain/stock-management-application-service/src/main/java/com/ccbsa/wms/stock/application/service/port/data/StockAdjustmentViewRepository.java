package com.ccbsa.wms.stock.application.service.port.data;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.stock.application.service.port.data.dto.StockAdjustmentView;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAdjustmentId;

/**
 * Data Port: StockAdjustmentViewRepository
 * <p>
 * Read model repository for stock adjustment queries. Provides optimized read access to adjustment data.
 * <p>
 * This is a data port (read model) used by query handlers, not a repository port (write model).
 */
public interface StockAdjustmentViewRepository {

    /**
     * Finds an adjustment view by tenant ID and adjustment ID.
     *
     * @param tenantId     Tenant ID
     * @param adjustmentId Adjustment ID
     * @return Optional StockAdjustmentView
     */
    Optional<StockAdjustmentView> findByTenantIdAndId(TenantId tenantId, StockAdjustmentId adjustmentId);

    /**
     * Finds all adjustment views for a tenant with pagination.
     *
     * @param tenantId Tenant ID
     * @param page     Page number (0-based)
     * @param size     Page size
     * @return List of StockAdjustmentView
     */
    List<StockAdjustmentView> findByTenantId(TenantId tenantId, int page, int size);

    /**
     * Counts adjustment views for a tenant.
     *
     * @param tenantId Tenant ID
     * @return Total count
     */
    long countByTenantId(TenantId tenantId);
}

