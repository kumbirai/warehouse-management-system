# Repository Adapter Decorator Pattern

## Warehouse Management System - Cached Repository Implementation

**Document Version:** 1.0
**Date:** 2025-12-09
**Status:** Approved

---

## Overview

This document defines the **mandatory** decorator pattern for implementing cached repository adapters. The decorator pattern ensures caching is transparent to the application layer while maintaining clean hexagonal architecture principles.

**Key Principle:** Caching is an **infrastructure concern** and must be implemented using the decorator pattern to avoid polluting domain and application layers.

---

## 1. Architecture Pattern

### 1.1 Decorator Pattern Structure

```
┌─────────────────────────────────────────────────────────────┐
│ Application Service Layer                                   │
│                                                              │
│ @Service                                                     │
│ public class UserQueryHandler {                             │
│     private final UserRepository repository; // Interface   │
│                                                              │
│     public User findUser(UserId id) {                       │
│         return repository.findById(id); // No cache logic   │
│     }                                                        │
│ }                                                            │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ Dependency Injection
                         │ (Spring injects CachedUserRepositoryAdapter)
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ Infrastructure Layer - Cached Adapter (Decorator)           │
│                                                              │
│ @Repository                                                  │
│ @Primary // Spring injects this instead of base adapter     │
│ public class CachedUserRepositoryAdapter                    │
│         implements UserRepository {                         │
│                                                              │
│     private final UserRepositoryAdapter baseRepository;     │
│     private final RedisTemplate<String, Object> redis;      │
│     private final CacheKeyGenerator keyGenerator;           │
│                                                              │
│     @Override                                                │
│     public Optional<User> findById(UserId id) {             │
│         // 1. Check cache first                             │
│         String key = keyGenerator.forEntity(...);           │
│         User cached = redis.opsForValue().get(key);         │
│         if (cached != null) return Optional.of(cached);     │
│                                                              │
│         // 2. Cache miss - delegate to base repository      │
│         Optional<User> user = baseRepository.findById(id);  │
│                                                              │
│         // 3. Populate cache                                │
│         user.ifPresent(u -> redis.opsForValue().set(key, u));│
│                                                              │
│         return user;                                         │
│     }                                                        │
│ }                                                            │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ Delegates to
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ Infrastructure Layer - Base Adapter (JPA Implementation)    │
│                                                              │
│ @Repository                                                  │
│ public class UserRepositoryAdapter implements UserRepository│
│                                                              │
│     private final UserJpaRepository jpaRepository;          │
│     private final UserEntityMapper mapper;                  │
│                                                              │
│     @Override                                                │
│     public Optional<User> findById(UserId id) {             │
│         return jpaRepository.findById(id.getValue())        │
│             .map(mapper::toDomain);                         │
│     }                                                        │
│ }                                                            │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ JPA Query
                         ▼
                    PostgreSQL Database
```

**Benefits:**

- ✅ **Separation of Concerns** - Caching logic separate from persistence logic
- ✅ **Testability** - Test base adapter without cache, test cache decorator independently
- ✅ **Composability** - Chain decorators (cache → metrics → circuit breaker)
- ✅ **Replaceability** - Swap cache implementation without touching base adapter

---

## 2. Base Decorator Template

### 2.1 Generic Cached Repository Decorator

**File:** `/common/common-cache/src/main/java/com/ccbsa/common/cache/decorator/CachedRepositoryDecorator.java`

```java
package com.ccbsa.common.cache.decorator;

import com.ccbsa.common.cache.key.CacheKeyGenerator;
import com.ccbsa.common.domain.valueobject.TenantId;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Base class for cached repository decorators.
 * <p>
 * Provides common caching logic for repository adapters.
 * Implements cache-aside pattern with automatic metrics collection.
 * <p>
 * Type Parameters:
 * - T: Domain entity type (e.g., User, Product)
 * - ID: Entity ID type (e.g., UserId, ProductId)
 * <p>
 * Usage:
 * <pre>
 * @Repository
 * @Primary
 * public class CachedUserRepositoryAdapter extends CachedRepositoryDecorator<User, UserId>
 *         implements UserRepository {
 *
 *     public CachedUserRepositoryAdapter(
 *             UserRepositoryAdapter baseRepository,
 *             RedisTemplate<String, Object> redisTemplate,
 *             MeterRegistry meterRegistry
 *     ) {
 *         super(baseRepository, redisTemplate, "users", Duration.ofMinutes(15), meterRegistry);
 *     }
 * }
 * </pre>
 */
public abstract class CachedRepositoryDecorator<T, ID> {

    private static final Logger log = LoggerFactory.getLogger(CachedRepositoryDecorator.class);

    protected final Object baseRepository;
    protected final RedisTemplate<String, Object> redisTemplate;
    protected final String cacheNamespace;
    protected final Duration cacheTtl;

    // Metrics
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Counter cacheWrites;
    private final Counter cacheEvictions;

    protected CachedRepositoryDecorator(
            Object baseRepository,
            RedisTemplate<String, Object> redisTemplate,
            String cacheNamespace,
            Duration cacheTtl,
            MeterRegistry meterRegistry
    ) {
        this.baseRepository = baseRepository;
        this.redisTemplate = redisTemplate;
        this.cacheNamespace = cacheNamespace;
        this.cacheTtl = cacheTtl;

        // Initialize metrics
        this.cacheHits = Counter.builder("cache.hits")
            .tag("namespace", cacheNamespace)
            .description("Number of cache hits")
            .register(meterRegistry);

        this.cacheMisses = Counter.builder("cache.misses")
            .tag("namespace", cacheNamespace)
            .description("Number of cache misses")
            .register(meterRegistry);

        this.cacheWrites = Counter.builder("cache.writes")
            .tag("namespace", cacheNamespace)
            .description("Number of cache writes")
            .register(meterRegistry);

        this.cacheEvictions = Counter.builder("cache.evictions")
            .tag("namespace", cacheNamespace)
            .description("Number of cache evictions")
            .register(meterRegistry);
    }

    /**
     * Finds entity in cache, or loads from database if not cached.
     * <p>
     * Cache-aside pattern:
     * 1. Check cache
     * 2. If hit, return cached value
     * 3. If miss, load from database
     * 4. Store in cache with TTL
     * 5. Return value
     */
    @SuppressWarnings("unchecked")
    protected Optional<T> findWithCache(TenantId tenantId, UUID entityId,
                                         java.util.function.Function<UUID, Optional<T>> databaseLoader) {
        // 1. Generate cache key
        String cacheKey = CacheKeyGenerator.forEntity(tenantId, cacheNamespace, entityId);

        // 2. Check cache first
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);

            if (cached != null) {
                cacheHits.increment();
                log.trace("Cache HIT for key: {}", cacheKey);
                return Optional.of((T) cached);
            }
        } catch (Exception e) {
            log.warn("Cache read failed for key: {}, falling back to database", cacheKey, e);
            // Fall through to database query
        }

        // 3. Cache miss - load from database
        cacheMisses.increment();
        log.trace("Cache MISS for key: {}", cacheKey);

        Optional<T> entity = databaseLoader.apply(entityId);

        // 4. Populate cache if entity exists
        if (entity.isPresent()) {
            try {
                redisTemplate.opsForValue().set(cacheKey, entity.get(), cacheTtl);
                cacheWrites.increment();
                log.trace("Cache WRITE for key: {}", cacheKey);
            } catch (Exception e) {
                log.error("Cache write failed for key: {}", cacheKey, e);
                // Don't throw - cache failure shouldn't break application
            }
        }

        return entity;
    }

    /**
     * Saves entity to database and updates cache (write-through).
     * <p>
     * Write-through pattern:
     * 1. Save to database (source of truth)
     * 2. Update cache immediately
     * 3. Return saved entity
     */
    protected T saveWithCache(TenantId tenantId, UUID entityId, T entity,
                              java.util.function.Function<T, T> databaseSaver) {
        // 1. Save to database first (source of truth)
        T savedEntity = databaseSaver.apply(entity);

        // 2. Update cache (write-through)
        String cacheKey = CacheKeyGenerator.forEntity(tenantId, cacheNamespace, entityId);

        try {
            redisTemplate.opsForValue().set(cacheKey, savedEntity, cacheTtl);
            cacheWrites.increment();
            log.trace("Cache WRITE (write-through) for key: {}", cacheKey);
        } catch (Exception e) {
            log.error("Cache write-through failed for key: {}", cacheKey, e);
            // Don't throw - cache failure shouldn't break application
        }

        return savedEntity;
    }

    /**
     * Deletes entity from database and evicts from cache.
     * <p>
     * Write-through delete:
     * 1. Delete from database
     * 2. Evict from cache
     */
    protected void deleteWithCache(TenantId tenantId, UUID entityId,
                                   java.util.function.Consumer<UUID> databaseDeleter) {
        // 1. Delete from database first
        databaseDeleter.accept(entityId);

        // 2. Evict from cache
        String cacheKey = CacheKeyGenerator.forEntity(tenantId, cacheNamespace, entityId);

        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (Boolean.TRUE.equals(deleted)) {
                cacheEvictions.increment();
                log.trace("Cache EVICTION for key: {}", cacheKey);
            }
        } catch (Exception e) {
            log.error("Cache eviction failed for key: {}", cacheKey, e);
            // Don't throw - cache failure shouldn't break application
        }
    }

    /**
     * Evicts cache key manually (for explicit invalidation).
     */
    protected void evictFromCache(TenantId tenantId, UUID entityId) {
        String cacheKey = CacheKeyGenerator.forEntity(tenantId, cacheNamespace, entityId);

        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (Boolean.TRUE.equals(deleted)) {
                cacheEvictions.increment();
                log.debug("Manual cache eviction for key: {}", cacheKey);
            }
        } catch (Exception e) {
            log.error("Manual cache eviction failed for key: {}", cacheKey, e);
        }
    }
}
```

---

## 3. Service-Specific Cached Repository Adapter

### 3.1 Cached User Repository Adapter

**File:** `/services/user-service/user-dataaccess/src/main/java/com/ccbsa/wms/user/dataaccess/adapter/CachedUserRepositoryAdapter.java`

```java
package com.ccbsa.wms.user.dataaccess.adapter;

import com.ccbsa.common.cache.decorator.CachedRepositoryDecorator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.user.application.service.port.repository.UserRepository;
import com.ccbsa.wms.user.domain.core.entity.User;
import com.ccbsa.wms.user.domain.core.valueobject.UserId;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Cached User Repository Adapter.
 * <p>
 * Decorates UserRepositoryAdapter with Redis caching.
 * Implements cache-aside pattern for reads and write-through for writes.
 * <p>
 * Cache Configuration:
 * - Namespace: "users"
 * - TTL: 15 minutes (configurable via application.yml)
 * - Eviction: LRU (configured in Redis)
 * <p>
 * Spring Configuration:
 * - @Primary: Ensures this adapter is injected instead of base adapter
 * - @Repository: Marks as Spring Data repository component
 */
@Repository
@Primary
public class CachedUserRepositoryAdapter
        extends CachedRepositoryDecorator<User, UserId>
        implements UserRepository {

    private final UserRepositoryAdapter baseRepository;

    public CachedUserRepositoryAdapter(
            UserRepositoryAdapter baseRepository,
            RedisTemplate<String, Object> redisTemplate,
            MeterRegistry meterRegistry
    ) {
        super(
            baseRepository,
            redisTemplate,
            CacheNamespace.USERS.getValue(),
            Duration.ofMinutes(15), // TTL from config
            meterRegistry
        );
        this.baseRepository = baseRepository;
    }

    /**
     * Finds user by ID with caching.
     * <p>
     * Cache key: tenant:{tenantId}:users:{userId}
     * <p>
     * Cache-aside pattern:
     * 1. Check cache
     * 2. If miss, load from database
     * 3. Populate cache with TTL
     */
    @Override
    public Optional<User> findById(UserId id) {
        throw new UnsupportedOperationException(
            "findById without tenantId is not supported in multi-tenant context"
        );
    }

    /**
     * Finds user by tenant ID and user ID with caching.
     * <p>
     * This is the primary lookup method in multi-tenant architecture.
     */
    @Override
    public Optional<User> findByTenantIdAndId(TenantId tenantId, UserId id) {
        return findWithCache(
            tenantId,
            id.getValue(),
            entityId -> baseRepository.findByTenantIdAndId(tenantId, id)
        );
    }

    /**
     * Finds all users for a tenant.
     * <p>
     * Note: Collection queries are NOT cached to avoid cache bloat.
     * Use view repositories with pagination for large datasets.
     */
    @Override
    public List<User> findByTenantId(TenantId tenantId) {
        // Delegate to base repository - no caching for collections
        return baseRepository.findByTenantId(tenantId);
    }

    /**
     * Saves user with write-through caching.
     * <p>
     * Write-through pattern:
     * 1. Save to database (source of truth)
     * 2. Update cache immediately
     * 3. Domain event published by command handler triggers collection cache invalidation
     */
    @Override
    public void save(User user) {
        // Use saveWithCache for entities that return the saved instance
        // For void save methods, manually handle cache update
        baseRepository.save(user);

        // Write-through cache update
        saveWithCache(
            user.getTenantId(),
            user.getId().getValue(),
            user,
            u -> u // Already saved, just return for cache
        );
    }

    /**
     * Deletes user with cache eviction.
     * <p>
     * Write-through delete:
     * 1. Delete from database
     * 2. Evict from cache
     */
    @Override
    public void deleteById(UserId id) {
        // Delete requires tenant context
        throw new UnsupportedOperationException(
            "deleteById without tenantId is not supported in multi-tenant context"
        );
    }

    /**
     * Checks if user exists (cached).
     * <p>
     * Cache key: tenant:{tenantId}:users:{userId}
     * Returns true if cached, otherwise queries database.
     */
    @Override
    public boolean existsById(UserId id) {
        // Delegate to base repository
        return baseRepository.existsById(id);
    }
}
```

---

## 4. Configuration and Bean Wiring

### 4.1 Service-Specific Cache Configuration

**File:** `/services/user-service/user-container/src/main/java/com/ccbsa/wms/user/config/UserCacheConfiguration.java`

```java
package com.ccbsa.wms.user.config;

import com.ccbsa.common.cache.config.CacheConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

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
```

### 4.2 Application Configuration (application.yml)

**File:** `/services/user-service/user-container/src/main/resources/application.yml`

```yaml
# Existing configuration...

# Cache Configuration
wms:
  cache:
    enabled: true
    default-ttl-minutes: 30
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: 0
    cache-configs:
      users:
        ttl-minutes: 15        # User entities cached for 15 minutes
      user-roles:
        ttl-minutes: 30        # Role assignments cached for 30 minutes
      user-permissions:
        ttl-minutes: 60        # Permissions cached for 1 hour (rarely change)

# Spring Cache Configuration
spring:
  cache:
    type: redis
    redis:
      time-to-live: 1800000    # Default TTL: 30 minutes (in milliseconds)
      cache-null-values: false
      use-key-prefix: true
      key-prefix: "wms:"
```

---

## 5. Testing Cached Repository Adapters

### 5.1 Unit Test for Cached Adapter

**File:** `/services/user-service/user-dataaccess/src/test/java/com/ccbsa/wms/user/dataaccess/adapter/CachedUserRepositoryAdapterTest.java`

```java
package com.ccbsa.wms.user.dataaccess.adapter;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.user.domain.core.entity.User;
import com.ccbsa.wms.user.domain.core.valueobject.UserId;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CachedUserRepositoryAdapterTest {

    @Mock
    private UserRepositoryAdapter baseRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private CachedUserRepositoryAdapter cachedRepository;

    private TenantId tenantId;
    private UserId userId;
    private User user;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cachedRepository = new CachedUserRepositoryAdapter(
            baseRepository,
            redisTemplate,
            new SimpleMeterRegistry()
        );

        tenantId = TenantId.of("test-tenant");
        userId = UserId.of(UUID.randomUUID());
        user = createTestUser(tenantId, userId);
    }

    @Test
    void findByTenantIdAndId_CacheHit_ShouldReturnCachedUser() {
        // Given: User is in cache
        String expectedKey = "tenant:test-tenant:users:" + userId.getValue();
        when(valueOperations.get(expectedKey)).thenReturn(user);

        // When: Finding user
        Optional<User> result = cachedRepository.findByTenantIdAndId(tenantId, userId);

        // Then: Returns cached user without database query
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(user);

        verify(valueOperations).get(expectedKey);
        verifyNoInteractions(baseRepository); // Database not called
    }

    @Test
    void findByTenantIdAndId_CacheMiss_ShouldLoadFromDatabaseAndCache() {
        // Given: User not in cache, but exists in database
        String expectedKey = "tenant:test-tenant:users:" + userId.getValue();
        when(valueOperations.get(expectedKey)).thenReturn(null); // Cache miss
        when(baseRepository.findByTenantIdAndId(tenantId, userId)).thenReturn(Optional.of(user));

        // When: Finding user
        Optional<User> result = cachedRepository.findByTenantIdAndId(tenantId, userId);

        // Then: Loads from database and populates cache
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(user);

        verify(valueOperations).get(expectedKey);
        verify(baseRepository).findByTenantIdAndId(tenantId, userId);
        verify(valueOperations).set(eq(expectedKey), eq(user), any());
    }

    @Test
    void save_ShouldUpdateDatabaseAndCache() {
        // When: Saving user
        cachedRepository.save(user);

        // Then: Saves to database and updates cache
        verify(baseRepository).save(user);

        String expectedKey = "tenant:test-tenant:users:" + userId.getValue();
        verify(valueOperations).set(eq(expectedKey), eq(user), any());
    }

    private User createTestUser(TenantId tenantId, UserId userId) {
        // Create test user using builder
        // Implementation depends on your User entity structure
        return User.builder()
            .userId(userId)
            .tenantId(tenantId)
            .build();
    }
}
```

---

## 6. Best Practices

### 6.1 When to Cache

**DO Cache:**
- ✅ Single entity lookups by ID (high read frequency)
- ✅ Reference data (products, locations, rarely changing)
- ✅ User sessions and permissions (frequently accessed)
- ✅ Configuration data (tenant settings, feature flags)

**DON'T Cache:**
- ❌ Large collections (>1000 items) - use pagination instead
- ❌ Real-time data (stock levels, order status) - short TTL only
- ❌ Transient data (cart contents) - use session storage
- ❌ Aggregations (reports, analytics) - compute on-demand

### 6.2 TTL Guidelines

| Data Type | TTL | Rationale |
|-----------|-----|-----------|
| User entities | 15 minutes | Moderate change frequency, frequent access |
| Product catalog | 60 minutes | Low change frequency, high read frequency |
| Stock levels | 5 minutes | High change frequency, eventual consistency acceptable |
| User permissions | 60 minutes | Low change frequency, security-critical |
| Tenant configuration | 30 minutes | Rare changes, moderate access frequency |

### 6.3 Error Handling

**Cache Failure Policy:**

```java
try {
    // Cache operation
    Object cached = redisTemplate.opsForValue().get(cacheKey);
} catch (Exception e) {
    log.warn("Cache operation failed, falling back to database", e);
    // Don't throw - cache failure shouldn't break application
    // Fall through to database query
}
```

**Key Principles:**

- ✅ **Graceful degradation** - Cache failure falls back to database
- ✅ **Log warnings** - Alert operators to cache issues
- ✅ **Don't throw exceptions** - Cache is optimization, not requirement
- ✅ **Monitor metrics** - Track cache hit/miss rates

---

**End of Section 4**

Next sections will cover:
- Multi-Tenant Caching Patterns
- Cache Warming
- Monitoring and Observability
