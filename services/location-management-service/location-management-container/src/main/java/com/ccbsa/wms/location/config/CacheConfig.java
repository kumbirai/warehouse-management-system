package com.ccbsa.wms.location.config;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Cache Configuration: CacheConfig
 * <p>
 * Configures Caffeine cache for location hierarchy queries.
 * <p>
 * Note: This service uses Redis for entity caching (via RedisTemplate in CachedLocationRepositoryAdapter).
 * This Caffeine cache manager is specifically for hierarchy queries to provide fast in-memory caching
 * for frequently accessed hierarchy data.
 * <p>
 * Cache Configuration:
 * - Maximum size: 1000 entries
 * - TTL: 5 minutes
 * - Cache manager bean named "hierarchyCacheManager" for explicit use in @Cacheable
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final int MAX_SIZE = 1000;
    private static final int TTL_MINUTES = 5;

    /**
     * Creates Caffeine cache manager for location hierarchy queries.
     * <p>
     * Named "hierarchyCacheManager" to avoid conflict with Redis cache manager auto-configured by Spring Boot.
     * Use with @Cacheable(cacheManager = "hierarchyCacheManager") to explicitly use this cache.
     *
     * @return CacheManager instance
     */
    @Bean(name = "hierarchyCacheManager")
    public CacheManager hierarchyCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("warehouses", "zones", "aisles", "racks", "bins");
        cacheManager.setCaffeine(Caffeine.newBuilder().maximumSize(MAX_SIZE).expireAfterWrite(TTL_MINUTES, TimeUnit.MINUTES).recordStats() // Enable cache statistics
        );
        return cacheManager;
    }
}
