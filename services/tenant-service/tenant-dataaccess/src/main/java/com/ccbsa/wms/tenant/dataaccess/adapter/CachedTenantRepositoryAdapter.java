package com.ccbsa.wms.tenant.dataaccess.adapter;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.cache.key.CacheKeyGenerator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.tenant.application.service.port.repository.TenantRepository;
import com.ccbsa.wms.tenant.domain.core.entity.Tenant;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Cached Tenant Repository Adapter.
 * <p>
 * MANDATORY: Decorates TenantRepositoryAdapter with Redis caching.
 * Implements cache-aside pattern for reads and write-through for writes.
 * <p>
 * Note: Tenants are not tenant-aware (they are the tenant themselves), so we use global cache keys.
 * This adapter does not extend CachedRepositoryDecorator because Tenant uses String IDs (TenantId)
 * instead of UUID, and tenants are not tenant-aware.
 * <p>
 * Cache Configuration:
 * - Namespace: "tenants"
 * - TTL: Configured via application.yml (default: 30 minutes)
 * - Eviction: LRU (configured in Redis)
 * <p>
 * Spring Configuration:
 * - @Primary: Ensures this adapter is injected instead of base adapter
 * - @Repository: Marks as Spring Data repository component
 */
@Repository
@Primary
public class CachedTenantRepositoryAdapter implements TenantRepository {

    private static final Logger log = LoggerFactory.getLogger(CachedTenantRepositoryAdapter.class);

    private final TenantRepositoryAdapter baseRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Duration cacheTtl;
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Counter cacheWrites;
    private final Counter cacheEvictions;

    public CachedTenantRepositoryAdapter(TenantRepositoryAdapter baseRepository, RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        this.baseRepository = baseRepository;
        this.redisTemplate = redisTemplate;
        this.cacheTtl = Duration.ofMinutes(30); // TTL from config

        // Initialize metrics
        this.cacheHits = Counter.builder("cache.hits").tag("namespace", CacheNamespace.TENANTS.getValue()).description("Number of cache hits").register(meterRegistry);
        this.cacheMisses = Counter.builder("cache.misses").tag("namespace", CacheNamespace.TENANTS.getValue()).description("Number of cache misses").register(meterRegistry);
        this.cacheWrites = Counter.builder("cache.writes").tag("namespace", CacheNamespace.TENANTS.getValue()).description("Number of cache writes").register(meterRegistry);
        this.cacheEvictions =
                Counter.builder("cache.evictions").tag("namespace", CacheNamespace.TENANTS.getValue()).description("Number of cache evictions").register(meterRegistry);
    }

    /**
     * Finds tenant by ID with caching using global cache key.
     */
    @Override
    public Optional<Tenant> findById(TenantId tenantId) {
        // Use global cache key since tenants are not tenant-aware
        String cacheKey = CacheKeyGenerator.forGlobal(CacheNamespace.TENANTS.getValue(), tenantId.getValue());

        // Check cache first
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                cacheHits.increment();
                log.trace("Cache HIT for key: {}", cacheKey);
                return Optional.of((Tenant) cached);
            }
        } catch (Exception e) {
            if (e instanceof org.springframework.data.redis.serializer.SerializationException) {
                log.debug("Cache deserialization failed for key: {} (old entry may be incompatible), falling back to database", cacheKey);
            } else {
                log.warn("Cache read failed for key: {}, falling back to database", cacheKey, e);
            }
        }

        // Cache miss - load from database
        cacheMisses.increment();
        log.trace("Cache MISS for key: {}", cacheKey);

        Optional<Tenant> tenant = baseRepository.findById(tenantId);

        // Populate cache if tenant exists
        if (tenant.isPresent()) {
            try {
                Tenant tenantToCache = tenant.get();
                tenantToCache.clearDomainEvents(); // Clear domain events before caching
                redisTemplate.opsForValue().set(cacheKey, tenantToCache, cacheTtl);
                cacheWrites.increment();
                log.trace("Cache WRITE for key: {}", cacheKey);
            } catch (Exception e) {
                log.error("Cache write failed for key: {}", cacheKey, e);
            }
        }

        return tenant;
    }

    @Override
    public void save(Tenant tenant) {
        // Write-through: Save to database + update cache
        baseRepository.save(tenant);

        // Update cache using global key (non-blocking with timeout protection)
        if (tenant.getId() != null) {
            String cacheKey = CacheKeyGenerator.forGlobal(CacheNamespace.TENANTS.getValue(), tenant.getId().getValue());
            try {
                tenant.clearDomainEvents(); // Clear domain events before caching

                // Execute cache write with timeout protection
                // Redis command timeout is configured to 2 seconds in CacheConfiguration
                // This try-catch ensures we don't block if Redis is unavailable or times out
                redisTemplate.opsForValue().set(cacheKey, tenant, cacheTtl);
                cacheWrites.increment();
                log.trace("Cache WRITE (write-through) for key: {}", cacheKey);
            } catch (org.springframework.data.redis.RedisConnectionFailureException e) {
                // Redis connection failure - log but don't throw (graceful degradation)
                log.warn("Cache write-through failed due to Redis connection failure for key: {} - continuing without cache", cacheKey);
            } catch (Exception e) {
                // Any other cache error (including timeouts) - log but don't throw (graceful degradation)
                // Redis timeout is handled by Lettuce client configuration (2 second command timeout)
                log.warn("Cache write-through failed for key: {} - continuing without cache. Error: {}", cacheKey, e.getMessage());
                // Don't throw - cache failure shouldn't break application
            }
        }
    }

    @Override
    public List<Tenant> findAll() {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findAll();
    }

    @Override
    public boolean existsById(TenantId tenantId) {
        // Existence checks are fast, not cached
        return baseRepository.existsById(tenantId);
    }

    @Override
    public void deleteById(TenantId tenantId) {
        // Delete from database
        baseRepository.deleteById(tenantId);

        // Evict from cache
        String cacheKey = CacheKeyGenerator.forGlobal(CacheNamespace.TENANTS.getValue(), tenantId.getValue());
        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (Boolean.TRUE.equals(deleted)) {
                cacheEvictions.increment();
                log.trace("Cache EVICTION for key: {}", cacheKey);
            }
        } catch (Exception e) {
            log.error("Cache eviction failed for key: {}", cacheKey, e);
        }
    }
}

