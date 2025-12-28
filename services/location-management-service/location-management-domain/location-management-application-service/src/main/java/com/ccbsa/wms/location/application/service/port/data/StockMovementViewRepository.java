package com.ccbsa.wms.location.application.service.port.data;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.application.service.port.data.dto.StockMovementView;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.MovementStatus;
import com.ccbsa.wms.location.domain.core.valueobject.StockMovementId;

/**
 * Data Port: StockMovementViewRepository
 * <p>
 * Read model repository for stock movement queries. Provides optimized read access to movement data.
 * <p>
 * This is a data port (read model) used by query handlers, not a repository port (write model).
 */
public interface StockMovementViewRepository {

    /**
     * Finds a stock movement view by tenant ID and movement ID.
     *
     * @param tenantId        Tenant ID
     * @param stockMovementId Stock movement ID
     * @return Optional StockMovementView
     */
    Optional<StockMovementView> findByTenantIdAndId(TenantId tenantId, StockMovementId stockMovementId);

    /**
     * Finds all stock movement views for a tenant.
     *
     * @param tenantId Tenant ID
     * @return List of StockMovementView
     */
    List<StockMovementView> findByTenantId(TenantId tenantId);

    /**
     * Finds stock movement views by tenant ID and stock item ID.
     *
     * @param tenantId    Tenant ID
     * @param stockItemId Stock item ID (as String, cross-service reference)
     * @return List of StockMovementView
     */
    List<StockMovementView> findByTenantIdAndStockItemId(TenantId tenantId, String stockItemId);

    /**
     * Finds stock movement views by tenant ID and source location ID.
     *
     * @param tenantId         Tenant ID
     * @param sourceLocationId Source location ID
     * @return List of StockMovementView
     */
    List<StockMovementView> findByTenantIdAndSourceLocationId(TenantId tenantId, LocationId sourceLocationId);

    /**
     * Finds stock movement views by tenant ID and destination location ID.
     *
     * @param tenantId              Tenant ID
     * @param destinationLocationId Destination location ID
     * @return List of StockMovementView
     */
    List<StockMovementView> findByTenantIdAndDestinationLocationId(TenantId tenantId, LocationId destinationLocationId);

    /**
     * Finds stock movement views by tenant ID and status.
     *
     * @param tenantId Tenant ID
     * @param status   Movement status
     * @return List of StockMovementView
     */
    List<StockMovementView> findByTenantIdAndStatus(TenantId tenantId, MovementStatus status);
}

