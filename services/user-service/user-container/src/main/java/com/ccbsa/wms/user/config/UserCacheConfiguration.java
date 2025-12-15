package com.ccbsa.wms.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.ccbsa.common.cache.config.CacheConfiguration;

/**
 * User Service Cache Configuration.
 * <p>
 * Imports common cache configuration and enables caching for user-service.
 * <p>
 * Configuration Properties (application.yml):
 * <pre>
 * wms:
 *   cache:
 *     enabled: true
 *     default-ttl-minutes: 30
 *     redis:
 *       host: ${REDIS_HOST:localhost}
 *       port: ${REDIS_PORT:6379}
 *       password: ${REDIS_PASSWORD:}
 *     cache-configs:
 *       users:
 *         ttl-minutes: 15
 *       user-roles:
 *         ttl-minutes: 30
 *       user-permissions:
 *         ttl-minutes: 60
 * </pre>
 */
@Configuration
@Import(CacheConfiguration.class)
public class UserCacheConfiguration {
    // Common cache configuration imported from common-cache module
    // No additional configuration needed unless service-specific customization required
}
