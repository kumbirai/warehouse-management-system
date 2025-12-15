# Service Caching Implementation Checklist

## Warehouse Management System - Caching Implementation Guide

**Document Version:** 1.0  
**Date:** 2025-12-09  
**Status:** Approved

---

## Overview

This checklist provides step-by-step instructions for implementing mandatory distributed caching in any WMS service. Follow this checklist to ensure consistent caching
implementation across all services.

**Prerequisites:**

- Service has repository adapters implemented
- Service has domain events defined
- Service has messaging module for event publishing/consumption

---

## Implementation Steps

### Step 1: Add Dependencies

#### 1.1 Update `{service}-dataaccess/pom.xml`

Add `common-cache` dependency:

```xml
<dependency>
    <groupId>com.ccbsa.wms</groupId>
    <artifactId>common-cache</artifactId>
    <version>${project.version}</version>
</dependency>
```

#### 1.2 Update `{service}-messaging/pom.xml`

Add `common-cache` dependency:

```xml
<dependency>
    <groupId>com.ccbsa.wms</groupId>
    <artifactId>common-cache</artifactId>
    <version>${project.version}</version>
</dependency>
```

#### 1.3 Update `{service}-container/pom.xml`

Add `common-cache` dependency:

```xml
<dependency>
    <groupId>com.ccbsa.wms</groupId>
    <artifactId>common-cache</artifactId>
    <version>${project.version}</version>
</dependency>
```

---

### Step 2: Create Cached Repository Adapter

#### 2.1 Create File

**Location:** `/{service}-service/{service}-dataaccess/src/main/java/com/ccbsa/wms/{service}/dataaccess/adapter/Cached{Entity}RepositoryAdapter.java`

#### 2.2 Implementation Template

```java
package com.ccbsa.wms.{service}.dataaccess.adapter;

import com.ccbsa.common.cache.decorator.CachedRepositoryDecorator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.{service}.application.service.port.repository.{Entity}Repository;
import com.ccbsa.wms.{service}.domain.core.entity.{Entity};
import com.ccbsa.wms.{service}.domain.core.valueobject.{EntityId};
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@Primary
public class Cached{Entity}RepositoryAdapter
        extends CachedRepositoryDecorator<{Entity}, {EntityId}>
        implements {Entity}Repository {

    private static final Logger log = LoggerFactory.getLogger(Cached{Entity}RepositoryAdapter.class);

    private final {Entity}RepositoryAdapter baseRepository;

    public Cached{Entity}RepositoryAdapter(
            {Entity}RepositoryAdapter baseRepository,
            RedisTemplate<String, Object> redisTemplate,
            MeterRegistry meterRegistry
    ) {
        super(
            baseRepository,
            redisTemplate,
            CacheNamespace.{NAMESPACE}.getValue(),
            Duration.ofMinutes({TTL}), // Configure TTL from application.yml
            meterRegistry
        );
        this.baseRepository = baseRepository;
    }

    @Override
    public Optional<{Entity}> findByTenantIdAndId(TenantId tenantId, {EntityId} id) {
        return findWithCache(
            tenantId,
            id.getValue(),
            entityId -> baseRepository.findByTenantIdAndId(tenantId, id)
        );
    }

    @Override
    public void save({Entity} entity) {
        // Save to database first
        baseRepository.save(entity);

        // Write-through cache update
        if (entity.getTenantId() != null && entity.getId() != null) {
            String cacheKey = com.ccbsa.common.cache.key.CacheKeyGenerator.forEntity(
                entity.getTenantId(),
                CacheNamespace.{NAMESPACE}.getValue(),
                entity.getId().getValue()
            );

            try {
                redisTemplate.opsForValue().set(cacheKey, entity, Duration.ofMinutes({TTL}));
                log.trace("Cache WRITE (write-through) for key: {}", cacheKey);
            } catch (Exception e) {
                log.error("Cache write-through failed for key: {}", cacheKey, e);
            }
        }
    }

    // Delegate other methods to baseRepository
    // Only cache frequently accessed single-entity lookups
}
```

#### 2.3 Key Points

- ✅ Extend `CachedRepositoryDecorator<Entity, EntityId>`
- ✅ Implement repository port interface
- ✅ Annotate with `@Repository` and `@Primary`
- ✅ Use `findWithCache()` for cached lookups
- ✅ Use write-through pattern for `save()` method
- ✅ Delegate collection queries to base repository (don't cache)

---

### Step 3: Create Cache Invalidation Listener

#### 3.1 Create File

**Location:** `/{service}-service/{service}-messaging/src/main/java/com/ccbsa/wms/{service}/messaging/listener/{Service}CacheInvalidationListener.java`

#### 3.2 Implementation Template

```java
package com.ccbsa.wms.{service}.messaging.listener;

import com.ccbsa.common.cache.invalidation.CacheInvalidationEventListener;
import com.ccbsa.common.cache.invalidation.LocalCacheInvalidator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.wms.{service}.domain.core.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class {Service}CacheInvalidationListener extends CacheInvalidationEventListener {

    private static final Logger log = LoggerFactory.getLogger({Service}CacheInvalidationListener.class);

    public {Service}CacheInvalidationListener(LocalCacheInvalidator cacheInvalidator) {
        super(cacheInvalidator);
    }

    @KafkaListener(
        topics = "{service}-events",
        groupId = "{service}-service-cache-invalidation",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handle{Service}Event(Object event) {
        if (event instanceof {Entity}CreatedEvent created) {
            handle{Entity}Created(created);
        } else if (event instanceof {Entity}UpdatedEvent updated) {
            handle{Entity}Updated(updated);
        } else if (event instanceof {Entity}DeletedEvent deleted) {
            handle{Entity}Deleted(deleted);
        }
    }

    private void handle{Entity}Created({Entity}CreatedEvent event) {
        // No cache invalidation needed - cache-aside pattern
        log.debug("{Entity} created, cache-aside pattern - no invalidation");
    }

    private void handle{Entity}Updated({Entity}UpdatedEvent event) {
        invalidateForEvent(event, CacheNamespace.{NAMESPACE}.getValue());
    }

    private void handle{Entity}Deleted({Entity}DeletedEvent event) {
        invalidateForEvent(event, CacheNamespace.{NAMESPACE}.getValue());
    }
}
```

#### 3.3 Key Points

- ✅ Extend `CacheInvalidationEventListener`
- ✅ Annotate with `@Component`
- ✅ Add `@KafkaListener` for service domain events topic
- ✅ Handle each event type appropriately
- ✅ Use `invalidateForEvent()` helper method

---

### Step 4: Create Cache Warming Service (Optional)

#### 4.1 Create File

**Location:** `/{service}-service/{service}-container/src/main/java/com/ccbsa/wms/{service}/cache/{Service}CacheWarmingService.java`

#### 4.2 Implementation Template

```java
package com.ccbsa.wms.{service}.cache;

import com.ccbsa.common.cache.warming.CacheWarmingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class {Service}CacheWarmingService extends CacheWarmingService {

    private static final Logger log = LoggerFactory.getLogger({Service}CacheWarmingService.class);

    @Override
    protected void performCacheWarming() {
        log.info("Warming {service} caches...");
        // Implement warming logic for frequently accessed data
        log.info("{Service} cache warming completed");
    }
}
```

---

### Step 5: Create Cache Configuration

#### 5.1 Create File

**Location:** `/{service}-service/{service}-container/src/main/java/com/ccbsa/wms/{service}/config/{Service}CacheConfiguration.java`

#### 5.2 Implementation Template

```java
package com.ccbsa.wms.{service}.config;

import com.ccbsa.common.cache.config.CacheConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CacheConfiguration.class)
public class {Service}CacheConfiguration {
    // Common cache configuration imported from common-cache module
}
```

---

### Step 6: Update Application Configuration

#### 6.1 Update `application.yml`

**Location:** `/{service}-service/{service}-container/src/main/resources/application.yml`

#### 6.2 Add Configuration

```yaml
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
      {namespace}:
        ttl-minutes: {value}  # Configure per-cache TTL

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

### Step 7: Update Cache Namespace Enum

#### 7.1 Update File

**Location:** `/common/common-cache/src/main/java/com/ccbsa/common/cache/key/CacheNamespace.java`

#### 7.2 Add Namespace

```java
// {Service} Service
{NAMESPACE}("{namespace}"),
```

---

### Step 8: Write Unit Tests

#### 8.1 Create Test File

**Location:** `/{service}-service/{service}-dataaccess/src/test/java/com/ccbsa/wms/{service}/dataaccess/adapter/Cached{Entity}RepositoryAdapterTest.java`

#### 8.2 Test Scenarios

- ✅ Cache hit (returns cached data)
- ✅ Cache miss (loads from database, populates cache)
- ✅ Write-through (updates cache on save)
- ✅ Cache eviction (removes from cache on delete)
- ✅ Redis failure (graceful degradation)
- ✅ Multi-tenant isolation (no cross-tenant leakage)

---

## Verification Checklist

After implementation, verify:

- [ ] Cached repository adapter is annotated with `@Primary`
- [ ] Cache invalidation listener is subscribed to correct Kafka topic
- [ ] Cache configuration is added to `application.yml`
- [ ] Cache namespace is added to `CacheNamespace` enum
- [ ] Unit tests cover cache hit/miss scenarios
- [ ] Application starts without errors
- [ ] Redis health check endpoint responds (`/actuator/health/redis`)
- [ ] Cache metrics are exposed (`/actuator/metrics/cache.*`)

---

## Common Issues and Solutions

### Issue: Cache not working

**Solution:**

- Verify Redis is running and accessible
- Check `wms.cache.enabled` is `true` in `application.yml`
- Verify `@Primary` annotation on cached adapter
- Check logs for cache operation errors

### Issue: Stale data in cache

**Solution:**

- Verify cache invalidation listener is consuming events
- Check Kafka consumer group health
- Verify event handlers call `invalidateForEvent()`

### Issue: Multi-tenant cache leakage

**Solution:**

- Verify tenant prefix in all cache keys
- Check `TenantContext` is set correctly
- Verify cache key generation includes tenant ID

---

## References

- [Production Caching Strategy](./01-production-caching-strategy.md)
- [Repository Adapter Decorator Pattern](./04-repository-adapter-decorator-pattern.md)
- [Cache Invalidation Strategy](./03-cache-invalidation-strategy.md)
- [Complete Implementation Guide](./05-complete-implementation-guide.md)

---

**End of Implementation Checklist**
