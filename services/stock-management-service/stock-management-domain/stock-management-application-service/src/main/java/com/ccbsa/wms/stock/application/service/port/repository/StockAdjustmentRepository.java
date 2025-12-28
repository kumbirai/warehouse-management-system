package com.ccbsa.wms.stock.application.service.port.repository;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.entity.StockAdjustment;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAdjustmentId;

/**
 * Repository Port: StockAdjustmentRepository
 * <p>
 * Defines the contract for StockAdjustment aggregate persistence (write model).
 * <p>
 * This port is defined in the application service layer and implemented by data access adapters.
 */
public interface StockAdjustmentRepository {

    /**
     * Saves a stock adjustment aggregate.
     *
     * @param adjustment Stock adjustment to save
     * @return Saved stock adjustment
     */
    StockAdjustment save(StockAdjustment adjustment);

    /**
     * Finds a stock adjustment by ID and tenant.
     *
     * @param adjustmentId Adjustment ID
     * @param tenantId     Tenant ID
     * @return Optional stock adjustment
     */
    Optional<StockAdjustment> findByIdAndTenantId(StockAdjustmentId adjustmentId, TenantId tenantId);

    /**
     * Finds all stock adjustments for a tenant.
     *
     * @param tenantId Tenant ID
     * @return List of stock adjustments
     */
    List<StockAdjustment> findByTenantId(TenantId tenantId);

    /**
     * Finds stock adjustments by tenant and product.
     *
     * @param tenantId  Tenant ID
     * @param productId Product ID
     * @return List of stock adjustments
     */
    List<StockAdjustment> findByTenantIdAndProductId(TenantId tenantId, ProductId productId);

    /**
     * Finds stock adjustments by tenant, product, and location.
     *
     * @param tenantId   Tenant ID
     * @param productId  Product ID
     * @param locationId Location ID
     * @return List of stock adjustments
     */
    List<StockAdjustment> findByTenantIdAndProductIdAndLocationId(TenantId tenantId, ProductId productId, LocationId locationId);

    /**
     * Finds stock adjustments by tenant and stock item.
     *
     * @param tenantId    Tenant ID
     * @param stockItemId Stock item ID
     * @return List of stock adjustments
     */
    List<StockAdjustment> findByTenantIdAndStockItemId(TenantId tenantId, StockItemId stockItemId);
}

