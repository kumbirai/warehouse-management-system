package com.ccbsa.wms.location.dataaccess.adapter;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.cache.decorator.CachedRepositoryDecorator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.application.service.port.repository.StockMovementRepository;
import com.ccbsa.wms.location.domain.core.entity.StockMovement;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.MovementStatus;
import com.ccbsa.wms.location.domain.core.valueobject.StockMovementId;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * Cached Stock Movement Repository Adapter.
 * <p>
 * MANDATORY: Decorates StockMovementRepositoryAdapter with Redis caching.
 * Implements cache-aside pattern for reads and write-through for writes.
 * <p>
 * Cache Configuration:
 * - Namespace: "stock-movements"
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
public class CachedStockMovementRepositoryAdapter extends CachedRepositoryDecorator<StockMovement, StockMovementId> implements StockMovementRepository {
    private final StockMovementRepositoryAdapter baseRepository;

    public CachedStockMovementRepositoryAdapter(StockMovementRepositoryAdapter baseRepository, RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        super(baseRepository, redisTemplate, CacheNamespace.STOCK_MOVEMENTS.getValue(), Duration.ofMinutes(30), // TTL from config
                meterRegistry);
        this.baseRepository = baseRepository;
    }

    @Override
    public StockMovement save(StockMovement movement) {
        // Write-through: Save to database + update cache
        StockMovement saved = baseRepository.save(movement);

        if (saved.getTenantId() != null && saved.getId() != null) {
            saveWithCache(saved.getTenantId(), saved.getId().getValue(), saved, obj -> saved);
        }

        return saved;
    }

    @Override
    public Optional<StockMovement> findByIdAndTenantId(StockMovementId id, TenantId tenantId) {
        return findWithCache(tenantId, id.getValue(), entityId -> baseRepository.findByIdAndTenantId(id, tenantId));
    }

    @Override
    public List<StockMovement> findByTenantId(TenantId tenantId) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByTenantId(tenantId);
    }

    @Override
    public List<StockMovement> findByTenantIdAndStockItemId(TenantId tenantId, String stockItemId) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByTenantIdAndStockItemId(tenantId, stockItemId);
    }

    @Override
    public List<StockMovement> findByTenantIdAndSourceLocationId(TenantId tenantId, LocationId sourceLocationId) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByTenantIdAndSourceLocationId(tenantId, sourceLocationId);
    }

    @Override
    public List<StockMovement> findByTenantIdAndDestinationLocationId(TenantId tenantId, LocationId destinationLocationId) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByTenantIdAndDestinationLocationId(tenantId, destinationLocationId);
    }

    @Override
    public List<StockMovement> findByTenantIdAndStatus(TenantId tenantId, MovementStatus status) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByTenantIdAndStatus(tenantId, status);
    }
}

