package com.ccbsa.common.cache.decorator;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import com.ccbsa.common.cache.key.CacheKeyGenerator;
import com.ccbsa.common.domain.AggregateRoot;
import com.ccbsa.common.domain.valueobject.TenantId;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Base class for cached repository decorators.
 * <p>
 * Provides common caching logic for repository adapters. Implements cache-aside pattern with automatic metrics collection.
 * <p>
 * Type Parameters: - T: Domain entity type (e.g., User, Product) - ID: Entity ID type (e.g., UserId, ProductId)
 * <p>
 * Usage:
 * <pre>
 * @Repository
 * @Primary
 * public class CachedUserRepositoryAdapter extends CachedRepositoryDecorator<User, UserId>
 *         implements UserRepository {
 *
 *     public CachedUserRepositoryAdapter(
 *             UserRepositoryAdapter baseRepository,
 *             RedisTemplate<String, Object> redisTemplate,
 *             MeterRegistry meterRegistry
 *     ) {
 *         super(baseRepository, redisTemplate, "users", Duration.ofMinutes(15), meterRegistry);
 *     }
 * }
 * </pre>
 */
public abstract class CachedRepositoryDecorator<T, ID> {

    private static final Logger log = LoggerFactory.getLogger(CachedRepositoryDecorator.class);

    protected final Object baseRepository;
    protected final RedisTemplate<String, Object> redisTemplate;
    protected final String cacheNamespace;
    protected final Duration cacheTtl;

    // Metrics
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Counter cacheWrites;
    private final Counter cacheEvictions;

    protected CachedRepositoryDecorator(Object baseRepository, RedisTemplate<String, Object> redisTemplate, String cacheNamespace, Duration cacheTtl, MeterRegistry meterRegistry) {
        this.baseRepository = baseRepository;
        this.redisTemplate = redisTemplate;
        this.cacheNamespace = cacheNamespace;
        this.cacheTtl = cacheTtl;

        // Initialize metrics
        this.cacheHits = Counter.builder("cache.hits").tag("namespace", cacheNamespace).description("Number of cache hits").register(meterRegistry);

        this.cacheMisses = Counter.builder("cache.misses").tag("namespace", cacheNamespace).description("Number of cache misses").register(meterRegistry);

        this.cacheWrites = Counter.builder("cache.writes").tag("namespace", cacheNamespace).description("Number of cache writes").register(meterRegistry);

        this.cacheEvictions = Counter.builder("cache.evictions").tag("namespace", cacheNamespace).description("Number of cache evictions").register(meterRegistry);
    }

    /**
     * Finds entity in cache, or loads from database if not cached.
     * <p>
     * Cache-aside pattern: 1. Check cache 2. If hit, return cached value 3. If miss, load from database 4. Store in cache with TTL 5. Return value
     */
    @SuppressWarnings("unchecked")
    protected Optional<T> findWithCache(TenantId tenantId, UUID entityId, Function<UUID, Optional<T>> databaseLoader) {
        // 1. Generate cache key
        String cacheKey = CacheKeyGenerator.forEntity(tenantId, cacheNamespace, entityId);

        // 2. Check cache first
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);

            if (cached != null) {
                cacheHits.increment();
                log.trace("Cache HIT for key: {}", cacheKey);
                return Optional.of((T) cached);
            }
        } catch (Exception e) {
            // Log deserialization errors at DEBUG level (expected when old incompatible entries exist)
            // Log other errors at WARN level
            if (e instanceof org.springframework.data.redis.serializer.SerializationException) {
                log.debug("Cache deserialization failed for key: {} (old entry may be incompatible), evicting stale entry and falling back to database", cacheKey);
                // Evict stale cache entry to prevent repeated deserialization failures
                try {
                    Boolean deleted = redisTemplate.delete(cacheKey);
                    if (Boolean.TRUE.equals(deleted)) {
                        cacheEvictions.increment();
                        log.trace("Cache EVICTION (stale entry) for key: {}", cacheKey);
                    }
                } catch (Exception evictionError) {
                    log.warn("Failed to evict stale cache entry for key: {}", cacheKey, evictionError);
                }
            } else {
                log.warn("Cache read failed for key: {}, falling back to database", cacheKey, e);
            }
            // Fall through to database query
        }

        // 3. Cache miss - load from database
        cacheMisses.increment();
        log.trace("Cache MISS for key: {}", cacheKey);

        Optional<T> entity = databaseLoader.apply(entityId);

        // 4. Populate cache if entity exists
        if (entity.isPresent()) {
            try {
                T entityToCache = entity.get();
                // Clear domain events before caching (domain events are transient and should not be cached)
                clearDomainEventsIfAggregateRoot(entityToCache);

                redisTemplate.opsForValue().set(cacheKey, entityToCache, cacheTtl);
                cacheWrites.increment();
                log.trace("Cache WRITE for key: {}", cacheKey);
            } catch (Exception e) {
                log.error("Cache write failed for key: {}", cacheKey, e);
                // Don't throw - cache failure shouldn't break application
            }
        }

        return entity;
    }

    /**
     * Clears domain events from an entity if it is an AggregateRoot.
     * <p>
     * Domain events are transient and should not be cached. This method ensures that
     * entities are cached without their domain events, preventing serialization issues
     * and maintaining correct domain event lifecycle (events should be published and cleared,
     * not persisted in cache).
     *
     * @param entity Entity to clear domain events from (if it's an AggregateRoot)
     */
    private void clearDomainEventsIfAggregateRoot(T entity) {
        if (entity instanceof AggregateRoot) {
            ((AggregateRoot<?>) entity).clearDomainEvents();
        }
    }

    /**
     * Saves entity to database and updates cache (write-through).
     * <p>
     * Write-through pattern: 1. Save to database (source of truth) 2. Update cache immediately 3. Return saved entity
     * <p>
     * Cache writes are non-blocking with timeout protection to prevent hanging operations.
     * Redis command timeout is configured to 2 seconds in CacheConfiguration.
     */
    protected T saveWithCache(TenantId tenantId, UUID entityId, T entity, Function<T, T> databaseSaver) {
        // 1. Save to database first (source of truth)
        T savedEntity = databaseSaver.apply(entity);

        // 2. Update cache (write-through) with timeout protection
        String cacheKey = CacheKeyGenerator.forEntity(tenantId, cacheNamespace, entityId);

        try {
            // Clear domain events before caching (domain events are transient and should not be cached)
            clearDomainEventsIfAggregateRoot(savedEntity);

            // Execute cache write with timeout protection
            // Redis command timeout is configured to 2 seconds in CacheConfiguration
            // This try-catch ensures we don't block if Redis is unavailable or times out
            redisTemplate.opsForValue().set(cacheKey, savedEntity, cacheTtl);
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

        return savedEntity;
    }

    /**
     * Deletes entity from database and evicts from cache.
     * <p>
     * Write-through delete: 1. Delete from database 2. Evict from cache
     */
    protected void deleteWithCache(TenantId tenantId, UUID entityId, Consumer<UUID> databaseDeleter) {
        // 1. Delete from database first
        databaseDeleter.accept(entityId);

        // 2. Evict from cache
        String cacheKey = CacheKeyGenerator.forEntity(tenantId, cacheNamespace, entityId);

        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (Boolean.TRUE.equals(deleted)) {
                cacheEvictions.increment();
                log.trace("Cache EVICTION for key: {}", cacheKey);
            }
        } catch (Exception e) {
            log.error("Cache eviction failed for key: {}", cacheKey, e);
            // Don't throw - cache failure shouldn't break application
        }
    }

    /**
     * Evicts cache key manually (for explicit invalidation).
     */
    protected void evictFromCache(TenantId tenantId, UUID entityId) {
        String cacheKey = CacheKeyGenerator.forEntity(tenantId, cacheNamespace, entityId);

        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (Boolean.TRUE.equals(deleted)) {
                cacheEvictions.increment();
                log.debug("Manual cache eviction for key: {}", cacheKey);
            }
        } catch (Exception e) {
            log.error("Manual cache eviction failed for key: {}", cacheKey, e);
        }
    }
}
