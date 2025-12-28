package com.ccbsa.wms.stock.dataaccess.adapter;

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
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.application.service.port.repository.StockLevelThresholdRepository;
import com.ccbsa.wms.stock.domain.core.entity.StockLevelThreshold;
import com.ccbsa.wms.stock.domain.core.valueobject.StockLevelThresholdId;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * Cached Stock Level Threshold Repository Adapter.
 * <p>
 * MANDATORY: Decorates StockLevelThresholdRepositoryAdapter with Redis caching.
 * Implements cache-aside pattern for reads and write-through for writes.
 * <p>
 * Cache Configuration:
 * - Namespace: "stock-level-thresholds"
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
public class CachedStockLevelThresholdRepositoryAdapter extends CachedRepositoryDecorator<StockLevelThreshold, StockLevelThresholdId> implements StockLevelThresholdRepository {
    private final StockLevelThresholdRepositoryAdapter baseRepository;

    public CachedStockLevelThresholdRepositoryAdapter(StockLevelThresholdRepositoryAdapter baseRepository, RedisTemplate<String, Object> redisTemplate,
                                                      MeterRegistry meterRegistry) {
        super(baseRepository, redisTemplate, CacheNamespace.STOCK_LEVEL_THRESHOLDS.getValue(), Duration.ofMinutes(30), // TTL from config
                meterRegistry);
        this.baseRepository = baseRepository;
    }

    @Override
    public StockLevelThreshold save(StockLevelThreshold threshold) {
        // Write-through: Save to database + update cache
        StockLevelThreshold saved = baseRepository.save(threshold);

        if (saved.getTenantId() != null && saved.getId() != null) {
            saveWithCache(saved.getTenantId(), saved.getId().getValue(), saved, obj -> saved);
        }

        return saved;
    }

    @Override
    public Optional<StockLevelThreshold> findByIdAndTenantId(StockLevelThresholdId id, TenantId tenantId) {
        return findWithCache(tenantId, id.getValue(), entityId -> baseRepository.findByIdAndTenantId(id, tenantId));
    }

    @Override
    public Optional<StockLevelThreshold> findByTenantIdAndProductIdAndLocationId(TenantId tenantId, ProductId productId, LocationId locationId) {
        // This query is not cached as it's a lookup by product/location combination
        // Caching would require complex key generation and invalidation
        return baseRepository.findByTenantIdAndProductIdAndLocationId(tenantId, productId, locationId);
    }

    @Override
    public List<StockLevelThreshold> findByTenantId(TenantId tenantId) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByTenantId(tenantId);
    }

    @Override
    public List<StockLevelThreshold> findByTenantIdAndProductId(TenantId tenantId, ProductId productId) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByTenantIdAndProductId(tenantId, productId);
    }
}

