package com.ccbsa.wms.picking.dataaccess.adapter;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.cache.decorator.CachedRepositoryDecorator;
import com.ccbsa.common.cache.key.CacheKeyGenerator;
import com.ccbsa.common.domain.AggregateRoot;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.application.service.port.repository.PickingListRepository;
import com.ccbsa.wms.picking.domain.core.entity.PickingList;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListStatus;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * Cached PickingList Repository Adapter.
 * <p>
 * MANDATORY: Decorates PickingListRepositoryAdapter with Redis caching.
 * Implements cache-aside pattern for reads and write-through for writes.
 */
@Repository
@Primary
@Slf4j
public class CachedPickingListRepositoryAdapter extends CachedRepositoryDecorator<PickingList, PickingListId> implements PickingListRepository {
    private final PickingListRepositoryAdapter baseRepository;

    public CachedPickingListRepositoryAdapter(PickingListRepositoryAdapter baseRepository, RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        super(baseRepository, redisTemplate, "picking-lists", Duration.ofMinutes(30), meterRegistry);
        this.baseRepository = baseRepository;
    }

    @Override
    public void save(PickingList pickingList) {
        // Save to database first (source of truth)
        baseRepository.save(pickingList);
        
        // Update cache after database save (write-through pattern)
        // We update cache directly here since we've already saved to database
        if (pickingList.getTenantId() != null && pickingList.getId() != null) {
            UUID entityId = pickingList.getId().getValue();
            if (entityId != null) {
                String cacheKey = CacheKeyGenerator.forEntity(pickingList.getTenantId(), "picking-lists", entityId);
                try {
                    // Clear domain events before caching (domain events are transient)
                    if (pickingList instanceof AggregateRoot) {
                        ((AggregateRoot<?>) pickingList).clearDomainEvents();
                    }
                    // Update cache with saved entity
                    redisTemplate.opsForValue().set(cacheKey, pickingList, Duration.ofMinutes(30));
                    log.trace("Cache updated (write-through) for picking list: {}", entityId);
                } catch (Exception e) {
                    // Cache update failure shouldn't break the application
                    log.warn("Cache update failed for picking list: {}. Error: {}", entityId, e.getMessage());
                }
            } else {
                log.warn("Cannot cache picking list with null entityId. PickingListId: {}", pickingList.getId());
            }
        }
    }
    
    /**
     * Evicts cache entry for a picking list.
     * Useful for explicit cache invalidation when status changes.
     *
     * @param pickingListId Picking list ID
     * @param tenantId Tenant ID
     */
    public void evictCache(PickingListId pickingListId, TenantId tenantId) {
        if (pickingListId != null && pickingListId.getValue() != null && tenantId != null) {
            evictFromCache(tenantId, pickingListId.getValue());
        }
    }

    @Override
    public Optional<PickingList> findByIdAndTenantId(PickingListId id, TenantId tenantId) {
        return findWithCache(tenantId, id.getValue(), entityId -> baseRepository.findByIdAndTenantId(id, tenantId));
    }

    @Override
    public List<PickingList> findByTenantId(TenantId tenantId, PickingListStatus status, int page, int size) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByTenantId(tenantId, status, page, size);
    }

    @Override
    public long countByTenantId(TenantId tenantId, PickingListStatus status) {
        // Count queries are dynamic, not cached
        return baseRepository.countByTenantId(tenantId, status);
    }
}
