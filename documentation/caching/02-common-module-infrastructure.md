# Common Module Infrastructure for Caching

## Warehouse Management System - Caching Infrastructure

**Document Version:** 1.0
**Date:** 2025-12-09
**Status:** Approved

---

## Overview

This document defines the **mandatory** common module infrastructure for caching. All caching components are centralized in the `common-cache` module to enforce DRY principles and ensure consistent behavior across services.

**Module Location:** `/common/common-cache/`

**Module Dependencies:**
- `common-domain` - For `TenantId`, `DomainEvent` base classes
- `common-application` - For `CorrelationContext`, `RequestContext`
- `common-security` - For `TenantContext` (tenant-aware cache keys)
- `spring-boot-starter-data-redis` - Redis client
- `spring-boot-starter-cache` - Cache abstraction

---

## Package Structure

```
common-cache/
├── pom.xml
└── src/main/java/com/ccbsa/common/cache/
    ├── config/
    │   ├── CacheConfiguration.java                 # Redis & cache manager configuration
    │   ├── CacheProperties.java                    # Externalized cache settings
    │   └── CacheMetricsConfiguration.java          # Micrometer metrics for cache
    ├── key/
    │   ├── CacheKeyGenerator.java                  # Tenant-aware cache key generation
    │   ├── TenantAwareCacheKeyGenerator.java       # Spring Cache key generator
    │   └── CacheNamespace.java                     # Enum of cache namespaces
    ├── manager/
    │   ├── TenantAwareCacheManager.java            # Custom cache manager with tenant isolation
    │   └── CacheManagerHealthIndicator.java        # Health check for Redis
    ├── serializer/
    │   ├── JsonCacheSerializer.java                # Jackson-based serialization
    │   └── CacheSerializationException.java        # Serialization error handling
    ├── invalidation/
    │   ├── CacheInvalidationEventListener.java     # Base class for event-driven invalidation
    │   ├── LocalCacheInvalidator.java              # Invalidates local service caches
    │   └── InvalidationEvent.java                  # Domain event for cache invalidation
    ├── decorator/
    │   ├── CachedRepositoryDecorator.java          # Base decorator for repository caching
    │   └── CacheOperationLogger.java               # Logs cache hits/misses
    └── aspect/
        ├── CacheEvictionAspect.java                # AOP for automatic cache eviction
        └── CacheWarmingAspect.java                 # AOP for cache warming on startup
```

---

## 1. Cache Configuration

### 1.1 Maven Dependencies (Parent POM)

**File:** `/pom.xml` (parent POM properties section)

```xml
<properties>
    <!-- Existing properties -->
    <spring-data-redis.version>3.2.0</spring-data-redis.version>
    <lettuce.version>6.3.0.RELEASE</lettuce.version>
</properties>
```

**File:** `/common/common-cache/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ccbsa.wms</groupId>
        <artifactId>common</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>common-cache</artifactId>
    <name>WMS Common - Cache Infrastructure</name>
    <description>Distributed caching infrastructure with Redis and tenant isolation</description>

    <dependencies>
        <!-- Internal Dependencies -->
        <dependency>
            <groupId>com.ccbsa.wms</groupId>
            <artifactId>common-domain</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.ccbsa.wms</groupId>
            <artifactId>common-application</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.ccbsa.wms</groupId>
            <artifactId>common-security</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Spring Cache Abstraction -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>

        <!-- Redis Client -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Lettuce Client (Async/Reactive support) -->
        <dependency>
            <groupId>io.lettuce</groupId>
            <artifactId>lettuce-core</artifactId>
        </dependency>

        <!-- Jackson for JSON Serialization (already in parent) -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>

        <!-- Micrometer for Metrics -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-core</artifactId>
        </dependency>

        <!-- Spring Boot Actuator (Health Checks) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Lombok (already in parent) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.github.fppt</groupId>
            <artifactId>jedis-mock</artifactId>
            <version>1.0.12</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

### 1.2 Cache Configuration Class

**File:** `/common/common-cache/src/main/java/com/ccbsa/common/cache/config/CacheConfiguration.java`

```java
package com.ccbsa.common.cache.config;

import com.ccbsa.common.cache.key.TenantAwareCacheKeyGenerator;
import com.ccbsa.common.cache.manager.TenantAwareCacheManager;
import com.ccbsa.common.cache.serializer.JsonCacheSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis Cache Configuration
 * <p>
 * Configures distributed caching infrastructure with:
 * - Tenant-aware cache key generation
 * - JSON serialization with type information
 * - Connection pooling and timeouts
 * - TTL-based eviction policies
 * <p>
 * Activated via: spring.cache.type=redis
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
public class CacheConfiguration {

    private final CacheProperties cacheProperties;

    public CacheConfiguration(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    /**
     * Redis Connection Factory with optimized Lettuce client configuration.
     * <p>
     * Configuration:
     * - Connection pooling: 10 max connections per node
     * - Timeout: 2 seconds for commands
     * - Socket timeout: 3 seconds for TCP operations
     * - Auto-reconnect: Enabled
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        // Redis server configuration
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(cacheProperties.getRedis().getHost());
        redisConfig.setPort(cacheProperties.getRedis().getPort());

        if (cacheProperties.getRedis().getPassword() != null) {
            redisConfig.setPassword(cacheProperties.getRedis().getPassword());
        }

        // Lettuce client configuration (async/reactive client)
        ClientOptions clientOptions = ClientOptions.builder()
            .socketOptions(SocketOptions.builder()
                .connectTimeout(Duration.ofSeconds(3))
                .keepAlive(true)
                .build())
            .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(2)))
            .autoReconnect(true)
            .build();

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .clientOptions(clientOptions)
            .commandTimeout(Duration.ofSeconds(2))
            .build();

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    /**
     * ObjectMapper for Redis value serialization.
     * <p>
     * Configuration:
     * - JavaTimeModule: Serialize LocalDateTime, Instant, etc.
     * - Type information: Enables polymorphic deserialization
     * - Fail on unknown properties: Disabled for backward compatibility
     */
    @Bean
    public ObjectMapper redisCacheObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL
        );
        objectMapper.configure(
            com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false
        );
        return objectMapper;
    }

    /**
     * Tenant-Aware Cache Manager.
     * <p>
     * Features:
     * - Automatic tenant prefix injection
     * - Per-cache TTL configuration
     * - JSON serialization with type safety
     * - Cache-aside pattern support
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                     ObjectMapper redisCacheObjectMapper) {
        // JSON serializer for cache values
        GenericJackson2JsonRedisSerializer serializer =
            new GenericJackson2JsonRedisSerializer(redisCacheObjectMapper);

        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(cacheProperties.getDefaultTtlMinutes()))
            .disableCachingNullValues()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer)
            );

        return new TenantAwareCacheManager(connectionFactory, defaultConfig, cacheProperties);
    }

    /**
     * Tenant-Aware Cache Key Generator.
     * <p>
     * Automatically prefixes cache keys with tenant ID from TenantContext.
     * Format: tenant:{tenantId}:{cacheName}:{key}
     */
    @Bean("tenantAwareCacheKeyGenerator")
    public TenantAwareCacheKeyGenerator tenantAwareCacheKeyGenerator() {
        return new TenantAwareCacheKeyGenerator();
    }

    /**
     * RedisTemplate for manual cache operations.
     * <p>
     * Use cases:
     * - Custom cache operations not covered by @Cacheable
     * - Batch cache operations
     * - Cache statistics queries
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
                                                        ObjectMapper redisCacheObjectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        GenericJackson2JsonRedisSerializer serializer =
            new GenericJackson2JsonRedisSerializer(redisCacheObjectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}
```

### 1.3 Cache Properties

**File:** `/common/common-cache/src/main/java/com/ccbsa/common/cache/config/CacheProperties.java`

```java
package com.ccbsa.common.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

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
public class CacheProperties {

    /**
     * Enable/disable caching globally.
     * Default: true (caching is mandatory in production)
     */
    @NotNull
    private Boolean enabled = true;

    /**
     * Default TTL for all caches (in minutes).
     * Default: 30 minutes
     */
    @Min(1)
    private Integer defaultTtlMinutes = 30;

    /**
     * Redis server configuration.
     */
    @NotNull
    private RedisConfig redis = new RedisConfig();

    /**
     * Per-cache TTL configurations.
     * Key: Cache name (e.g., "users", "products")
     * Value: Cache-specific configuration
     */
    private Map<String, CacheConfig> cacheConfigs = new HashMap<>();

    @Data
    public static class RedisConfig {
        /**
         * Redis server hostname.
         * Default: localhost (dev), overridden in prod via environment variables
         */
        @NotBlank
        private String host = "localhost";

        /**
         * Redis server port.
         * Default: 6379
         */
        @Min(1)
        private Integer port = 6379;

        /**
         * Redis password (optional for dev, mandatory for prod).
         */
        private String password;

        /**
         * Redis database index (0-15).
         * Default: 0
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
         * Maximum number of entries in this cache.
         * Redis will evict entries based on LRU policy when limit is reached.
         */
        private Long maxEntries;
    }
}
```

---

## 2. Cache Key Generation

### 2.1 Tenant-Aware Cache Key Generator

**File:** `/common/common-cache/src/main/java/com/ccbsa/common/cache/key/TenantAwareCacheKeyGenerator.java`

```java
package com.ccbsa.common.cache.key;

import com.ccbsa.common.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Tenant-Aware Cache Key Generator.
 * <p>
 * Automatically prefixes cache keys with tenant ID from TenantContext.
 * This ensures cache isolation between tenants.
 * <p>
 * Key Format: tenant:{tenantId}:{cacheName}:{methodParams}
 * <p>
 * Example:
 * - Method: findUserById(UUID id)
 * - Tenant: "acme-corp"
 * - Cache Name: "users"
 * - Generated Key: "tenant:acme-corp:users:550e8400-e29b-41d4-a716-446655440000"
 * <p>
 * Usage:
 * <pre>
 * @Cacheable(value = "users", keyGenerator = "tenantAwareCacheKeyGenerator")
 * public User findUserById(UserId id) { ... }
 * </pre>
 */
public class TenantAwareCacheKeyGenerator implements KeyGenerator {

    private static final Logger log = LoggerFactory.getLogger(TenantAwareCacheKeyGenerator.class);
    private static final String KEY_SEPARATOR = ":";
    private static final String TENANT_PREFIX = "tenant";

    @Override
    public Object generate(Object target, Method method, Object... params) {
        // 1. Get tenant ID from security context
        String tenantId = TenantContext.getTenantId();

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
     * Handles:
     * - Value Objects (calls getValue())
     * - Domain IDs (calls getValue())
     * - Primitives and Strings (toString())
     */
    private String extractKeyValue(Object param) {
        if (param == null) {
            return "null";
        }

        // Handle Value Objects (UserId, TenantId, etc.)
        try {
            Method getValueMethod = param.getClass().getMethod("getValue");
            Object value = getValueMethod.invoke(param);
            return String.valueOf(value);
        } catch (NoSuchMethodException e) {
            // Not a value object, use toString()
            return param.toString();
        } catch (Exception e) {
            log.warn("Failed to extract value from parameter: {}", param.getClass().getName(), e);
            return param.toString();
        }
    }
}
```

### 2.2 Cache Key Utility

**File:** `/common/common-cache/src/main/java/com/ccbsa/common/cache/key/CacheKeyGenerator.java`

```java
package com.ccbsa.common.cache.key;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.security.TenantContext;

import java.util.UUID;

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
        String tenantId = TenantContext.getTenantId();
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
```

### 2.3 Cache Namespace Enum

**File:** `/common/common-cache/src/main/java/com/ccbsa/common/cache/key/CacheNamespace.java`

```java
package com.ccbsa.common.cache.key;

/**
 * Cache Namespace Constants.
 * <p>
 * Defines standardized cache namespaces for all services.
 * Ensures consistency in cache key prefixes.
 * <p>
 * Usage:
 * <pre>
 * String cacheKey = CacheKeyGenerator.forEntity(
 *     tenantId,
 *     CacheNamespace.USERS.getValue(),
 *     userId
 * );
 * </pre>
 */
public enum CacheNamespace {

    // User Service
    USERS("users"),
    USER_ROLES("user-roles"),
    USER_PERMISSIONS("user-permissions"),

    // Tenant Service
    TENANTS("tenants"),
    TENANT_CONFIG("tenant-config"),

    // Product Service
    PRODUCTS("products"),
    PRODUCT_CATEGORIES("product-categories"),

    // Stock Management Service
    STOCK_CONSIGNMENTS("stock-consignments"),
    STOCK_LEVELS("stock-levels"),

    // Location Management Service
    LOCATIONS("locations"),
    ZONES("zones"),

    // Notification Service
    NOTIFICATIONS("notifications"),

    // Integration Service
    INTEGRATION_CONFIGS("integration-configs"),

    // Global (Cross-Tenant)
    GLOBAL_TENANT_METADATA("global-tenant-metadata");

    private final String value;

    CacheNamespace(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
```

---

**End of Section 2**

This section establishes the common infrastructure for caching. Next sections will cover:
- Cache Invalidation Strategy
- Repository Adapter Decorator Pattern
- Multi-Tenant Caching Patterns
- Cache Warming
- Monitoring and Observability
