# Common Cache Module

## Overview

The `common-cache` module provides production-grade distributed caching infrastructure for the Warehouse Management System. It implements tenant-aware caching using Redis with
Spring Cache abstraction.

## Features

- **Tenant-Aware Caching**: Automatic tenant isolation via cache key prefixes
- **Decorator Pattern**: Transparent caching for repository adapters
- **Event-Driven Invalidation**: Cache invalidation via Kafka domain events
- **Write-Through Caching**: Strong consistency for write operations
- **Cache-Aside Pattern**: Optimized read performance
- **Metrics Integration**: Micrometer metrics for cache operations
- **Health Checks**: Redis connectivity monitoring

## Architecture

This module follows Clean Hexagonal Architecture principles:

- Cache is an infrastructure concern, transparent to domain layer
- Repository decorators implement caching without polluting domain logic
- Event-driven invalidation maintains eventual consistency

## Usage

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.ccbsa.wms</groupId>
    <artifactId>common-cache</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 2. Configure Cache

```yaml
wms:
  cache:
    enabled: true
    default-ttl-minutes: 30
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
    cache-configs:
      users:
        ttl-minutes: 15
```

### 3. Create Cached Repository Adapter

```java
@Repository
@Primary
public class CachedUserRepositoryAdapter
        extends CachedRepositoryDecorator<User, UserId>
        implements UserRepository {
    // Implementation
}
```

### 4. Create Cache Invalidation Listener

```java
@Component
public class UserCacheInvalidationListener extends CacheInvalidationEventListener {
    @KafkaListener(topics = "user-events")
    public void handleUserEvent(UserUpdatedEvent event) {
        invalidateForEvent(event, CacheNamespace.USERS.getValue());
    }
}
```

## Documentation

For complete implementation guide, see:

- [Production Caching Strategy](../../documentation/caching/01-production-caching-strategy.md)
- [Repository Adapter Decorator Pattern](../../documentation/caching/04-repository-adapter-decorator-pattern.md)
- [Cache Invalidation Strategy](../../documentation/caching/03-cache-invalidation-strategy.md)
