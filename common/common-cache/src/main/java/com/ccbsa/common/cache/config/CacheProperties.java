package com.ccbsa.common.cache.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Externalized Cache Configuration Properties.
 * <p>
 * Binds to application.yml properties under 'wms.cache'.
 * <p>
 * Example configuration:
 * <pre>
 * wms:
 *   cache:
 *     enabled: true
 *     default-ttl-minutes: 30
 *     redis:
 *       host: localhost
 *       port: 6379
 *     cache-configs:
 *       users:
 *         ttl-minutes: 15
 *       products:
 *         ttl-minutes: 60
 * </pre>
 */
@Data
@Validated
@ConfigurationProperties(prefix = "wms.cache")
@SuppressFBWarnings(value = {"RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "EI_EXPOSE_REP2"},
        justification = "Lombok @Data generates equals/hashCode with null checks. setRedis() is required by Spring @ConfigurationProperties. RedisConfig is managed by Spring and"
                + " not mutated after initialization.")
public class CacheProperties {

    /**
     * Enable/disable caching globally. Default: true (caching is mandatory in production)
     */
    @NotNull
    private Boolean enabled = true;

    /**
     * Default TTL for all caches (in minutes). Default: 30 minutes
     */
    @Min(1)
    private Integer defaultTtlMinutes = 30;

    /**
     * Redis server configuration.
     */
    @NotNull
    private RedisConfig redis = new RedisConfig();

    /**
     * Per-cache TTL configurations. Key: Cache name (e.g., "users", "products") Value: Cache-specific configuration
     */
    private Map<String, CacheConfig> cacheConfigs = new HashMap<>();

    /**
     * Returns an unmodifiable view of cache configurations. Prevents external modification of internal state.
     *
     * @return Unmodifiable map of cache configurations
     */
    public Map<String, CacheConfig> getCacheConfigs() {
        return Collections.unmodifiableMap(cacheConfigs);
    }

    /**
     * Sets cache configurations with defensive copy. Prevents external modification of internal state.
     *
     * @param cacheConfigs Map of cache configurations
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring @ConfigurationProperties requires setters. The map is copied defensively to prevent external " + "modification.")
    public void setCacheConfigs(Map<String, CacheConfig> cacheConfigs) {
        if (cacheConfigs == null) {
            this.cacheConfigs = new HashMap<>();
        } else {
            this.cacheConfigs = new HashMap<>(cacheConfigs);
        }
    }

    /**
     * Returns Redis configuration. Note: RedisConfig is a nested configuration class managed by Spring. It is initialized once and not mutated after construction.
     *
     * @return Redis configuration
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "RedisConfig is a Spring @ConfigurationProperties nested class managed by Spring. It is initialized once and not"
                    + " mutated after construction. Returning the reference is safe.")
    public RedisConfig getRedis() {
        return redis;
    }

    /**
     * Sets Redis configuration. Note: This setter is required by Spring @ConfigurationProperties. RedisConfig is managed by Spring and not mutated after initialization.
     *
     * @param redis Redis configuration
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring @ConfigurationProperties requires this setter. RedisConfig is managed by Spring and not mutated after " + "initialization.")
    public void setRedis(RedisConfig redis) {
        this.redis = redis;
    }

    @Data
    public static class RedisConfig {
        /**
         * Redis server hostname. Default: localhost (dev), overridden in prod via environment variables
         */
        @NotBlank
        private String host = "localhost";

        /**
         * Redis server port. Default: 6379
         */
        @Min(1)
        private Integer port = 6379;

        /**
         * Redis password (optional for dev, mandatory for prod).
         */
        private String password;

        /**
         * Redis database index (0-15). Default: 0
         */
        @Min(0)
        private Integer database = 0;
    }

    @Data
    public static class CacheConfig {
        /**
         * TTL for this specific cache (in minutes).
         */
        @Min(1)
        private Integer ttlMinutes;

        /**
         * Maximum number of entries in this cache. Redis will evict entries based on LRU policy when limit is reached.
         */
        private Long maxEntries;
    }
}
