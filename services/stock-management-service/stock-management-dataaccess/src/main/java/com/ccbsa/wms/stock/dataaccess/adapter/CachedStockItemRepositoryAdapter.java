package com.ccbsa.wms.stock.dataaccess.adapter;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.cache.decorator.CachedRepositoryDecorator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.stock.application.service.port.repository.StockItemRepository;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stock.domain.core.valueobject.StockItemId;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Cached Stock Item Repository Adapter.
 * <p>
 * MANDATORY: Decorates StockItemRepositoryAdapter with Redis caching.
 * Implements cache-aside pattern for reads and write-through for writes.
 * <p>
 * Cache Configuration:
 * - Namespace: "stock-items" (needs to be added to CacheNamespace enum)
 * - TTL: Configured via application.yml (default: 30 minutes)
 * - Eviction: LRU (configured in Redis)
 * <p>
 * Spring Configuration:
 * - @Primary: Ensures this adapter is injected instead of base adapter
 * - @Repository: Marks as Spring Data repository component
 */
@Repository
@Primary
public class CachedStockItemRepositoryAdapter extends CachedRepositoryDecorator<StockItem, StockItemId> implements StockItemRepository {

    private static final Logger log = LoggerFactory.getLogger(CachedStockItemRepositoryAdapter.class);

    private final StockItemRepositoryAdapter baseRepository;

    public CachedStockItemRepositoryAdapter(StockItemRepositoryAdapter baseRepository, RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        super(baseRepository, redisTemplate, CacheNamespace.STOCK_ITEMS.getValue(), Duration.ofMinutes(30), // TTL from config
                meterRegistry);
        this.baseRepository = baseRepository;
    }

    @Override
    public StockItem save(StockItem stockItem) {
        // Write-through: Save to database + update cache
        StockItem saved = baseRepository.save(stockItem);

        if (saved.getTenantId() != null && saved.getId() != null) {
            saveWithCache(saved.getTenantId(), saved.getId().getValue(), saved, obj -> saved // Already saved
            );
        }

        return saved;
    }

    @Override
    public Optional<StockItem> findById(StockItemId stockItemId, TenantId tenantId) {
        return findWithCache(tenantId, stockItemId.getValue(), entityId -> baseRepository.findById(stockItemId, tenantId));
    }

    @Override
    public List<StockItem> findByConsignmentId(ConsignmentId consignmentId, TenantId tenantId) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByConsignmentId(consignmentId, tenantId);
    }

    @Override
    public List<StockItem> findByClassification(StockClassification classification, TenantId tenantId) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByClassification(classification, tenantId);
    }

    @Override
    public boolean existsById(StockItemId stockItemId, TenantId tenantId) {
        // Existence checks are fast, not cached
        return baseRepository.existsById(stockItemId, tenantId);
    }
}

