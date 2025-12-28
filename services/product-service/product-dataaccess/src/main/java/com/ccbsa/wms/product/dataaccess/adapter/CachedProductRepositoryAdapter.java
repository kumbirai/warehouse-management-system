package com.ccbsa.wms.product.dataaccess.adapter;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.cache.decorator.CachedRepositoryDecorator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.product.application.service.port.repository.ProductRepository;
import com.ccbsa.wms.product.domain.core.entity.Product;
import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * Cached Product Repository Adapter.
 * <p>
 * MANDATORY: Decorates ProductRepositoryAdapter with Redis caching.
 * Implements cache-aside pattern for reads and write-through for writes.
 * <p>
 * Cache Configuration:
 * - Namespace: "products"
 * - TTL: Configured via application.yml (default: 30 minutes)
 * - Eviction: LRU (configured in Redis)
 * <p>
 * Spring Configuration:
 * - @Primary: Ensures this adapter is injected instead of base adapter
 * - @Repository: Marks as Spring Data repository component
 */
@Repository
@Primary
@Slf4j
public class CachedProductRepositoryAdapter extends CachedRepositoryDecorator<Product, ProductId> implements ProductRepository {
    private final ProductRepositoryAdapter baseRepository;

    public CachedProductRepositoryAdapter(ProductRepositoryAdapter baseRepository, RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        super(baseRepository, redisTemplate, CacheNamespace.PRODUCTS.getValue(), Duration.ofMinutes(30), // TTL from config
                meterRegistry);
        this.baseRepository = baseRepository;
    }

    @Override
    public Product save(Product product) {
        // Write-through: Save to database + update cache
        Product saved = baseRepository.save(product);

        if (saved.getTenantId() != null && saved.getId() != null) {
            saveWithCache(saved.getTenantId(), saved.getId().getValue(), saved, obj -> saved // Already saved
            );
        }

        return saved;
    }

    @Override
    public Optional<Product> findByIdAndTenantId(ProductId id, TenantId tenantId) {
        return findWithCache(tenantId, id.getValue(), entityId -> baseRepository.findByIdAndTenantId(id, tenantId));
    }

    @Override
    public Optional<Product> findByProductCodeAndTenantId(ProductCode productCode, TenantId tenantId) {
        // Product code lookups are less frequent, not cached
        return baseRepository.findByProductCodeAndTenantId(productCode, tenantId);
    }

    @Override
    public boolean existsByProductCodeAndTenantId(ProductCode productCode, TenantId tenantId) {
        // Existence checks are fast, not cached
        return baseRepository.existsByProductCodeAndTenantId(productCode, tenantId);
    }

    @Override
    public boolean existsByBarcodeAndTenantId(ProductBarcode barcode, TenantId tenantId) {
        // Existence checks are fast, not cached
        return baseRepository.existsByBarcodeAndTenantId(barcode, tenantId);
    }

    @Override
    public boolean existsByBarcodeAndTenantIdExcludingProduct(ProductBarcode barcode, TenantId tenantId, ProductId excludeProductId) {
        // Existence checks are fast, not cached
        return baseRepository.existsByBarcodeAndTenantIdExcludingProduct(barcode, tenantId, excludeProductId);
    }

    @Override
    public Optional<Product> findByBarcodeAndTenantId(String barcode, TenantId tenantId) {
        // Barcode lookups are less frequent, not cached
        return baseRepository.findByBarcodeAndTenantId(barcode, tenantId);
    }

    @Override
    public List<Product> findByTenantId(TenantId tenantId) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByTenantId(tenantId);
    }

    @Override
    public List<Product> findByTenantIdAndCategory(TenantId tenantId, String category) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByTenantIdAndCategory(tenantId, category);
    }

    @Override
    public List<Product> findByTenantIdWithFilters(TenantId tenantId, String category, String brand, String search, int page, int size) {
        // Filtered queries are dynamic, not cached
        return baseRepository.findByTenantIdWithFilters(tenantId, category, brand, search, page, size);
    }

    @Override
    public long countByTenantIdWithFilters(TenantId tenantId, String category, String brand, String search) {
        // Count queries are dynamic, not cached
        return baseRepository.countByTenantIdWithFilters(tenantId, category, brand, search);
    }
}

