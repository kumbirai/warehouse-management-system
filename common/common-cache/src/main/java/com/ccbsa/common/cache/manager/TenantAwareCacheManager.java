package com.ccbsa.common.cache.manager;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import com.ccbsa.common.cache.config.CacheProperties;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tenant-Aware Cache Manager.
 * <p>
 * Extends RedisCacheManager to provide:
 * - Automatic tenant prefix injection in cache keys
 * - Per-cache TTL configuration from CacheProperties
 * - Support for cache-specific configurations
 */
public class TenantAwareCacheManager extends RedisCacheManager {

    private final CacheProperties cacheProperties;
    private final Map<String, RedisCacheConfiguration> cacheConfigurations;
    private final Map<String, Cache> dynamicCaches;

    @SuppressFBWarnings(value = {"EI_EXPOSE_REP2", "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR"},
            justification = "CacheProperties is a Spring @ConfigurationProperties bean managed by Spring. It is initialized once and not mutated after construction. The "
                    + "reference is safe to store. getDefaultCacheConfiguration() is called to initialize cache configurations, and this is safe as the method is not overridden "
                    + "in practice.")
    public TenantAwareCacheManager(RedisConnectionFactory connectionFactory,
                                   RedisCacheConfiguration defaultCacheConfiguration,
                                   CacheProperties cacheProperties) {
        super(RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory), defaultCacheConfiguration);
        this.cacheProperties = cacheProperties;
        this.cacheConfigurations = new ConcurrentHashMap<>();
        this.dynamicCaches = new ConcurrentHashMap<>();
        initializeCacheConfigurations();
    }

    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
            justification = "getCacheConfigs() returns Collections.unmodifiableMap() which is never null. The null check is defensive programming for clarity.")
    private void initializeCacheConfigurations() {
        // Build cache configurations from properties
        if (cacheProperties.getCacheConfigs() != null) {
            cacheProperties.getCacheConfigs().forEach((cacheName, config) -> {
                RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(config.getTtlMinutes() != null
                                ? config.getTtlMinutes()
                                : cacheProperties.getDefaultTtlMinutes()))
                        .serializeKeysWith(getDefaultCacheConfiguration().getKeySerializationPair())
                        .serializeValuesWith(getDefaultCacheConfiguration().getValueSerializationPair());

                cacheConfigurations.put(cacheName, cacheConfig);
            });
        }
    }

    @Override
    protected Collection<RedisCache> loadCaches() {
        // Return empty collection - caches are created on-demand
        return Collections.emptyList();
    }

    @Override
    public Cache getCache(String name) {
        // First check dynamic caches
        Cache cache = dynamicCaches.get(name);
        if (cache != null) {
            return cache;
        }

        // Then check parent
        cache = super.getCache(name);
        if (cache != null) {
            return cache;
        }

        // Create cache on-demand if enabled
        if (cacheProperties.getEnabled()) {
            RedisCacheConfiguration cacheConfig = cacheConfigurations.getOrDefault(
                    name,
                    getDefaultCacheConfiguration()
            );
            cache = createRedisCache(name, cacheConfig);
            dynamicCaches.put(name, cache);
        }

        return cache;
    }

    @Override
    protected RedisCache createRedisCache(String name, RedisCacheConfiguration cacheConfig) {
        return super.createRedisCache(name, cacheConfig);
    }
}
