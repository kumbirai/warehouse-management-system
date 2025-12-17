package com.ccbsa.wms.product.application.service.query;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.product.application.service.query.dto.ProductInfo;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Cache Component: ProductBarcodeCache
 * <p>
 * Caches product barcode lookups for performance.
 * <p>
 * Cache Strategy: - Key: {tenantId}:{barcode} - TTL: 1 hour - Maximum size: 10,000 entries
 */
@Component
public class ProductBarcodeCache {
    private static final int MAX_SIZE = 10_000;
    private static final int TTL_HOURS = 1;

    private final Cache<String, ProductInfo> cache;

    public ProductBarcodeCache() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(MAX_SIZE)
                .expireAfterWrite(TTL_HOURS, TimeUnit.HOURS)
                .build();
    }

    /**
     * Gets product info from cache.
     *
     * @param barcode  Barcode value
     * @param tenantId Tenant identifier
     * @return Optional ProductInfo if found in cache
     */
    public Optional<ProductInfo> get(String barcode, TenantId tenantId) {
        String key = buildKey(barcode, tenantId);
        return Optional.ofNullable(cache.getIfPresent(key));
    }

    /**
     * Builds cache key from barcode and tenant ID.
     *
     * @param barcode  Barcode value
     * @param tenantId Tenant identifier
     * @return Cache key
     */
    private String buildKey(String barcode, TenantId tenantId) {
        return String.format("%s:%s", tenantId.getValue(), barcode);
    }

    /**
     * Puts product info into cache.
     *
     * @param barcode     Barcode value
     * @param tenantId    Tenant identifier
     * @param productInfo Product information to cache
     */
    public void put(String barcode, TenantId tenantId, ProductInfo productInfo) {
        String key = buildKey(barcode, tenantId);
        cache.put(key, productInfo);
    }

    /**
     * Invalidates cache entry for a barcode.
     *
     * @param barcode  Barcode value
     * @param tenantId Tenant identifier
     */
    public void invalidate(String barcode, TenantId tenantId) {
        String key = buildKey(barcode, tenantId);
        cache.invalidate(key);
    }

    /**
     * Clears all cache entries.
     */
    public void clear() {
        cache.invalidateAll();
    }
}

