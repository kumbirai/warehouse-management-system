package com.ccbsa.wms.tenant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.ccbsa.common.cache.config.CacheConfiguration;

/**
 * Tenant Service Cache Configuration.
 * <p>
 * Imports common cache configuration and enables caching for tenant-service.
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
 *       tenants:
 *         ttl-minutes: 30
 * </pre>
 */
@Configuration
@Import(CacheConfiguration.class)
public class TenantCacheConfiguration {
    // Common cache configuration imported from common-cache module
    // No additional configuration needed unless service-specific customization required
}

