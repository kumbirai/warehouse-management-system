package com.ccbsa.wms.gateway.api.fixture;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.ccbsa.wms.gateway.api.dto.CreateConsignmentResponse;
import com.ccbsa.wms.gateway.api.dto.CreateLocationRequest;
import com.ccbsa.wms.gateway.api.dto.CreateLocationResponse;
import com.ccbsa.wms.gateway.api.dto.CreateProductRequest;
import com.ccbsa.wms.gateway.api.dto.CreateProductResponse;
import com.ccbsa.wms.gateway.api.dto.CreateTenantResponse;
import com.ccbsa.wms.gateway.api.dto.CreateUserRequest;
import com.ccbsa.wms.gateway.api.dto.CreateUserResponse;
import com.ccbsa.wms.gateway.api.dto.OrderQueryResult;
import com.ccbsa.wms.gateway.api.dto.StockItemResponse;
import com.ccbsa.wms.gateway.api.fixture.repository.H2TestDataRepository;
import com.ccbsa.wms.gateway.api.fixture.repository.TestDataRepository;

/**
 * High-level manager for test data operations.
 * Provides convenient methods to get or create test data, checking the repository first.
 */
public class TestDataManager {

    private static final TestDataRepository repository = new H2TestDataRepository();

    static {
        repository.initialize();
    }

    /**
     * Get or create a location. Checks repository first, creates if not found.
     *
     * @param accessToken    the access token for API calls
     * @param tenantId       the tenant ID
     * @param requestBuilder supplier that creates the location request
     * @param creator        function that creates the location via API
     * @return the location (from repository or newly created)
     */
    public static CreateLocationResponse getOrCreateLocation(String accessToken, String tenantId,
                                                             Supplier<CreateLocationRequest> requestBuilder,
                                                             Function<CreateLocationRequest, CreateLocationResponse> creator) {
        CreateLocationRequest request = requestBuilder.get();
        // Try to find by code first
        Optional<CreateLocationResponse> existing = repository.findLocationByCode(request.getCode(), tenantId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new location
        CreateLocationResponse location = creator.apply(request);
        repository.saveLocation(location, tenantId);
        return location;
    }

    /**
     * Get or create a product. Checks repository first, creates if not found.
     *
     * @param accessToken    the access token for API calls
     * @param tenantId       the tenant ID
     * @param requestBuilder supplier that creates the product request
     * @param creator        function that creates the product via API
     * @return the product (from repository or newly created)
     */
    public static CreateProductResponse getOrCreateProduct(String accessToken, String tenantId,
                                                           Supplier<CreateProductRequest> requestBuilder,
                                                           Function<CreateProductRequest, CreateProductResponse> creator) {
        CreateProductRequest request = requestBuilder.get();
        // Try to find by code first
        Optional<CreateProductResponse> existing = repository.findProductByCode(request.getProductCode(), tenantId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new product
        CreateProductResponse product = creator.apply(request);
        repository.saveProduct(product, tenantId);
        return product;
    }

    /**
     * Get an existing location by type, or return empty if not found.
     *
     * @param type     the location type
     * @param tenantId the tenant ID
     * @return Optional containing the first location of the type, or empty
     */
    public static Optional<CreateLocationResponse> getLocationByType(String type, String tenantId) {
        List<CreateLocationResponse> locations = repository.findLocationsByType(type, tenantId);
        return locations.isEmpty() ? Optional.empty() : Optional.of(locations.get(0));
    }

    /**
     * Get an existing location by ID.
     *
     * @param locationId the location ID
     * @param tenantId   the tenant ID
     * @return Optional containing the location, or empty
     */
    public static Optional<CreateLocationResponse> getLocationById(String locationId, String tenantId) {
        return repository.findLocationById(locationId, tenantId);
    }

    /**
     * Get an existing product by code.
     *
     * @param productCode the product code
     * @param tenantId    the tenant ID
     * @return Optional containing the product, or empty
     */
    public static Optional<CreateProductResponse> getProductByCode(String productCode, String tenantId) {
        return repository.findProductByCode(productCode, tenantId);
    }

    /**
     * Get an existing product by ID.
     *
     * @param productId the product ID
     * @param tenantId the tenant ID
     * @return Optional containing the product, or empty
     */
    public static Optional<CreateProductResponse> getProductById(String productId, String tenantId) {
        return repository.findProductById(productId, tenantId);
    }

    /**
     * Save a location to the repository.
     *
     * @param location the location to save
     * @param tenantId the tenant ID
     */
    public static void saveLocation(CreateLocationResponse location, String tenantId) {
        repository.saveLocation(location, tenantId);
    }

    /**
     * Save a product to the repository.
     *
     * @param product  the product to save
     * @param tenantId the tenant ID
     */
    public static void saveProduct(CreateProductResponse product, String tenantId) {
        repository.saveProduct(product, tenantId);
    }

    /**
     * Get or create a user. Checks repository first for any existing user for the tenant, creates if not found.
     *
     * @param accessToken    the access token for API calls
     * @param tenantId       the tenant ID
     * @param requestBuilder supplier that creates the user request
     * @param creator        function that creates the user via API
     * @return the user (from repository or newly created)
     */
    public static CreateUserResponse getOrCreateUser(String accessToken, String tenantId,
                                                      Supplier<CreateUserRequest> requestBuilder,
                                                      Function<CreateUserRequest, CreateUserResponse> creator) {
        // Try to find any existing user for the tenant first
        Optional<CreateUserResponse> existing = repository.findAnyUserByTenant(tenantId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new user
        CreateUserRequest request = requestBuilder.get();
        CreateUserResponse user = creator.apply(request);
        repository.saveUser(user, tenantId);
        return user;
    }

    /**
     * Save a user to the repository.
     *
     * @param user     the user to save
     * @param tenantId the tenant ID
     */
    public static void saveUser(CreateUserResponse user, String tenantId) {
        repository.saveUser(user, tenantId);
    }

    /**
     * Save a consignment to the repository.
     *
     * @param consignment the consignment to save
     * @param tenantId    the tenant ID
     */
    public static void saveConsignment(CreateConsignmentResponse consignment, String tenantId) {
        repository.saveConsignment(consignment, tenantId);
    }

    /**
     * Save a tenant to the repository.
     *
     * @param tenant the tenant to save
     */
    public static void saveTenant(CreateTenantResponse tenant) {
        repository.saveTenant(tenant);
    }

    /**
     * Find all orders for a tenant.
     *
     * @param tenantId the tenant ID
     * @return list of orders for the tenant
     */
    public static List<OrderQueryResult> findAllOrders(String tenantId) {
        return repository.findAllOrders(tenantId);
    }

    /**
     * Save an order to the repository.
     *
     * @param order    the order to save
     * @param tenantId the tenant ID
     */
    public static void saveOrder(OrderQueryResult order, String tenantId) {
        repository.saveOrder(order, tenantId);
    }

    /**
     * Force creation of fresh data (bypasses repository check).
     * Use this when a test specifically needs new data.
     *
     * @param creator the function that creates the data
     * @param <T>    the type of data
     * @return the newly created data
     */
    public static <T> T createFresh(Supplier<T> creator) {
        return creator.get();
    }

    /**
     * Clear all test data for a specific tenant.
     *
     * @param tenantId the tenant ID
     */
    public static void clearTenantData(String tenantId) {
        repository.clearTenantData(tenantId);
    }

    /**
     * Clear all test data.
     */
    public static void clearAllData() {
        repository.clearAllData();
    }

    /**
     * Get the underlying repository instance.
     *
     * @return the TestDataRepository instance
     */
    public static TestDataRepository getRepository() {
        return repository;
    }

    /**
     * Validate that a location still exists in the system by checking the repository.
     * This is an optional health check that can be used to verify stored data is still valid.
     *
     * @param locationId the location ID to validate
     * @param tenantId  the tenant ID
     * @return true if location exists in repository, false otherwise
     */
    public static boolean validateLocationExists(String locationId, String tenantId) {
        return repository.findLocationById(locationId, tenantId).isPresent();
    }

    /**
     * Validate that a product still exists in the system by checking the repository.
     * This is an optional health check that can be used to verify stored data is still valid.
     *
     * @param productId the product ID to validate
     * @param tenantId the tenant ID
     * @return true if product exists in repository, false otherwise
     */
    public static boolean validateProductExists(String productId, String tenantId) {
        return repository.findProductById(productId, tenantId).isPresent();
    }

    // ==================== STOCK ITEM OPERATIONS ====================

    /**
     * Get the first available stock item for a product from the repository.
     * Filters out stock items with zero quantity.
     *
     * @param productId the product ID
     * @param tenantId  the tenant ID
     * @return Optional containing the first available stock item, or empty if none found
     */
    public static Optional<StockItemResponse> getStockItemByProductId(String productId, String tenantId) {
        List<StockItemResponse> stockItems = repository.findStockItemsByProductId(productId, tenantId);
        return stockItems.stream()
                .filter(item -> item.getQuantity() != null && item.getQuantity() > 0)
                .findFirst();
    }

    /**
     * Get all stock items for a product from the repository.
     *
     * @param productId the product ID
     * @param tenantId  the tenant ID
     * @return list of stock items for the product
     */
    public static List<StockItemResponse> getStockItemsByProductId(String productId, String tenantId) {
        return repository.findStockItemsByProductId(productId, tenantId);
    }

    /**
     * Get stock items by classification from the repository.
     *
     * @param classification the classification (NORMAL, EXPIRED, NEAR_EXPIRY, etc.)
     * @param tenantId       the tenant ID
     * @return list of stock items matching the classification
     */
    public static List<StockItemResponse> getStockItemsByClassification(String classification, String tenantId) {
        return repository.findStockItemsByClassification(classification, tenantId);
    }

    /**
     * Save a stock item to the repository.
     *
     * @param stockItem the stock item to save
     * @param tenantId  the tenant ID
     */
    public static void saveStockItem(StockItemResponse stockItem, String tenantId) {
        repository.saveStockItem(stockItem, tenantId);
    }

    /**
     * Save multiple stock items to the repository.
     *
     * @param stockItems the stock items to save
     * @param tenantId   the tenant ID
     */
    public static void saveStockItems(List<StockItemResponse> stockItems, String tenantId) {
        repository.saveStockItems(stockItems, tenantId);
    }
}
