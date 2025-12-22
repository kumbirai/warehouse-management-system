package com.ccbsa.wms.stock.application.service.port.repository;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stock.domain.core.valueobject.StockItemId;

/**
 * Repository Port: StockItemRepository
 * <p>
 * Defines the contract for StockItem aggregate persistence.
 * <p>
 * This port is defined in the application service layer and implemented by data access adapters.
 */
public interface StockItemRepository {

    /**
     * Saves a stock item aggregate.
     *
     * @param stockItem Stock item to save
     * @return Saved stock item
     */
    StockItem save(StockItem stockItem);

    /**
     * Finds a stock item by ID and tenant.
     *
     * @param stockItemId Stock item ID
     * @param tenantId    Tenant ID
     * @return Optional stock item
     */
    Optional<StockItem> findById(StockItemId stockItemId, TenantId tenantId);

    /**
     * Finds all stock items for a consignment.
     *
     * @param consignmentId Consignment ID
     * @param tenantId      Tenant ID
     * @return List of stock items
     */
    List<StockItem> findByConsignmentId(ConsignmentId consignmentId, TenantId tenantId);

    /**
     * Finds stock items by classification.
     *
     * @param classification Stock classification
     * @param tenantId       Tenant ID
     * @return List of stock items
     */
    List<StockItem> findByClassification(StockClassification classification, TenantId tenantId);

    /**
     * Checks if a stock item exists.
     *
     * @param stockItemId Stock item ID
     * @param tenantId    Tenant ID
     * @return true if exists
     */
    boolean existsById(StockItemId stockItemId, TenantId tenantId);
}

