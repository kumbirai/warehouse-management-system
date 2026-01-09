package com.ccbsa.wms.picking.application.service.port.service;

import java.util.List;
import java.util.Map;

import com.ccbsa.wms.picking.application.service.port.service.dto.StockAvailabilityInfo;

/**
 * Service Port: StockManagementServicePort
 * <p>
 * Defines the contract for Stock Management Service integration. Implemented by infrastructure adapters.
 * <p>
 * This port is used for synchronous stock availability queries during picking location planning.
 */
public interface StockManagementServicePort {
    /**
     * Queries available stock for a product, sorted by FEFO (First Expiring First Out).
     * <p>
     * Returns stock items sorted by expiration date (earliest first), excluding expired stock.
     *
     * @param productCode Product code
     * @param quantity    Required quantity
     * @return List of StockAvailabilityInfo sorted by expiration date (earliest first)
     */
    List<StockAvailabilityInfo> queryAvailableStockByFEFO(String productCode, int quantity);

    /**
     * Queries available stock for multiple products.
     *
     * @param productQuantities Map of product codes to required quantities
     * @return Map of product codes to lists of StockAvailabilityInfo
     */
    Map<String, List<StockAvailabilityInfo>> queryAvailableStockForProducts(Map<String, Integer> productQuantities);
}
