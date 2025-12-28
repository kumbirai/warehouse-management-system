package com.ccbsa.wms.location.application.service.port.service;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

/**
 * Service Port: StockManagementServicePort
 * <p>
 * Defines the contract for validating stock items via Stock Management Service.
 * Implemented by infrastructure adapters (REST client).
 * <p>
 * This port enables synchronous validation of stock items before creating movements.
 */
public interface StockManagementServicePort {

    /**
     * Validates that a stock item exists and has sufficient quantity.
     *
     * @param stockItemId Stock item identifier (as String, cross-service reference)
     * @param quantity    Required quantity
     * @param tenantId    Tenant identifier
     * @return Validation result containing product ID if valid
     * @throws IllegalArgumentException if stock item is invalid or has insufficient quantity
     */
    StockItemValidationResult validateStockItem(String stockItemId, Quantity quantity, TenantId tenantId);

    /**
     * Finds a stock item by product ID and location ID.
     * Used when stockItemId is not provided in the movement request.
     *
     * @param productId  Product identifier
     * @param locationId Location identifier
     * @param tenantId   Tenant identifier
     * @return Query result containing stock item ID if found
     */
    StockItemQueryResult findStockItemByProductAndLocation(ProductId productId, LocationId locationId, TenantId tenantId);

    /**
     * Finds a stock item by product ID only (for stock items without location assignment).
     * Used as fallback when stock items haven't been assigned locations yet via FEFO.
     *
     * @param productId Product identifier
     * @param tenantId  Tenant identifier
     * @return Query result containing stock item ID if found
     */
    StockItemQueryResult findStockItemByProduct(ProductId productId, TenantId tenantId);

    /**
     * Result of stock item query.
     */
    final class StockItemQueryResult {
        private final boolean found;
        private final String stockItemId;
        private final String errorMessage;

        private StockItemQueryResult(boolean found, String stockItemId, String errorMessage) {
            this.found = found;
            this.stockItemId = stockItemId;
            this.errorMessage = errorMessage;
        }

        public static StockItemQueryResult found(String stockItemId) {
            return new StockItemQueryResult(true, stockItemId, null);
        }

        public static StockItemQueryResult notFound(String errorMessage) {
            return new StockItemQueryResult(false, null, errorMessage);
        }

        public boolean isFound() {
            return found;
        }

        public String getStockItemId() {
            return stockItemId;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Result of stock item validation.
     */
    final class StockItemValidationResult {
        private final boolean valid;
        private final ProductId productId;
        private final String errorMessage;

        private StockItemValidationResult(boolean valid, ProductId productId, String errorMessage) {
            this.valid = valid;
            this.productId = productId;
            this.errorMessage = errorMessage;
        }

        public static StockItemValidationResult valid(ProductId productId) {
            return new StockItemValidationResult(true, productId, null);
        }

        public static StockItemValidationResult invalid(String errorMessage) {
            return new StockItemValidationResult(false, null, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public ProductId getProductId() {
            return productId;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}

