package com.ccbsa.wms.gateway.api.fixture.repository;

import java.util.List;
import java.util.Optional;

import com.ccbsa.wms.gateway.api.dto.CreateConsignmentResponse;
import com.ccbsa.wms.gateway.api.dto.CreateLocationResponse;
import com.ccbsa.wms.gateway.api.dto.CreateProductResponse;
import com.ccbsa.wms.gateway.api.dto.CreateTenantResponse;
import com.ccbsa.wms.gateway.api.dto.CreateUserResponse;
import com.ccbsa.wms.gateway.api.dto.OrderQueryResult;
import com.ccbsa.wms.gateway.api.dto.StockItemResponse;

/**
 * Repository interface for storing and retrieving test data.
 * Enables test data reuse across test runs to improve performance.
 */
public interface TestDataRepository {

    // ==================== LOCATION OPERATIONS ====================

    /**
     * Find a location by its code within a tenant.
     *
     * @param code     the location code
     * @param tenantId the tenant ID
     * @return Optional containing the location if found
     */
    Optional<CreateLocationResponse> findLocationByCode(String code, String tenantId);

    /**
     * Find a location by its ID within a tenant.
     *
     * @param locationId the location ID
     * @param tenantId   the tenant ID
     * @return Optional containing the location if found
     */
    Optional<CreateLocationResponse> findLocationById(String locationId, String tenantId);

    /**
     * Save a location to the repository.
     *
     * @param location the location to save
     * @param tenantId the tenant ID
     */
    void saveLocation(CreateLocationResponse location, String tenantId);

    /**
     * Find all locations of a specific type within a tenant.
     *
     * @param type     the location type (WAREHOUSE, ZONE, AISLE, RACK, BIN)
     * @param tenantId the tenant ID
     * @return list of locations matching the type
     */
    List<CreateLocationResponse> findLocationsByType(String type, String tenantId);

    // ==================== PRODUCT OPERATIONS ====================

    /**
     * Find a product by its code within a tenant.
     *
     * @param productCode the product code
     * @param tenantId    the tenant ID
     * @return Optional containing the product if found
     */
    Optional<CreateProductResponse> findProductByCode(String productCode, String tenantId);

    /**
     * Find a product by its ID within a tenant.
     *
     * @param productId the product ID
     * @param tenantId  the tenant ID
     * @return Optional containing the product if found
     */
    Optional<CreateProductResponse> findProductById(String productId, String tenantId);

    /**
     * Save a product to the repository.
     *
     * @param product  the product to save
     * @param tenantId the tenant ID
     */
    void saveProduct(CreateProductResponse product, String tenantId);

    // ==================== ORDER OPERATIONS ====================

    /**
     * Find an order by its number within a tenant.
     *
     * @param orderNumber the order number
     * @param tenantId    the tenant ID
     * @return Optional containing the order if found
     */
    Optional<OrderQueryResult> findOrderByNumber(String orderNumber, String tenantId);

    /**
     * Find all orders for a tenant.
     *
     * @param tenantId the tenant ID
     * @return list of orders for the tenant
     */
    List<OrderQueryResult> findAllOrders(String tenantId);

    /**
     * Save an order to the repository.
     *
     * @param order    the order to save
     * @param tenantId the tenant ID
     */
    void saveOrder(OrderQueryResult order, String tenantId);

    // ==================== USER OPERATIONS ====================

    /**
     * Find a user by its ID within a tenant.
     *
     * @param userId   the user ID
     * @param tenantId the tenant ID
     * @return Optional containing the user if found
     */
    Optional<CreateUserResponse> findUserById(String userId, String tenantId);

    /**
     * Find any user for a tenant. Useful when any user will suffice for testing.
     *
     * @param tenantId the tenant ID
     * @return Optional containing the first user found for the tenant, or empty if none exists
     */
    Optional<CreateUserResponse> findAnyUserByTenant(String tenantId);

    /**
     * Save a user to the repository.
     *
     * @param user     the user to save
     * @param tenantId the tenant ID
     */
    void saveUser(CreateUserResponse user, String tenantId);

    // ==================== CONSIGNMENT OPERATIONS ====================

    /**
     * Find a consignment by its ID within a tenant.
     *
     * @param consignmentId the consignment ID
     * @param tenantId       the tenant ID
     * @return Optional containing the consignment if found
     */
    Optional<CreateConsignmentResponse> findConsignmentById(String consignmentId, String tenantId);

    /**
     * Save a consignment to the repository.
     *
     * @param consignment the consignment to save
     * @param tenantId    the tenant ID
     */
    void saveConsignment(CreateConsignmentResponse consignment, String tenantId);

    // ==================== STOCK ITEM OPERATIONS ====================

    /**
     * Find a stock item by its ID within a tenant.
     *
     * @param stockItemId the stock item ID
     * @param tenantId    the tenant ID
     * @return Optional containing the stock item if found
     */
    Optional<StockItemResponse> findStockItemById(String stockItemId, String tenantId);

    /**
     * Find stock items by product ID within a tenant.
     *
     * @param productId the product ID
     * @param tenantId  the tenant ID
     * @return list of stock items for the product
     */
    List<StockItemResponse> findStockItemsByProductId(String productId, String tenantId);

    /**
     * Find stock items by product code within a tenant.
     * Note: This requires a lookup from product code to product ID first.
     *
     * @param productCode the product code
     * @param tenantId    the tenant ID
     * @return list of stock items for the product
     */
    List<StockItemResponse> findStockItemsByProductCode(String productCode, String tenantId);

    /**
     * Find stock items by classification within a tenant.
     *
     * @param classification the classification (NORMAL, EXPIRED, NEAR_EXPIRY, etc.)
     * @param tenantId       the tenant ID
     * @return list of stock items matching the classification
     */
    List<StockItemResponse> findStockItemsByClassification(String classification, String tenantId);

    /**
     * Save a stock item to the repository.
     *
     * @param stockItem the stock item to save
     * @param tenantId  the tenant ID
     */
    void saveStockItem(StockItemResponse stockItem, String tenantId);

    /**
     * Save multiple stock items to the repository.
     *
     * @param stockItems the stock items to save
     * @param tenantId   the tenant ID
     */
    void saveStockItems(List<StockItemResponse> stockItems, String tenantId);

    // ==================== TENANT OPERATIONS ====================

    /**
     * Find a tenant by its ID.
     *
     * @param tenantId the tenant ID
     * @return Optional containing the tenant if found
     */
    Optional<CreateTenantResponse> findTenantById(String tenantId);

    /**
     * Save a tenant to the repository.
     *
     * @param tenant the tenant to save
     */
    void saveTenant(CreateTenantResponse tenant);

    // ==================== CLEANUP OPERATIONS ====================

    /**
     * Clear all test data for a specific tenant.
     *
     * @param tenantId the tenant ID
     */
    void clearTenantData(String tenantId);

    /**
     * Clear all test data from the repository.
     */
    void clearAllData();

    /**
     * Initialize the repository (create schema, etc.).
     */
    void initialize();
}
