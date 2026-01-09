package com.ccbsa.wms.picking.dataaccess.adapter;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.cache.decorator.CachedRepositoryDecorator;
import com.ccbsa.common.domain.valueobject.LoadNumber;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.application.service.port.repository.LoadRepository;
import com.ccbsa.wms.picking.domain.core.entity.Load;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadStatus;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * Cached Load Repository Adapter.
 * <p>
 * MANDATORY: Decorates LoadRepositoryAdapter with Redis caching.
 */
@Repository
@Primary
@Slf4j
public class CachedLoadRepositoryAdapter extends CachedRepositoryDecorator<Load, LoadId> implements LoadRepository {
    private final LoadRepositoryAdapter baseRepository;

    public CachedLoadRepositoryAdapter(LoadRepositoryAdapter baseRepository, RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        super(baseRepository, redisTemplate, "loads", Duration.ofMinutes(30), meterRegistry);
        this.baseRepository = baseRepository;
    }

    @Override
    public void save(Load load) {
        baseRepository.save(load);
        if (load.getTenantId() != null && load.getId() != null) {
            UUID entityId = load.getId().getValue();
            if (entityId != null) {
                saveWithCache(load.getTenantId(), entityId, load, obj -> load);
            } else {
                log.warn("Cannot cache load with null entityId. LoadId: {}", load.getId());
            }
        }
    }

    @Override
    public Optional<Load> findByIdAndTenantId(LoadId id, TenantId tenantId) {
        return findWithCache(tenantId, id.getValue(), entityId -> baseRepository.findByIdAndTenantId(id, tenantId));
    }

    @Override
    public Optional<Load> findByLoadNumberAndTenantId(LoadNumber loadNumber, TenantId tenantId) {
        // Load number lookups are less frequent, not cached
        return baseRepository.findByLoadNumberAndTenantId(loadNumber, tenantId);
    }

    @Override
    public List<Load> findByTenantId(TenantId tenantId, LoadStatus status, int page, int size) {
        // Collections NOT cached
        return baseRepository.findByTenantId(tenantId, status, page, size);
    }

    @Override
    public Optional<PickingListId> findPickingListIdByLoadId(LoadId loadId, TenantId tenantId) {
        // Not cached - direct query
        return baseRepository.findPickingListIdByLoadId(loadId, tenantId);
    }
}
