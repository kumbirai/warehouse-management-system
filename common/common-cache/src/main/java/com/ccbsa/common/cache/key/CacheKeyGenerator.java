package com.ccbsa.common.cache.key;

import java.util.UUID;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.security.TenantContext;

/**
 * Utility class for manual cache key generation.
 * <p>
 * Use this when you need to build cache keys manually (e.g., in repository decorators).
 * <p>
 * Example:
 * <pre>
 * String cacheKey = CacheKeyGenerator.forEntity(
 *     tenantId,
 *     "users",
 *     userId.getValue()
 * );
 * // Result: "tenant:acme-corp:users:550e8400-e29b-41d4-a716-446655440000"
 * </pre>
 */
public final class CacheKeyGenerator {

    private static final String KEY_SEPARATOR = ":";
    private static final String TENANT_PREFIX = "tenant";
    private static final String GLOBAL_PREFIX = "global";

    private CacheKeyGenerator() {
        // Utility class - prevent instantiation
    }

    /**
     * Generates cache key for entity lookup.
     * Format: tenant:{tenantId}:{namespace}:{entityId}
     */
    public static String forEntity(TenantId tenantId, String namespace, UUID entityId) {
        return String.join(KEY_SEPARATOR,
                TENANT_PREFIX,
                tenantId.getValue(),
                namespace,
                entityId.toString()
        );
    }

    /**
     * Generates cache key for entity lookup using current tenant context.
     */
    public static String forEntity(String namespace, UUID entityId) {
        String tenantId = TenantContext.getTenantId() != null
                ? TenantContext.getTenantId().getValue()
                : "unknown";
        return String.join(KEY_SEPARATOR,
                TENANT_PREFIX,
                tenantId,
                namespace,
                entityId.toString()
        );
    }

    /**
     * Generates cache key for collection queries.
     * Format: tenant:{tenantId}:{namespace}:{queryParams}
     */
    public static String forCollection(TenantId tenantId, String namespace, String... queryParams) {
        StringBuilder key = new StringBuilder()
                .append(TENANT_PREFIX)
                .append(KEY_SEPARATOR)
                .append(tenantId.getValue())
                .append(KEY_SEPARATOR)
                .append(namespace);

        if (queryParams != null && queryParams.length > 0) {
            key.append(KEY_SEPARATOR)
                    .append(String.join(KEY_SEPARATOR, queryParams));
        }

        return key.toString();
    }

    /**
     * Generates global cache key (not tenant-specific).
     * Use only for cross-tenant admin operations.
     * Format: global:{namespace}:{key}
     */
    public static String forGlobal(String namespace, String key) {
        return String.join(KEY_SEPARATOR,
                GLOBAL_PREFIX,
                namespace,
                key
        );
    }

    /**
     * Generates wildcard pattern for cache invalidation.
     * Format: tenant:{tenantId}:{namespace}:*
     */
    public static String wildcardPattern(TenantId tenantId, String namespace) {
        return String.join(KEY_SEPARATOR,
                TENANT_PREFIX,
                tenantId.getValue(),
                namespace,
                "*"
        );
    }
}
