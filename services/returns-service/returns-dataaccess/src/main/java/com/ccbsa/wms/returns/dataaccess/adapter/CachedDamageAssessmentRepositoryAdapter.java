package com.ccbsa.wms.returns.dataaccess.adapter;

import java.time.Duration;
import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.cache.decorator.CachedRepositoryDecorator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.returns.application.service.port.repository.DamageAssessmentRepository;
import com.ccbsa.wms.returns.domain.core.entity.DamageAssessment;
import com.ccbsa.wms.returns.domain.core.valueobject.DamageAssessmentId;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * Cached Damage Assessment Repository Adapter.
 * <p>
 * Decorates DamageAssessmentRepositoryAdapter with Redis caching. Implements cache-aside pattern for reads and write-through for writes.
 * <p>
 * Cache Configuration:
 * - Namespace: "damage-assessments"
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
public class CachedDamageAssessmentRepositoryAdapter extends CachedRepositoryDecorator<DamageAssessment, DamageAssessmentId> implements DamageAssessmentRepository {
    private final DamageAssessmentRepositoryAdapter baseRepository;

    public CachedDamageAssessmentRepositoryAdapter(DamageAssessmentRepositoryAdapter baseRepository, RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        super(baseRepository, redisTemplate, CacheNamespace.DAMAGE_ASSESSMENTS.getValue(), Duration.ofMinutes(15), meterRegistry);
        this.baseRepository = baseRepository;
    }

    @Override
    public DamageAssessment save(DamageAssessment damageAssessment) {
        // Write-through: Save to database + update cache
        DamageAssessment saved = baseRepository.save(damageAssessment);

        if (damageAssessment.getTenantId() != null && damageAssessment.getId() != null) {
            saveWithCache(damageAssessment.getTenantId(), damageAssessment.getId().getValue(), saved, obj -> saved);
        }

        return saved;
    }

    @Override
    public Optional<DamageAssessment> findByIdAndTenantId(DamageAssessmentId id, TenantId tenantId) {
        return findWithCache(tenantId, id.getValue(), entityId -> baseRepository.findByIdAndTenantId(id, tenantId));
    }
}
