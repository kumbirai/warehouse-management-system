package com.ccbsa.common.cache.key;

/**
 * Cache Namespace Constants.
 * <p>
 * Defines standardized cache namespaces for all services. Ensures consistency in cache key prefixes.
 * <p>
 * Usage:
 * <pre>
 * String cacheKey = CacheKeyGenerator.forEntity(
 *     tenantId,
 *     CacheNamespace.USERS.getValue(),
 *     userId
 * );
 * </pre>
 */
public enum CacheNamespace {

    // User Service
    USERS("users"), USER_ROLES("user-roles"), USER_PERMISSIONS("user-permissions"),

    // Tenant Service
    TENANTS("tenants"), TENANT_CONFIG("tenant-config"),

    // Product Service
    PRODUCTS("products"), PRODUCT_CATEGORIES("product-categories"),

    // Stock Management Service
    STOCK_CONSIGNMENTS("stock-consignments"), STOCK_ITEMS("stock-items"), STOCK_LEVELS("stock-levels"), STOCK_ALLOCATIONS("stock-allocations"),
    STOCK_ADJUSTMENTS("stock-adjustments"), STOCK_LEVEL_THRESHOLDS("stock-level-thresholds"), RESTOCK_REQUESTS("restock-requests"),

    // Location Management Service
    LOCATIONS("locations"), ZONES("zones"), STOCK_MOVEMENTS("stock-movements"),

    // Notification Service
    NOTIFICATIONS("notifications"),

    // Integration Service
    INTEGRATION_CONFIGS("integration-configs"),

    // Global (Cross-Tenant)
    GLOBAL_TENANT_METADATA("global-tenant-metadata");

    private final String value;

    CacheNamespace(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
