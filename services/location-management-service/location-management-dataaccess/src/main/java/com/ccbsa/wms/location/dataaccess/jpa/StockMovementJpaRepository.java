package com.ccbsa.wms.location.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ccbsa.wms.location.dataaccess.entity.StockMovementEntity;
import com.ccbsa.wms.location.domain.core.valueobject.MovementStatus;

/**
 * JPA Repository: StockMovementJpaRepository
 * <p>
 * Spring Data JPA repository for StockMovementEntity. Provides database access methods with multi-tenant support.
 */
public interface StockMovementJpaRepository extends JpaRepository<StockMovementEntity, UUID> {

    /**
     * Finds a stock movement by tenant ID and movement ID.
     *
     * @param tenantId Tenant identifier
     * @param id       Stock movement identifier
     * @return Optional StockMovementEntity if found
     */
    Optional<StockMovementEntity> findByTenantIdAndId(String tenantId, UUID id);

    /**
     * Finds all stock movements for a tenant.
     *
     * @param tenantId Tenant identifier
     * @return List of StockMovementEntity for the tenant
     */
    List<StockMovementEntity> findByTenantId(String tenantId);

    /**
     * Finds stock movements by tenant ID and stock item ID.
     *
     * @param tenantId    Tenant identifier
     * @param stockItemId Stock item identifier (as String, cross-service reference)
     * @return List of StockMovementEntity matching the criteria
     */
    List<StockMovementEntity> findByTenantIdAndStockItemId(String tenantId, String stockItemId);

    /**
     * Finds stock movements by tenant ID and source location ID.
     *
     * @param tenantId         Tenant identifier
     * @param sourceLocationId Source location identifier
     * @return List of StockMovementEntity matching the criteria
     */
    List<StockMovementEntity> findByTenantIdAndSourceLocationId(String tenantId, UUID sourceLocationId);

    /**
     * Finds stock movements by tenant ID and destination location ID.
     *
     * @param tenantId              Tenant identifier
     * @param destinationLocationId Destination location identifier
     * @return List of StockMovementEntity matching the criteria
     */
    List<StockMovementEntity> findByTenantIdAndDestinationLocationId(String tenantId, UUID destinationLocationId);

    /**
     * Finds stock movements by tenant ID and status.
     *
     * @param tenantId Tenant identifier
     * @param status   Movement status
     * @return List of StockMovementEntity matching the criteria
     */
    List<StockMovementEntity> findByTenantIdAndStatus(String tenantId, MovementStatus status);
}

