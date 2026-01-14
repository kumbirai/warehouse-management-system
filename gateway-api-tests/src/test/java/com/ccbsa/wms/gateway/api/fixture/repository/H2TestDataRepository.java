package com.ccbsa.wms.gateway.api.fixture.repository;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ccbsa.wms.gateway.api.dto.CreateConsignmentResponse;
import com.ccbsa.wms.gateway.api.dto.CreateLocationResponse;
import com.ccbsa.wms.gateway.api.dto.CreateProductResponse;
import com.ccbsa.wms.gateway.api.dto.CreateTenantResponse;
import com.ccbsa.wms.gateway.api.dto.CreateUserResponse;
import com.ccbsa.wms.gateway.api.dto.OrderQueryResult;
import com.ccbsa.wms.gateway.api.dto.StockItemResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * H2-based implementation of TestDataRepository.
 * Uses file-based persistence to store test data across test runs.
 */
public class H2TestDataRepository implements TestDataRepository {

    private static final String DB_URL = "jdbc:h2:file:./src/test/resources/test-data/test-data;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static volatile boolean initialized = false;
    private static final Object initLock = new Object();

    /**
     * Initialize the repository and create schema if needed.
     */
    @Override
    public void initialize() {
        if (initialized) {
            return;
        }

        synchronized (initLock) {
            if (initialized) {
                return;
            }

            try {
                // Ensure target directory exists
                File dbDir = new File("./src/test/resources/test-data");
                if (!dbDir.exists()) {
                    dbDir.mkdirs();
                }

                // Create schema
                try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                    // Locations table
                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS test_locations (
                            id VARCHAR(255) PRIMARY KEY,
                            location_id VARCHAR(255) UNIQUE NOT NULL,
                            location_code VARCHAR(255) NOT NULL,
                            location_type VARCHAR(50) NOT NULL,
                            parent_location_id VARCHAR(255),
                            tenant_id VARCHAR(255) NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            data_json CLOB,
                            UNIQUE(tenant_id, location_code)
                        )
                        """);

                    // Products table
                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS test_products (
                            id VARCHAR(255) PRIMARY KEY,
                            product_id VARCHAR(255) UNIQUE NOT NULL,
                            product_code VARCHAR(255) NOT NULL,
                            tenant_id VARCHAR(255) NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            data_json CLOB,
                            UNIQUE(tenant_id, product_code)
                        )
                        """);

                    // Orders table
                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS test_orders (
                            id VARCHAR(255) PRIMARY KEY,
                            order_number VARCHAR(255) NOT NULL,
                            tenant_id VARCHAR(255) NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            data_json CLOB,
                            UNIQUE(tenant_id, order_number)
                        )
                        """);

                    // Users table
                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS test_users (
                            id VARCHAR(255) PRIMARY KEY,
                            user_id VARCHAR(255) UNIQUE NOT NULL,
                            tenant_id VARCHAR(255) NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            data_json CLOB
                        )
                        """);

                    // Consignments table
                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS test_consignments (
                            id VARCHAR(255) PRIMARY KEY,
                            consignment_id VARCHAR(255) UNIQUE NOT NULL,
                            tenant_id VARCHAR(255) NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            data_json CLOB
                        )
                        """);

                    // Tenants table
                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS test_tenants (
                            id VARCHAR(255) PRIMARY KEY,
                            tenant_id VARCHAR(255) UNIQUE NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            data_json CLOB
                        )
                        """);

                    // Stock items table
                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS test_stock_items (
                            id VARCHAR(255) PRIMARY KEY,
                            stock_item_id VARCHAR(255) UNIQUE NOT NULL,
                            product_id VARCHAR(255) NOT NULL,
                            product_code VARCHAR(255),
                            location_id VARCHAR(255),
                            quantity INTEGER,
                            expiration_date DATE,
                            classification VARCHAR(50),
                            tenant_id VARCHAR(255) NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            data_json CLOB
                        )
                        """);

                    conn.commit();
                }

                initialized = true;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to initialize test data repository", e);
            }
        }
    }

    /**
     * Get a database connection.
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Serialize an object to JSON string.
     */
    private String serializeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    /**
     * Deserialize JSON string to object.
     */
    private <T> T deserializeFromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON to object", e);
        }
    }

    // ==================== LOCATION OPERATIONS ====================

    @Override
    public synchronized Optional<CreateLocationResponse> findLocationByCode(String code, String tenantId) {
        initialize();
        String sql = "SELECT data_json FROM test_locations WHERE location_code = ? AND tenant_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            stmt.setString(2, tenantId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("data_json");
                    return Optional.of(deserializeFromJson(json, CreateLocationResponse.class));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find location by code", e);
        }
        return Optional.empty();
    }

    @Override
    public synchronized Optional<CreateLocationResponse> findLocationById(String locationId, String tenantId) {
        initialize();
        String sql = "SELECT data_json FROM test_locations WHERE location_id = ? AND tenant_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, locationId);
            stmt.setString(2, tenantId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("data_json");
                    return Optional.of(deserializeFromJson(json, CreateLocationResponse.class));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find location by ID", e);
        }
        return Optional.empty();
    }

    @Override
    public synchronized void saveLocation(CreateLocationResponse location, String tenantId) {
        initialize();
        String sql = """
            INSERT INTO test_locations (id, location_id, location_code, location_type, parent_location_id, tenant_id, data_json)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE data_json = VALUES(data_json)
            """;
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, location.getLocationId());
            stmt.setString(3, location.getCode());
            stmt.setString(4, location.getType());
            stmt.setString(5, null); // parent_location_id not in response, would need to be tracked separately
            stmt.setString(6, tenantId);
            stmt.setString(7, serializeToJson(location));
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            // H2 doesn't support ON DUPLICATE KEY UPDATE, use MERGE instead
            try (Connection conn = getConnection()) {
                String mergeSql = """
                    MERGE INTO test_locations (id, location_id, location_code, location_type, parent_location_id, tenant_id, data_json)
                    KEY (location_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;
                try (PreparedStatement stmt = conn.prepareStatement(mergeSql)) {
                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, location.getLocationId());
                    stmt.setString(3, location.getCode());
                    stmt.setString(4, location.getType());
                    stmt.setString(5, null);
                    stmt.setString(6, tenantId);
                    stmt.setString(7, serializeToJson(location));
                    stmt.executeUpdate();
                    conn.commit();
                }
            } catch (SQLException ex) {
                throw new RuntimeException("Failed to save location", ex);
            }
        }
    }

    @Override
    public synchronized List<CreateLocationResponse> findLocationsByType(String type, String tenantId) {
        initialize();
        List<CreateLocationResponse> locations = new ArrayList<>();
        String sql = "SELECT data_json FROM test_locations WHERE location_type = ? AND tenant_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type);
            stmt.setString(2, tenantId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String json = rs.getString("data_json");
                    locations.add(deserializeFromJson(json, CreateLocationResponse.class));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find locations by type", e);
        }
        return locations;
    }

    // ==================== PRODUCT OPERATIONS ====================

    @Override
    public synchronized Optional<CreateProductResponse> findProductByCode(String productCode, String tenantId) {
        initialize();
        String sql = "SELECT data_json FROM test_products WHERE product_code = ? AND tenant_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, productCode);
            stmt.setString(2, tenantId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("data_json");
                    return Optional.of(deserializeFromJson(json, CreateProductResponse.class));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find product by code", e);
        }
        return Optional.empty();
    }

    @Override
    public synchronized Optional<CreateProductResponse> findProductById(String productId, String tenantId) {
        initialize();
        String sql = "SELECT data_json FROM test_products WHERE product_id = ? AND tenant_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, productId);
            stmt.setString(2, tenantId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("data_json");
                    return Optional.of(deserializeFromJson(json, CreateProductResponse.class));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find product by ID", e);
        }
        return Optional.empty();
    }

    @Override
    public synchronized void saveProduct(CreateProductResponse product, String tenantId) {
        initialize();
        String sql = """
            MERGE INTO test_products (id, product_id, product_code, tenant_id, data_json)
            KEY (product_id)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, product.getProductId());
            stmt.setString(3, product.getProductCode());
            stmt.setString(4, tenantId);
            stmt.setString(5, serializeToJson(product));
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save product", e);
        }
    }

    // ==================== ORDER OPERATIONS ====================

    @Override
    public synchronized Optional<OrderQueryResult> findOrderByNumber(String orderNumber, String tenantId) {
        initialize();
        String sql = "SELECT data_json FROM test_orders WHERE order_number = ? AND tenant_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, orderNumber);
            stmt.setString(2, tenantId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("data_json");
                    return Optional.of(deserializeFromJson(json, OrderQueryResult.class));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find order by number", e);
        }
        return Optional.empty();
    }

    @Override
    public synchronized List<OrderQueryResult> findAllOrders(String tenantId) {
        initialize();
        String sql = "SELECT data_json FROM test_orders WHERE tenant_id = ?";
        List<OrderQueryResult> orders = new java.util.ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tenantId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String json = rs.getString("data_json");
                    orders.add(deserializeFromJson(json, OrderQueryResult.class));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all orders", e);
        }
        return orders;
    }

    @Override
    public synchronized void saveOrder(OrderQueryResult order, String tenantId) {
        initialize();
        String sql = """
            MERGE INTO test_orders (id, order_number, tenant_id, data_json)
            KEY (tenant_id, order_number)
            VALUES (?, ?, ?, ?)
            """;
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, order.getOrderNumber());
            stmt.setString(3, tenantId);
            stmt.setString(4, serializeToJson(order));
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save order", e);
        }
    }

    // ==================== USER OPERATIONS ====================

    @Override
    public synchronized Optional<CreateUserResponse> findUserById(String userId, String tenantId) {
        initialize();
        String sql = "SELECT data_json FROM test_users WHERE user_id = ? AND tenant_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, tenantId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("data_json");
                    return Optional.of(deserializeFromJson(json, CreateUserResponse.class));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by ID", e);
        }
        return Optional.empty();
    }

    @Override
    public synchronized Optional<CreateUserResponse> findAnyUserByTenant(String tenantId) {
        initialize();
        String sql = "SELECT data_json FROM test_users WHERE tenant_id = ? LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tenantId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("data_json");
                    return Optional.of(deserializeFromJson(json, CreateUserResponse.class));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by tenant", e);
        }
        return Optional.empty();
    }

    @Override
    public synchronized void saveUser(CreateUserResponse user, String tenantId) {
        initialize();
        String sql = """
            MERGE INTO test_users (id, user_id, tenant_id, data_json)
            KEY (user_id)
            VALUES (?, ?, ?, ?)
            """;
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, user.getUserId());
            stmt.setString(3, tenantId);
            stmt.setString(4, serializeToJson(user));
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user", e);
        }
    }

    // ==================== CONSIGNMENT OPERATIONS ====================

    @Override
    public synchronized Optional<CreateConsignmentResponse> findConsignmentById(String consignmentId, String tenantId) {
        initialize();
        String sql = "SELECT data_json FROM test_consignments WHERE consignment_id = ? AND tenant_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, consignmentId);
            stmt.setString(2, tenantId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("data_json");
                    return Optional.of(deserializeFromJson(json, CreateConsignmentResponse.class));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find consignment by ID", e);
        }
        return Optional.empty();
    }

    @Override
    public synchronized void saveConsignment(CreateConsignmentResponse consignment, String tenantId) {
        initialize();
        String sql = """
            MERGE INTO test_consignments (id, consignment_id, tenant_id, data_json)
            KEY (consignment_id)
            VALUES (?, ?, ?, ?)
            """;
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, consignment.getConsignmentId());
            stmt.setString(3, tenantId);
            stmt.setString(4, serializeToJson(consignment));
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save consignment", e);
        }
    }

    // ==================== STOCK ITEM OPERATIONS ====================

    @Override
    public synchronized Optional<StockItemResponse> findStockItemById(String stockItemId, String tenantId) {
        initialize();
        String sql = "SELECT data_json FROM test_stock_items WHERE stock_item_id = ? AND tenant_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, stockItemId);
            stmt.setString(2, tenantId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("data_json");
                    return Optional.of(deserializeFromJson(json, StockItemResponse.class));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find stock item by ID", e);
        }
        return Optional.empty();
    }

    @Override
    public synchronized List<StockItemResponse> findStockItemsByProductId(String productId, String tenantId) {
        initialize();
        List<StockItemResponse> stockItems = new ArrayList<>();
        String sql = "SELECT data_json FROM test_stock_items WHERE product_id = ? AND tenant_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, productId);
            stmt.setString(2, tenantId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String json = rs.getString("data_json");
                    stockItems.add(deserializeFromJson(json, StockItemResponse.class));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find stock items by product ID", e);
        }
        return stockItems;
    }

    @Override
    public synchronized List<StockItemResponse> findStockItemsByProductCode(String productCode, String tenantId) {
        initialize();
        List<StockItemResponse> stockItems = new ArrayList<>();
        String sql = "SELECT data_json FROM test_stock_items WHERE product_code = ? AND tenant_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, productCode);
            stmt.setString(2, tenantId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String json = rs.getString("data_json");
                    stockItems.add(deserializeFromJson(json, StockItemResponse.class));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find stock items by product code", e);
        }
        return stockItems;
    }

    @Override
    public synchronized List<StockItemResponse> findStockItemsByClassification(String classification, String tenantId) {
        initialize();
        List<StockItemResponse> stockItems = new ArrayList<>();
        String sql = "SELECT data_json FROM test_stock_items WHERE classification = ? AND tenant_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, classification);
            stmt.setString(2, tenantId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String json = rs.getString("data_json");
                    stockItems.add(deserializeFromJson(json, StockItemResponse.class));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find stock items by classification", e);
        }
        return stockItems;
    }

    @Override
    public synchronized void saveStockItem(StockItemResponse stockItem, String tenantId) {
        initialize();
        // Get product code from product if available
        String productCode = null;
        if (stockItem.getProductId() != null) {
            Optional<CreateProductResponse> product = findProductById(stockItem.getProductId(), tenantId);
            if (product.isPresent()) {
                productCode = product.get().getProductCode();
            }
        }

        String sql = """
            MERGE INTO test_stock_items (id, stock_item_id, product_id, product_code, location_id, quantity, expiration_date, classification, tenant_id, data_json)
            KEY (stock_item_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, stockItem.getStockItemId());
            stmt.setString(3, stockItem.getProductId());
            stmt.setString(4, productCode);
            stmt.setString(5, stockItem.getLocationId());
            stmt.setObject(6, stockItem.getQuantity());
            stmt.setObject(7, stockItem.getExpirationDate() != null ? java.sql.Date.valueOf(stockItem.getExpirationDate()) : null);
            stmt.setString(8, stockItem.getClassification());
            stmt.setString(9, tenantId);
            stmt.setString(10, serializeToJson(stockItem));
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save stock item", e);
        }
    }

    @Override
    public synchronized void saveStockItems(List<StockItemResponse> stockItems, String tenantId) {
        for (StockItemResponse stockItem : stockItems) {
            saveStockItem(stockItem, tenantId);
        }
    }

    // ==================== TENANT OPERATIONS ====================

    @Override
    public synchronized Optional<CreateTenantResponse> findTenantById(String tenantId) {
        initialize();
        String sql = "SELECT data_json FROM test_tenants WHERE tenant_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tenantId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("data_json");
                    return Optional.of(deserializeFromJson(json, CreateTenantResponse.class));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find tenant by ID", e);
        }
        return Optional.empty();
    }

    @Override
    public synchronized void saveTenant(CreateTenantResponse tenant) {
        initialize();
        String sql = """
            MERGE INTO test_tenants (id, tenant_id, data_json)
            KEY (tenant_id)
            VALUES (?, ?, ?)
            """;
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, tenant.getTenantId());
            stmt.setString(3, serializeToJson(tenant));
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save tenant", e);
        }
    }

    // ==================== CLEANUP OPERATIONS ====================

    @Override
    public synchronized void clearTenantData(String tenantId) {
        initialize();
        try (Connection conn = getConnection()) {
            String[] tables = { "test_locations", "test_products", "test_orders", "test_users", "test_consignments", "test_stock_items" };
            for (String table : tables) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM " + table + " WHERE tenant_id = ?")) {
                    stmt.setString(1, tenantId);
                    stmt.executeUpdate();
                }
            }
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear tenant data", e);
        }
    }

    @Override
    public synchronized void clearAllData() {
        initialize();
        try (Connection conn = getConnection()) {
            String[] tables = { "test_locations", "test_products", "test_orders", "test_users", "test_consignments", "test_stock_items", "test_tenants" };
            for (String table : tables) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DELETE FROM " + table);
                }
            }
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear all data", e);
        }
    }
}
