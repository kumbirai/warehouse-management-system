package com.ccbsa.common.cache.invalidation;

import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.ccbsa.common.cache.key.CacheKeyGenerator;
import com.ccbsa.common.domain.valueobject.TenantId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Local Cache Invalidator.
 * <p>
 * Provides methods for invalidating caches in the current service.
 * Supports:
 * - Single entity invalidation
 * - Collection invalidation (all queries for a namespace)
 * - Pattern-based invalidation (wildcard matching)
 * - Tenant-wide invalidation (all caches for a tenant)
 * <p>
 * Thread-safe and idempotent.
 */
@Component
public class LocalCacheInvalidator {

    private static final Logger log = LoggerFactory.getLogger(LocalCacheInvalidator.class);

    private final RedisTemplate<String, Object> redisTemplate;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "RedisTemplate is a Spring-managed bean that is thread-safe and immutable after initialization. It is safe to "
            + "store the reference.")
    public LocalCacheInvalidator(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Invalidates cache for a single entity.
     * <p>
     * Example: Invalidate user cache
     * - Tenant: acme-corp
     * - Namespace: users
     * - Entity ID: 550e8400-e29b-41d4-a716-446655440000
     * - Invalidated Key: tenant:acme-corp:users:550e8400-e29b-41d4-a716-446655440000
     */
    public void invalidateEntity(TenantId tenantId, String namespace, UUID entityId) {
        String cacheKey = CacheKeyGenerator.forEntity(tenantId, namespace, entityId);

        Boolean deleted = redisTemplate.delete(cacheKey);

        if (Boolean.TRUE.equals(deleted)) {
            log.debug("Invalidated entity cache: {}", cacheKey);
        } else {
            log.trace("No cache entry found for: {}", cacheKey);
        }
    }

    /**
     * Invalidates all collection caches for a namespace.
     * <p>
     * Example: Invalidate all user query caches
     * - Tenant: acme-corp
     * - Namespace: users
     * - Invalidated Keys: tenant:acme-corp:users:* (all user queries)
     */
    public void invalidateCollection(TenantId tenantId, String namespace) {
        String pattern = CacheKeyGenerator.wildcardPattern(tenantId, namespace);
        invalidateByPattern(pattern);
    }

    /**
     * Invalidates all caches matching a pattern.
     * <p>
     * Example: Invalidate all stock consignments for a product
     * - Pattern: tenant:acme-corp:stock-consignments:product:12345:*
     * <p>
     * Warning: Pattern matching scans all Redis keys, use sparingly.
     */
    public void invalidateByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null && !keys.isEmpty()) {
            Long deletedCount = redisTemplate.delete(keys);
            log.info("Invalidated {} cache entries matching pattern: {}",
                    deletedCount, pattern);
        } else {
            log.trace("No cache entries found matching pattern: {}", pattern);
        }
    }

    /**
     * Invalidates ALL caches for a tenant (use with extreme caution).
     * <p>
     * Example: Tenant deactivation - invalidate all tenant data
     * - Tenant: acme-corp
     * - Invalidated Keys: tenant:acme-corp:* (ALL caches for this tenant)
     * <p>
     * Warning: This is a heavy operation, only use for tenant lifecycle events.
     */
    public void invalidateTenant(TenantId tenantId) {
        String pattern = String.format("tenant:%s:*", tenantId.getValue());

        log.warn("Invalidating ALL caches for tenant: {}", tenantId.getValue());

        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null && !keys.isEmpty()) {
            Long deletedCount = redisTemplate.delete(keys);
            log.warn("Invalidated {} cache entries for tenant: {}",
                    deletedCount, tenantId.getValue());
        }
    }

    /**
     * Invalidates a specific cache by exact key.
     * <p>
     * Use when you have the exact cache key (e.g., from a custom cache key generator).
     */
    public void invalidateByKey(String cacheKey) {
        Boolean deleted = redisTemplate.delete(cacheKey);

        if (Boolean.TRUE.equals(deleted)) {
            log.debug("Invalidated cache: {}", cacheKey);
        } else {
            log.trace("No cache entry found for: {}", cacheKey);
        }
    }
}
