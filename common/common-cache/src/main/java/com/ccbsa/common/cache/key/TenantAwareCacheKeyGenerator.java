package com.ccbsa.common.cache.key;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.interceptor.KeyGenerator;

import com.ccbsa.wms.common.security.TenantContext;

/**
 * Tenant-Aware Cache Key Generator.
 * <p>
 * Automatically prefixes cache keys with tenant ID from TenantContext. This ensures cache isolation between tenants.
 * <p>
 * Key Format: tenant:{tenantId}:{cacheName}:{methodParams}
 * <p>
 * Example: - Method: findUserById(UUID id) - Tenant: "acme-corp" - Cache Name: "users" - Generated Key: "tenant:acme-corp:users:550e8400-e29b-41d4-a716-446655440000"
 * <p>
 * Usage:
 * <pre>
 * @Cacheable(value = "users", keyGenerator = "tenantAwareCacheKeyGenerator")
 * public User findUserById(UserId id) { ... }
 * </pre>
 */
public class TenantAwareCacheKeyGenerator
        implements KeyGenerator {

    private static final Logger log = LoggerFactory.getLogger(TenantAwareCacheKeyGenerator.class);
    private static final String KEY_SEPARATOR = ":";
    private static final String TENANT_PREFIX = "tenant";

    @Override
    public Object generate(Object target, Method method, Object... params) {
        // 1. Get tenant ID from security context
        String tenantId = TenantContext.getTenantId() != null ? TenantContext.getTenantId()
                .getValue() : "unknown";

        // 2. Build cache key with tenant prefix
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(TENANT_PREFIX)
                .append(KEY_SEPARATOR)
                .append(tenantId)
                .append(KEY_SEPARATOR);

        // 3. Append method parameters
        if (params != null && params.length > 0) {
            String paramsKey = Arrays.stream(params)
                    .map(this::extractKeyValue)
                    .collect(Collectors.joining(KEY_SEPARATOR));
            keyBuilder.append(paramsKey);
        } else {
            keyBuilder.append("no-params");
        }

        String cacheKey = keyBuilder.toString();
        log.trace("Generated cache key: {} for method: {}", cacheKey, method.getName());

        return cacheKey;
    }

    /**
     * Extracts cache key value from method parameter.
     * <p>
     * Handles: - Value Objects (calls getValue()) - Domain IDs (calls getValue()) - Primitives and Strings (toString())
     */
    private String extractKeyValue(Object param) {
        if (param == null) {
            return "null";
        }

        // Handle Value Objects (UserId, TenantId, etc.)
        try {
            Method getValueMethod = param.getClass()
                    .getMethod("getValue");
            Object value = getValueMethod.invoke(param);
            return String.valueOf(value);
        } catch (NoSuchMethodException e) {
            // Not a value object, use toString()
            return param.toString();
        } catch (Exception e) {
            log.warn("Failed to extract value from parameter: {}", param.getClass()
                    .getName(), e);
            return param.toString();
        }
    }
}
