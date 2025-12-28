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
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.application.service.port.repository.StockAdjustmentRepository;
import com.ccbsa.wms.stock.domain.core.entity.StockAdjustment;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAdjustmentId;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * Cached Stock Adjustment Repository Adapter.
 * <p>
 * MANDATORY: Decorates StockAdjustmentRepositoryAdapter with Redis caching.
 * Implements cache-aside pattern for reads and write-through for writes.
 * <p>
 * Cache Configuration:
 * - Namespace: "stock-adjustments"
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
public class CachedStockAdjustmentRepositoryAdapter extends CachedRepositoryDecorator<StockAdjustment, StockAdjustmentId> implements StockAdjustmentRepository {
    private final StockAdjustmentRepositoryAdapter baseRepository;

    public CachedStockAdjustmentRepositoryAdapter(StockAdjustmentRepositoryAdapter baseRepository, RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        super(baseRepository, redisTemplate, CacheNamespace.STOCK_ADJUSTMENTS.getValue(), Duration.ofMinutes(30), // TTL from config
                meterRegistry);
        this.baseRepository = baseRepository;
    }

    @Override
    public StockAdjustment save(StockAdjustment adjustment) {
        // Write-through: Save to database + update cache
        StockAdjustment saved = baseRepository.save(adjustment);

        if (saved.getTenantId() != null && saved.getId() != null) {
            saveWithCache(saved.getTenantId(), saved.getId().getValue(), saved, obj -> saved);
        }

        return saved;
    }

    @Override
    public Optional<StockAdjustment> findByIdAndTenantId(StockAdjustmentId id, TenantId tenantId) {
        return findWithCache(tenantId, id.getValue(), entityId -> baseRepository.findByIdAndTenantId(id, tenantId));
    }

    @Override
    public List<StockAdjustment> findByTenantId(TenantId tenantId) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByTenantId(tenantId);
    }

    @Override
    public List<StockAdjustment> findByTenantIdAndProductId(TenantId tenantId, ProductId productId) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByTenantIdAndProductId(tenantId, productId);
    }

    @Override
    public List<StockAdjustment> findByTenantIdAndProductIdAndLocationId(TenantId tenantId, ProductId productId, LocationId locationId) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByTenantIdAndProductIdAndLocationId(tenantId, productId, locationId);
    }

    @Override
    public List<StockAdjustment> findByTenantIdAndStockItemId(TenantId tenantId, StockItemId stockItemId) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByTenantIdAndStockItemId(tenantId, stockItemId);
    }
}

