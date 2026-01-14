package com.ccbsa.wms.returns.dataaccess.adapter;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.cache.decorator.CachedRepositoryDecorator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.ReturnStatus;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.returns.application.service.port.repository.ReturnRepository;
import com.ccbsa.wms.returns.domain.core.entity.Return;
import com.ccbsa.common.domain.valueobject.ReturnId;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * Cached Return Repository Adapter.
 * <p>
 * Decorates ReturnRepositoryAdapter with Redis caching. Implements cache-aside pattern for reads and write-through for writes.
 * <p>
 * Cache Configuration:
 * - Namespace: "returns"
 * - TTL: 15 minutes (configurable via application.yml)
 * - Eviction: LRU (configured in Redis)
 * <p>
 * Spring Configuration:
 * - @Primary: Ensures this adapter is injected instead of base adapter
 * - @Repository: Marks as Spring Data repository component
 */
@Repository
@Primary
@Slf4j
public class CachedReturnRepositoryAdapter extends CachedRepositoryDecorator<Return, ReturnId> implements ReturnRepository {
    private final ReturnRepositoryAdapter baseRepository;

    public CachedReturnRepositoryAdapter(ReturnRepositoryAdapter baseRepository, RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        super(baseRepository, redisTemplate, CacheNamespace.RETURNS.getValue(), Duration.ofMinutes(15), meterRegistry);
        this.baseRepository = baseRepository;
    }

    @Override
    public Return save(Return returnAggregate) {
        // Write-through: Save to database + update cache
        Return saved = baseRepository.save(returnAggregate);

        if (returnAggregate.getTenantId() != null && returnAggregate.getId() != null) {
            saveWithCache(returnAggregate.getTenantId(), returnAggregate.getId().getValue(), saved, obj -> saved);
        }

        return saved;
    }

    @Override
    public Optional<Return> findByIdAndTenantId(ReturnId id, TenantId tenantId) {
        return findWithCache(tenantId, id.getValue(), entityId -> baseRepository.findByIdAndTenantId(id, tenantId));
    }

    @Override
    public List<Return> findByStatusAndTenantId(ReturnStatus status, TenantId tenantId) {
        // List queries are not cached (too many combinations)
        return baseRepository.findByStatusAndTenantId(status, tenantId);
    }

    @Override
    public List<Return> findByOrderNumberAndTenantId(OrderNumber orderNumber, TenantId tenantId) {
        // List queries are not cached (too many combinations)
        return baseRepository.findByOrderNumberAndTenantId(orderNumber, tenantId);
    }
}
