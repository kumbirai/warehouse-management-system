# Cache Invalidation Strategy

## Warehouse Management System - Event-Driven Cache Invalidation

**Document Version:** 1.0
**Date:** 2025-12-09
**Status:** Approved

---

## Overview

This document defines the **mandatory** event-driven cache invalidation strategy. Cache invalidation is critical for maintaining data consistency across distributed services in a
CQRS architecture.

**Core Principle:** Cache invalidation is **event-driven**, leveraging the existing Kafka infrastructure to ensure eventual consistency between write models (database) and read
models (cache).

---

## 1. Invalidation Patterns

### 1.1 Write-Through Invalidation (Same Service)

**When:** Write operation (Command Handler) modifies an aggregate

**Pattern:**

```java
@Service
public class UpdateUserCommandHandler implements CommandHandler<UpdateUserCommand> {

    private final UserRepository userRepository;          // Cached repository
    private final UserEventPublisher eventPublisher;

    @Override
    @Transactional
    public void handle(UpdateUserCommand command) {
        // 1. Load aggregate from cache/database
        User user = userRepository.findById(command.getUserId())
            .orElseThrow(() -> new UserNotFoundException(command.getUserId()));

        // 2. Execute domain logic
        user.updateEmail(command.getEmail());
        user.updateProfile(command.getProfile());

        // 3. Save to database (write-through cache update)
        userRepository.save(user);
        // Cache is automatically updated via CachedRepositoryDecorator

        // 4. Publish domain event (triggers cache invalidation across services)
        eventPublisher.publish(new UserUpdatedEvent(user));
    }
}
```

**CachedRepositoryDecorator Behavior:**

```java
@Override
public void save(User user) {
    // 1. Persist to database (source of truth)
    baseRepository.save(user);

    // 2. Update cache (write-through)
    String cacheKey = CacheKeyGenerator.forEntity(
        user.getTenantId(),
        CacheNamespace.USERS.getValue(),
        user.getId().getValue()
    );
    cacheManager.put(cacheKey, user);

    log.debug("Cache updated for user: {} in tenant: {}",
        user.getId().getValue(), user.getTenantId().getValue());
}
```

**Cache State After Write:**

- ✅ Single entity cache updated immediately (strong consistency)
- ⏳ Collection/query caches invalidated asynchronously (eventual consistency)

---

### 1.2 Event-Driven Invalidation (Cross-Service)

**When:** Domain event published from another service

**Pattern:**

```java
@Component
public class UserEventListener {

    private final CacheManager cacheManager;
    private final LocalCacheInvalidator cacheInvalidator;

    /**
     * Listens to UserUpdatedEvent and invalidates affected caches.
     * <p>
     * Invalidation Strategy:
     * 1. Invalidate single entity cache (tenant:123:users:uuid)
     * 2. Invalidate all collection caches for this tenant (tenant:123:users:*)
     * 3. Invalidate derived caches (e.g., user-roles, user-permissions)
     */
    @KafkaListener(topics = "user-events", groupId = "user-service-cache-invalidation")
    public void handleUserUpdatedEvent(UserUpdatedEvent event) {
        TenantId tenantId = TenantId.of(event.getTenantId());
        UserId userId = UserId.of(event.getUserId());

        log.info("Invalidating caches for user: {} in tenant: {}",
            userId.getValue(), tenantId.getValue());

        // 1. Invalidate single entity cache
        cacheInvalidator.invalidateEntity(
            tenantId,
            CacheNamespace.USERS.getValue(),
            userId.getValue()
        );

        // 2. Invalidate collection caches (all user queries for this tenant)
        cacheInvalidator.invalidateCollection(
            tenantId,
            CacheNamespace.USERS.getValue()
        );

        // 3. Invalidate derived caches
        cacheInvalidator.invalidateCollection(
            tenantId,
            CacheNamespace.USER_ROLES.getValue()
        );
        cacheInvalidator.invalidateCollection(
            tenantId,
            CacheNamespace.USER_PERMISSIONS.getValue()
        );

        log.info("Cache invalidation completed for user: {}", userId.getValue());
    }
}
```

**Kafka Topic Structure:**

| Service         | Topic Name       | Events Published                                                                        |
|-----------------|------------------|-----------------------------------------------------------------------------------------|
| User Service    | `user-events`    | `UserCreatedEvent`, `UserUpdatedEvent`, `UserDeactivatedEvent`, `UserRoleAssignedEvent` |
| Tenant Service  | `tenant-events`  | `TenantCreatedEvent`, `TenantActivatedEvent`, `TenantDeactivatedEvent`                  |
| Product Service | `product-events` | `ProductCreatedEvent`, `ProductUpdatedEvent`, `ProductDiscontinuedEvent`                |
| Stock Service   | `stock-events`   | `StockConsignmentCreatedEvent`, `StockLevelUpdatedEvent`                                |

---

### 1.3 Cascade Invalidation (Dependent Caches)

**When:** One entity change affects multiple related caches

**Example:** Product update affects stock level caches

```java
@Component
public class ProductEventListener {

    private final LocalCacheInvalidator cacheInvalidator;

    @KafkaListener(topics = "product-events", groupId = "stock-service-cache-invalidation")
    public void handleProductUpdatedEvent(ProductUpdatedEvent event) {
        TenantId tenantId = TenantId.of(event.getTenantId());
        ProductId productId = ProductId.of(event.getProductId());

        log.info("Cascade invalidation: Product {} updated in tenant {}",
            productId.getValue(), tenantId.getValue());

        // 1. Invalidate product caches (even though this is stock-service)
        cacheInvalidator.invalidateEntity(
            tenantId,
            CacheNamespace.PRODUCTS.getValue(),
            productId.getValue()
        );

        // 2. Invalidate stock consignments for this product
        cacheInvalidator.invalidateByPattern(
            String.format("tenant:%s:stock-consignments:product:%s:*",
                tenantId.getValue(), productId.getValue())
        );

        // 3. Invalidate stock level aggregations
        cacheInvalidator.invalidateCollection(
            tenantId,
            CacheNamespace.STOCK_LEVELS.getValue()
        );
    }
}
```

**Cascade Invalidation Rules:**

| Event                    | Originating Service | Affected Caches                            | Services to Invalidate             |
|--------------------------|---------------------|--------------------------------------------|------------------------------------|
| `UserUpdatedEvent`       | user-service        | users, user-roles, user-permissions        | user-service, notification-service |
| `ProductUpdatedEvent`    | product-service     | products, stock-consignments, stock-levels | product-service, stock-service     |
| `TenantDeactivatedEvent` | tenant-service      | ALL tenant caches                          | ALL services                       |
| `LocationUpdatedEvent`   | location-service    | locations, stock-consignments              | location-service, stock-service    |

---

## 2. Local Cache Invalidator

### 2.1 Implementation

**File:** `/common/common-cache/src/main/java/com/ccbsa/common/cache/invalidation/LocalCacheInvalidator.java`

```java
package com.ccbsa.common.cache.invalidation;

import com.ccbsa.common.cache.key.CacheKeyGenerator;
import com.ccbsa.common.domain.valueobject.TenantId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Local Cache Invalidator.
 * <p>
 * Provides methods for invalidating caches in the current service.
 * Supports:
 * - Single entity invalidation
 * - Collection invalidation (all queries for a namespace)
 * - Pattern-based invalidation (wildcard matching)
 * - Tenant-wide invalidation (all caches for a tenant)
 * <p>
 * Thread-safe and idempotent.
 */
@Component
public class LocalCacheInvalidator {

    private static final Logger log = LoggerFactory.getLogger(LocalCacheInvalidator.class);

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    public LocalCacheInvalidator(CacheManager cacheManager,
                                 RedisTemplate<String, Object> redisTemplate) {
        this.cacheManager = cacheManager;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Invalidates cache for a single entity.
     * <p>
     * Example: Invalidate user cache
     * - Tenant: acme-corp
     * - Namespace: users
     * - Entity ID: 550e8400-e29b-41d4-a716-446655440000
     * - Invalidated Key: tenant:acme-corp:users:550e8400-e29b-41d4-a716-446655440000
     */
    public void invalidateEntity(TenantId tenantId, String namespace, UUID entityId) {
        String cacheKey = CacheKeyGenerator.forEntity(tenantId, namespace, entityId);

        Boolean deleted = redisTemplate.delete(cacheKey);

        if (Boolean.TRUE.equals(deleted)) {
            log.debug("Invalidated entity cache: {}", cacheKey);
        } else {
            log.trace("No cache entry found for: {}", cacheKey);
        }
    }

    /**
     * Invalidates all collection caches for a namespace.
     * <p>
     * Example: Invalidate all user query caches
     * - Tenant: acme-corp
     * - Namespace: users
     * - Invalidated Keys: tenant:acme-corp:users:* (all user queries)
     */
    public void invalidateCollection(TenantId tenantId, String namespace) {
        String pattern = CacheKeyGenerator.wildcardPattern(tenantId, namespace);
        invalidateByPattern(pattern);
    }

    /**
     * Invalidates all caches matching a pattern.
     * <p>
     * Example: Invalidate all stock consignments for a product
     * - Pattern: tenant:acme-corp:stock-consignments:product:12345:*
     * <p>
     * Warning: Pattern matching scans all Redis keys, use sparingly.
     */
    public void invalidateByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null && !keys.isEmpty()) {
            Long deletedCount = redisTemplate.delete(keys);
            log.info("Invalidated {} cache entries matching pattern: {}",
                deletedCount, pattern);
        } else {
            log.trace("No cache entries found matching pattern: {}", pattern);
        }
    }

    /**
     * Invalidates ALL caches for a tenant (use with extreme caution).
     * <p>
     * Example: Tenant deactivation - invalidate all tenant data
     * - Tenant: acme-corp
     * - Invalidated Keys: tenant:acme-corp:* (ALL caches for this tenant)
     * <p>
     * Warning: This is a heavy operation, only use for tenant lifecycle events.
     */
    public void invalidateTenant(TenantId tenantId) {
        String pattern = String.format("tenant:%s:*", tenantId.getValue());

        log.warn("Invalidating ALL caches for tenant: {}", tenantId.getValue());

        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null && !keys.isEmpty()) {
            Long deletedCount = redisTemplate.delete(keys);
            log.warn("Invalidated {} cache entries for tenant: {}",
                deletedCount, tenantId.getValue());
        }
    }

    /**
     * Invalidates a specific cache by exact key.
     * <p>
     * Use when you have the exact cache key (e.g., from a custom cache key generator).
     */
    public void invalidateByKey(String cacheKey) {
        Boolean deleted = redisTemplate.delete(cacheKey);

        if (Boolean.TRUE.equals(deleted)) {
            log.debug("Invalidated cache: {}", cacheKey);
        } else {
            log.trace("No cache entry found for: {}", cacheKey);
        }
    }
}
```

---

## 3. Cache Invalidation Event Listener Base Class

### 3.1 Base Event Listener

**File:** `/common/common-cache/src/main/java/com/ccbsa/common/cache/invalidation/CacheInvalidationEventListener.java`

```java
package com.ccbsa.common.cache.invalidation;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.TenantId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for event-driven cache invalidation listeners.
 * <p>
 * Provides common functionality for invalidating caches based on domain events.
 * <p>
 * Usage:
 * <pre>
 * @Component
 * public class UserCacheInvalidationListener extends CacheInvalidationEventListener {
 *
 *     @KafkaListener(topics = "user-events")
 *     public void handleUserEvent(UserUpdatedEvent event) {
 *         invalidateForEvent(event, "users");
 *     }
 * }
 * </pre>
 */
public abstract class CacheInvalidationEventListener {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationEventListener.class);

    protected final LocalCacheInvalidator cacheInvalidator;

    protected CacheInvalidationEventListener(LocalCacheInvalidator cacheInvalidator) {
        this.cacheInvalidator = cacheInvalidator;
    }

    /**
     * Invalidates caches for a domain event.
     * <p>
     * Default strategy:
     * 1. Invalidate single entity cache (tenant:123:namespace:uuid)
     * 2. Invalidate all collection caches (tenant:123:namespace:*)
     * <p>
     * Override this method for custom invalidation logic.
     */
    protected void invalidateForEvent(DomainEvent event, String namespace) {
        TenantId tenantId = extractTenantId(event);

        log.debug("Invalidating caches for event: {} in namespace: {}",
            event.getClass().getSimpleName(), namespace);

        // Strategy 1: Invalidate single entity if event contains aggregate ID
        if (event.getAggregateId() != null) {
            try {
                java.util.UUID aggregateId = java.util.UUID.fromString(event.getAggregateId());
                cacheInvalidator.invalidateEntity(tenantId, namespace, aggregateId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid aggregate ID format in event: {}", event.getAggregateId());
            }
        }

        // Strategy 2: Invalidate all collection caches for this namespace
        cacheInvalidator.invalidateCollection(tenantId, namespace);
    }

    /**
     * Invalidates multiple namespaces for a single event.
     * <p>
     * Use for cascade invalidation (e.g., product update affects stock caches).
     */
    protected void invalidateForEvent(DomainEvent event, String... namespaces) {
        for (String namespace : namespaces) {
            invalidateForEvent(event, namespace);
        }
    }

    /**
     * Extracts tenant ID from domain event.
     * <p>
     * Override if your events use a different field name.
     */
    protected TenantId extractTenantId(DomainEvent event) {
        // Assuming domain events have a getTenantId() method
        // Adjust based on your actual event structure
        try {
            java.lang.reflect.Method getTenantIdMethod = event.getClass().getMethod("getTenantId");
            String tenantId = (String) getTenantIdMethod.invoke(event);
            return TenantId.of(tenantId);
        } catch (Exception e) {
            log.error("Failed to extract tenant ID from event: {}",
                event.getClass().getSimpleName(), e);
            throw new IllegalArgumentException("Event must contain tenant ID", e);
        }
    }
}
```

---

## 4. Service-Specific Event Listeners

### 4.1 User Service Cache Invalidation Listener

**File:** `/services/user-service/user-container/src/main/java/com/ccbsa/wms/user/cache/UserCacheInvalidationListener.java`

```java
package com.ccbsa.wms.user.cache;

import com.ccbsa.common.cache.invalidation.CacheInvalidationEventListener;
import com.ccbsa.common.cache.invalidation.LocalCacheInvalidator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.wms.user.domain.event.UserCreatedEvent;
import com.ccbsa.wms.user.domain.event.UserDeactivatedEvent;
import com.ccbsa.wms.user.domain.event.UserRoleAssignedEvent;
import com.ccbsa.wms.user.domain.event.UserRoleRemovedEvent;
import com.ccbsa.wms.user.domain.event.UserUpdatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * User Service Cache Invalidation Listener.
 * <p>
 * Listens to user domain events and invalidates affected caches.
 * <p>
 * Invalidation Strategy:
 * - UserCreatedEvent: No invalidation needed (cache-aside pattern)
 * - UserUpdatedEvent: Invalidate user entity + all user collections
 * - UserDeactivatedEvent: Invalidate user entity + all user collections
 * - UserRoleAssignedEvent: Invalidate user roles + permissions
 * - UserRoleRemovedEvent: Invalidate user roles + permissions
 */
@Component
public class UserCacheInvalidationListener extends CacheInvalidationEventListener {

    public UserCacheInvalidationListener(LocalCacheInvalidator cacheInvalidator) {
        super(cacheInvalidator);
    }

    @KafkaListener(
        topics = "user-events",
        groupId = "user-service-cache-invalidation",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserEvent(Object event) {
        if (event instanceof UserCreatedEvent userCreated) {
            handleUserCreated(userCreated);
        } else if (event instanceof UserUpdatedEvent userUpdated) {
            handleUserUpdated(userUpdated);
        } else if (event instanceof UserDeactivatedEvent userDeactivated) {
            handleUserDeactivated(userDeactivated);
        } else if (event instanceof UserRoleAssignedEvent roleAssigned) {
            handleRoleAssigned(roleAssigned);
        } else if (event instanceof UserRoleRemovedEvent roleRemoved) {
            handleRoleRemoved(roleRemoved);
        }
    }

    private void handleUserCreated(UserCreatedEvent event) {
        // No cache invalidation needed - cache-aside pattern
        // Cache will be populated on first read
        log.debug("User created, cache-aside pattern - no invalidation");
    }

    private void handleUserUpdated(UserUpdatedEvent event) {
        // Invalidate user entity cache and all user collection caches
        invalidateForEvent(event,
            CacheNamespace.USERS.getValue()
        );
    }

    private void handleUserDeactivated(UserDeactivatedEvent event) {
        // Invalidate user entity cache and all user collection caches
        invalidateForEvent(event,
            CacheNamespace.USERS.getValue()
        );
    }

    private void handleRoleAssigned(UserRoleAssignedEvent event) {
        // Invalidate user roles and permissions (cascade invalidation)
        invalidateForEvent(event,
            CacheNamespace.USERS.getValue(),
            CacheNamespace.USER_ROLES.getValue(),
            CacheNamespace.USER_PERMISSIONS.getValue()
        );
    }

    private void handleRoleRemoved(UserRoleRemovedEvent event) {
        // Invalidate user roles and permissions (cascade invalidation)
        invalidateForEvent(event,
            CacheNamespace.USERS.getValue(),
            CacheNamespace.USER_ROLES.getValue(),
            CacheNamespace.USER_PERMISSIONS.getValue()
        );
    }
}
```

---

## 5. Invalidation Strategies Summary

### 5.1 Invalidation Matrix

| Event Type                       | Invalidation Scope                       | Consistency Model | Latency   |
|----------------------------------|------------------------------------------|-------------------|-----------|
| **Write-Through** (same service) | Single entity + immediate write to cache | Strong            | <10ms     |
| **Event-Driven** (cross-service) | Entity + collections via Kafka events    | Eventual          | 50-200ms  |
| **Cascade** (dependent caches)   | Multiple namespaces via event listeners  | Eventual          | 50-200ms  |
| **Tenant Deactivation**          | All tenant caches (pattern match)        | Eventual          | 100-500ms |

### 5.2 Best Practices

**DO:**

- ✅ Use write-through caching for single entity writes
- ✅ Publish domain events after successful database commits
- ✅ Invalidate collections conservatively (entire namespace)
- ✅ Use cascade invalidation for denormalized data
- ✅ Log all invalidation operations for debugging

**DON'T:**

- ❌ Don't invalidate caches before database commit (risk of stale data)
- ❌ Don't use pattern matching for high-frequency invalidation (performance)
- ❌ Don't invalidate global caches without careful consideration
- ❌ Don't skip invalidation in event listeners (eventual consistency broken)

---

**End of Section 3**

Next sections will cover:

- Repository Adapter Decorator Pattern
- Multi-Tenant Caching Patterns
- Cache Warming
- Monitoring and Observability
