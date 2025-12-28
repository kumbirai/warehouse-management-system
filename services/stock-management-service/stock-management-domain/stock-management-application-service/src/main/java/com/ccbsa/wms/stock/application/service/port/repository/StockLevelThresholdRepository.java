package com.ccbsa.wms.stock.application.service.port.repository;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.entity.StockLevelThreshold;
import com.ccbsa.wms.stock.domain.core.valueobject.StockLevelThresholdId;

/**
 * Repository Port: StockLevelThresholdRepository
 * <p>
 * Defines the contract for StockLevelThreshold aggregate persistence (write model).
 * <p>
 * This port is defined in the application service layer and implemented by data access adapters.
 */
public interface StockLevelThresholdRepository {

    /**
     * Saves a stock level threshold aggregate.
     *
     * @param threshold Stock level threshold to save
     * @return Saved stock level threshold
     */
    StockLevelThreshold save(StockLevelThreshold threshold);

    /**
     * Finds a stock level threshold by ID and tenant.
     *
     * @param thresholdId Threshold ID
     * @param tenantId    Tenant ID
     * @return Optional stock level threshold
     */
    Optional<StockLevelThreshold> findByIdAndTenantId(StockLevelThresholdId thresholdId, TenantId tenantId);

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
    Optional<StockLevelThreshold> findByTenantIdAndProductIdAndLocationId(TenantId tenantId, ProductId productId, LocationId locationId);

    /**
     * Finds all stock level thresholds for a tenant.
     *
     * @param tenantId Tenant ID
     * @return List of stock level thresholds
     */
    List<StockLevelThreshold> findByTenantId(TenantId tenantId);

    /**
     * Finds stock level thresholds by tenant and product.
     *
     * @param tenantId  Tenant ID
     * @param productId Product ID
     * @return List of stock level thresholds
     */
    List<StockLevelThreshold> findByTenantIdAndProductId(TenantId tenantId, ProductId productId);
}

