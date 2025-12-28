package com.ccbsa.wms.location.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ccbsa.wms.location.dataaccess.entity.StockMovementViewEntity;
import com.ccbsa.wms.location.domain.core.valueobject.MovementStatus;

/**
 * JPA Repository: StockMovementViewJpaRepository
 * <p>
 * Spring Data JPA repository for StockMovementViewEntity read model queries.
 * <p>
 * Provides optimized read-only queries for stock movement views.
 */
@Repository
public interface StockMovementViewJpaRepository extends JpaRepository<StockMovementViewEntity, UUID> {

    /**
     * Finds a stock movement view by tenant ID and movement ID.
     *
     * @param tenantId   Tenant ID
     * @param movementId Movement ID
     * @return Optional StockMovementViewEntity
     */
    Optional<StockMovementViewEntity> findByTenantIdAndId(String tenantId, UUID movementId);

    /**
     * Finds all stock movement views for a tenant.
     *
     * @param tenantId Tenant ID
     * @return List of StockMovementViewEntity
     */
    List<StockMovementViewEntity> findByTenantId(String tenantId);

    /**
     * Finds stock movement views by tenant ID and stock item ID.
     *
     * @param tenantId    Tenant ID
     * @param stockItemId Stock item ID (as String, cross-service reference)
     * @return List of StockMovementViewEntity
     */
    List<StockMovementViewEntity> findByTenantIdAndStockItemId(String tenantId, String stockItemId);

    /**
     * Finds stock movement views by tenant ID and source location ID.
     *
     * @param tenantId         Tenant ID
     * @param sourceLocationId Source location ID
     * @return List of StockMovementViewEntity
     */
    List<StockMovementViewEntity> findByTenantIdAndSourceLocationId(String tenantId, UUID sourceLocationId);

    /**
     * Finds stock movement views by tenant ID and destination location ID.
     *
     * @param tenantId              Tenant ID
     * @param destinationLocationId Destination location ID
     * @return List of StockMovementViewEntity
     */
    List<StockMovementViewEntity> findByTenantIdAndDestinationLocationId(String tenantId, UUID destinationLocationId);

    /**
     * Finds stock movement views by tenant ID and status.
     *
     * @param tenantId Tenant ID
     * @param status   Movement status
     * @return List of StockMovementViewEntity
     */
    List<StockMovementViewEntity> findByTenantIdAndStatus(String tenantId, MovementStatus status);
}

