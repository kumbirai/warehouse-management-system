package com.ccbsa.wms.location.application.service.port.repository;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.entity.StockMovement;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.StockMovementId;

/**
 * Repository Port: StockMovementRepository
 * <p>
 * Repository port for StockMovement aggregate persistence (write model).
 * <p>
 * This port is defined in the application service layer and implemented by data access adapters.
 */
public interface StockMovementRepository {

    /**
     * Saves a StockMovement aggregate.
     *
     * @param movement StockMovement aggregate to save
     * @return Saved StockMovement aggregate (with updated version)
     */
    StockMovement save(StockMovement movement);

    /**
     * Finds a StockMovement by ID and tenant ID.
     *
     * @param movementId Movement identifier
     * @param tenantId   Tenant identifier
     * @return Optional StockMovement if found
     */
    Optional<StockMovement> findByIdAndTenantId(StockMovementId movementId, TenantId tenantId);

    /**
     * Finds all StockMovements for a tenant.
     *
     * @param tenantId Tenant identifier
     * @return List of StockMovements
     */
    List<StockMovement> findByTenantId(TenantId tenantId);

    /**
     * Finds StockMovements by stock item ID and tenant ID.
     *
     * @param tenantId    Tenant identifier
     * @param stockItemId Stock item identifier (as String, cross-service reference)
     * @return List of StockMovements
     */
    List<StockMovement> findByTenantIdAndStockItemId(TenantId tenantId, String stockItemId);

    /**
     * Finds StockMovements by source location ID and tenant ID.
     *
     * @param tenantId         Tenant identifier
     * @param sourceLocationId Source location identifier
     * @return List of StockMovements
     */
    List<StockMovement> findByTenantIdAndSourceLocationId(TenantId tenantId, LocationId sourceLocationId);

    /**
     * Finds StockMovements by destination location ID and tenant ID.
     *
     * @param tenantId              Tenant identifier
     * @param destinationLocationId Destination location identifier
     * @return List of StockMovements
     */
    List<StockMovement> findByTenantIdAndDestinationLocationId(TenantId tenantId, LocationId destinationLocationId);

    /**
     * Finds StockMovements by status and tenant ID.
     *
     * @param tenantId Tenant identifier
     * @param status   Movement status
     * @return List of StockMovements
     */
    List<StockMovement> findByTenantIdAndStatus(TenantId tenantId, com.ccbsa.wms.location.domain.core.valueobject.MovementStatus status);
}

