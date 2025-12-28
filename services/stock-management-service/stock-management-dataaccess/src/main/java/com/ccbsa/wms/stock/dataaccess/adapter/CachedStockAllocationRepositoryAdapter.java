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
import com.ccbsa.wms.stock.application.service.port.repository.StockAllocationRepository;
import com.ccbsa.wms.stock.domain.core.entity.StockAllocation;
import com.ccbsa.wms.stock.domain.core.valueobject.AllocationStatus;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAllocationId;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * Cached Stock Allocation Repository Adapter.
 * <p>
 * MANDATORY: Decorates StockAllocationRepositoryAdapter with Redis caching.
 * Implements cache-aside pattern for reads and write-through for writes.
 * <p>
 * Cache Configuration:
 * - Namespace: "stock-allocations"
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
public class CachedStockAllocationRepositoryAdapter extends CachedRepositoryDecorator<StockAllocation, StockAllocationId> implements StockAllocationRepository {
    private final StockAllocationRepositoryAdapter baseRepository;

    public CachedStockAllocationRepositoryAdapter(StockAllocationRepositoryAdapter baseRepository, RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        super(baseRepository, redisTemplate, CacheNamespace.STOCK_ALLOCATIONS.getValue(), Duration.ofMinutes(30), // TTL from config
                meterRegistry);
        this.baseRepository = baseRepository;
    }

    @Override
    public StockAllocation save(StockAllocation allocation) {
        // Write-through: Save to database + update cache
        StockAllocation saved = baseRepository.save(allocation);

        if (saved.getTenantId() != null && saved.getId() != null) {
            saveWithCache(saved.getTenantId(), saved.getId().getValue(), saved, obj -> saved);
        }

        return saved;
    }

    @Override
    public Optional<StockAllocation> findByIdAndTenantId(StockAllocationId id, TenantId tenantId) {
        return findWithCache(tenantId, id.getValue(), entityId -> baseRepository.findByIdAndTenantId(id, tenantId));
    }

    @Override
    public List<StockAllocation> findByTenantId(TenantId tenantId) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByTenantId(tenantId);
    }

    @Override
    public List<StockAllocation> findByTenantIdAndProductId(TenantId tenantId, ProductId productId) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByTenantIdAndProductId(tenantId, productId);
    }

    @Override
    public List<StockAllocation> findByTenantIdAndProductIdAndLocationId(TenantId tenantId, ProductId productId, LocationId locationId) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByTenantIdAndProductIdAndLocationId(tenantId, productId, locationId);
    }

    @Override
    public List<StockAllocation> findByStockItemId(StockItemId stockItemId) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByStockItemId(stockItemId);
    }

    @Override
    public List<StockAllocation> findByStockItemIdAndStatus(StockItemId stockItemId, AllocationStatus status) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByStockItemIdAndStatus(stockItemId, status);
    }

    @Override
    public List<StockAllocation> findByTenantIdAndReferenceId(TenantId tenantId, String referenceId) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByTenantIdAndReferenceId(tenantId, referenceId);
    }
}

