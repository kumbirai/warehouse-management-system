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
import com.ccbsa.wms.stock.application.service.port.repository.RestockRequestRepository;
import com.ccbsa.wms.stock.domain.core.entity.RestockRequest;
import com.ccbsa.wms.stock.domain.core.valueobject.RestockRequestId;
import com.ccbsa.wms.stock.domain.core.valueobject.RestockRequestStatus;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * Cached Restock Request Repository Adapter.
 * <p>
 * MANDATORY: Decorates RestockRequestRepositoryAdapter with Redis caching.
 * Implements cache-aside pattern for reads and write-through for writes.
 */
@Repository
@Primary
@Slf4j
public class CachedRestockRequestRepositoryAdapter extends CachedRepositoryDecorator<RestockRequest, RestockRequestId> implements RestockRequestRepository {
    private final RestockRequestRepositoryAdapter baseRepository;

    public CachedRestockRequestRepositoryAdapter(RestockRequestRepositoryAdapter baseRepository, RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        super(baseRepository, redisTemplate, CacheNamespace.RESTOCK_REQUESTS.getValue(), Duration.ofMinutes(30), meterRegistry);
        this.baseRepository = baseRepository;
    }

    @Override
    public void save(RestockRequest restockRequest) {
        // Write-through: Save to database + update cache
        baseRepository.save(restockRequest);

        if (restockRequest.getTenantId() != null && restockRequest.getId() != null) {
            saveWithCache(restockRequest.getTenantId(), restockRequest.getId().getValue(), restockRequest, obj -> restockRequest);
        }
    }

    @Override
    public Optional<RestockRequest> findById(RestockRequestId restockRequestId, TenantId tenantId) {
        return findWithCache(tenantId, restockRequestId.getValue(), entityId -> baseRepository.findById(restockRequestId, tenantId));
    }

    @Override
    public Optional<RestockRequest> findActiveByProductIdAndLocationId(TenantId tenantId, ProductId productId, LocationId locationId) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findActiveByProductIdAndLocationId(tenantId, productId, locationId);
    }

    @Override
    public List<RestockRequest> findByTenantId(TenantId tenantId, RestockRequestStatus status) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByTenantId(tenantId, status);
    }
}
