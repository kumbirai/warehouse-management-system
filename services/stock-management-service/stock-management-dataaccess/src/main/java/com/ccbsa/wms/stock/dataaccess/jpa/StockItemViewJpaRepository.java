package com.ccbsa.wms.stock.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.wms.stock.dataaccess.entity.StockItemViewEntity;

/**
 * JPA Repository: StockItemViewJpaRepository
 * <p>
 * Spring Data JPA repository for StockItemViewEntity read model queries.
 * <p>
 * Provides optimized read-only queries for stock item views.
 */
@Repository
public interface StockItemViewJpaRepository extends JpaRepository<StockItemViewEntity, UUID> {

    /**
     * Finds a stock item view by tenant ID and stock item ID.
     *
     * @param tenantId    Tenant ID
     * @param stockItemId Stock item ID
     * @return Optional StockItemViewEntity
     */
    Optional<StockItemViewEntity> findByTenantIdAndId(String tenantId, UUID stockItemId);

    /**
     * Finds all stock item views for a tenant.
     *
     * @param tenantId Tenant ID
     * @return List of StockItemViewEntity
     */
    List<StockItemViewEntity> findByTenantId(String tenantId);

    /**
     * Finds stock item views by tenant ID and classification.
     *
     * @param tenantId       Tenant ID
     * @param classification Stock classification
     * @return List of StockItemViewEntity
     */
    List<StockItemViewEntity> findByTenantIdAndClassification(String tenantId, StockClassification classification);
}

