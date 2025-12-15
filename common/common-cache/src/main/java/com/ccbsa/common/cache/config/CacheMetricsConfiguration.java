package com.ccbsa.common.cache.config;

/**
 * Cache Metrics Configuration.
 * <p>
 * Cache metrics are automatically configured by Spring Boot Actuator via
 * {@code org.springframework.boot.actuate.autoconfigure.metrics.cache.CacheMetricsRegistrarConfiguration}.
 * <p>
 * This class exists for documentation purposes only. No bean definitions are provided here
 * to avoid conflicts with Spring Boot Actuator's auto-configuration.
 * <p>
 * Exposed Metrics (via Spring Boot Actuator):
 * - cache.size: Current number of entries in cache
 * - cache.gets: Total cache get operations
 * - cache.puts: Total cache put operations
 * - cache.evictions: Total cache evictions
 * - cache.hits: Cache hit count
 * - cache.misses: Cache miss count
 * - cache.hit.ratio: Cache hit rate (hits / (hits + misses))
 * <p>
 * To enable cache metrics, ensure that:
 * 1. {@code spring-boot-starter-actuator} is on the classpath
 * 2. Management endpoints are enabled in application.yml:
 * <pre>
 *    management:
 *      endpoints:
 *        web:
 *          exposure:
 *            include: metrics,prometheus
 *    </pre>
 */
public class CacheMetricsConfiguration {
    // Cache metrics are automatically provided by Spring Boot Actuator
    // No bean definitions needed to avoid conflicts with auto-configuration
}
