# Caching Developer Guide

## Warehouse Management System - Quick Start Guide for Developers

**Document Version:** 1.0  
**Date:** 2025-12-09  
**Status:** Approved

---

## Quick Start

### 1. Understanding the Caching Architecture

The WMS uses a **decorator pattern** for caching that maintains clean hexagonal architecture:

```
Application Service → Repository Port ← Cached Adapter → Base Adapter → Database
                                            ↓
                                        Redis Cache
```

**Key Principles:**

- ✅ Domain layer is cache-agnostic
- ✅ Application layer works with port interfaces (unaware of caching)
- ✅ Infrastructure adapters handle caching transparently
- ✅ Cache invalidation driven by domain events

### 2. Common Patterns

#### Pattern 1: Cached Entity Lookup

```java
@Repository
@Primary
public class CachedUserRepositoryAdapter
        extends CachedRepositoryDecorator<User, UserId>
        implements UserRepository {

    @Override
    public Optional<User> findByTenantIdAndId(TenantId tenantId, UserId id) {
        return findWithCache(
            tenantId,
            id.getValue(),
            entityId -> baseRepository.findByTenantIdAndId(tenantId, id)
        );
    }
}
```

**What happens:**

1. Check cache for key: `tenant:{tenantId}:users:{userId}`
2. If hit, return cached user
3. If miss, load from database
4. Populate cache with TTL
5. Return user

#### Pattern 2: Write-Through Cache Update

```java
@Override
public void save(User user) {
    // 1. Save to database (source of truth)
    baseRepository.save(user);

    // 2. Update cache (write-through)
    String cacheKey = CacheKeyGenerator.forEntity(
        user.getTenantId(),
        CacheNamespace.USERS.getValue(),
        user.getId().getValue()
    );
    redisTemplate.opsForValue().set(cacheKey, user, Duration.ofMinutes(15));
}
```

**What happens:**

1. Save to database first
2. Update cache immediately
3. Domain event published triggers collection cache invalidation

#### Pattern 3: Event-Driven Cache Invalidation

```java
@Component
public class UserCacheInvalidationListener extends CacheInvalidationEventListener {

    @KafkaListener(topics = "user-events", groupId = "user-service-cache-invalidation")
    public void handleUserEvent(Object event) {
        if (event instanceof UserUpdatedEvent updated) {
            invalidateForEvent(updated, CacheNamespace.USERS.getValue());
        }
    }
}
```

**What happens:**

1. Domain event published to Kafka
2. Listener consumes event
3. Invalidates entity cache: `tenant:{tenantId}:users:{userId}`
4. Invalidates collection caches: `tenant:{tenantId}:users:*`

---

## Best Practices

### ✅ DO

1. **Cache Single Entity Lookups**
    - Cache `findByTenantIdAndId()` methods
    - High read frequency, low write frequency

2. **Use Write-Through for Writes**
    - Update cache immediately after database save
    - Ensures strong consistency for write model

3. **Invalidate Collections on Updates**
    - Use event-driven invalidation
    - Invalidate entire namespace for collection queries

4. **Configure Appropriate TTL**
    - Reference data: 60+ minutes
    - Frequently changing: 5-15 minutes
    - Real-time data: 5 minutes or less

5. **Monitor Cache Metrics**
    - Track hit ratio (target >80%)
    - Monitor eviction rates
    - Alert on connection failures

### ❌ DON'T

1. **Don't Cache Collections**
    - Large collections cause cache bloat
    - Use pagination instead

2. **Don't Cache Cross-Tenant Queries**
    - Admin operations shouldn't be cached
    - Cache only tenant-scoped queries

3. **Don't Skip Invalidation**
    - Every update event must invalidate caches
    - Stale data breaks user trust

4. **Don't Cache Transient Data**
    - Session data, cart contents
    - Use session storage instead

5. **Don't Ignore Cache Failures**
    - Always fall back to database
    - Log warnings for monitoring

---

## Troubleshooting

### Issue: Low Cache Hit Ratio (<50%)

**Symptoms:**

- High database load
- Slow response times
- Cache metrics show low hit ratio

**Solutions:**

1. Review TTL configuration (may be too short)
2. Check cache key generation (keys may not match)
3. Audit invalidation logic (too aggressive)
4. Implement cache warming for frequently accessed data

### Issue: Stale Data in Cache

**Symptoms:**

- Users see outdated information
- Updates not reflected immediately

**Solutions:**

1. Check Kafka consumer health (consumer lag)
2. Verify invalidation listener is consuming events
3. Review write-through logic in cached adapter
4. Check event publishing is working

### Issue: Redis Connection Failures

**Symptoms:**

- Cache operations failing
- Health check DOWN
- Errors in logs: `RedisConnectionException`

**Solutions:**

1. Check Redis server status: `redis-cli PING`
2. Verify network connectivity
3. Check credentials in `application.yml`
4. Review firewall rules

### Issue: Multi-Tenant Cache Leakage

**Symptoms:**

- Users see data from other tenants
- Security violation

**Solutions:**

1. Verify tenant prefix in all cache keys
2. Check `TenantContext` is set correctly
3. Review cache key generation logic
4. Run security tests

---

## Configuration Examples

### Basic Configuration

```yaml
wms:
  cache:
    enabled: true
    default-ttl-minutes: 30
    redis:
      host: localhost
      port: 6379
      password: ""
    cache-configs:
      users:
        ttl-minutes: 15
```

### Production Configuration

```yaml
wms:
  cache:
    enabled: true
    default-ttl-minutes: 30
    redis:
      host: ${REDIS_HOST:redis-cluster.internal}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}
    cache-configs:
      users:
        ttl-minutes: 15
      products:
        ttl-minutes: 60
      stock-levels:
        ttl-minutes: 5
```

---

## Testing

### Unit Test Example

```java
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
    verifyNoInteractions(baseRepository);
}
```

### Integration Test Example

```java
@SpringBootTest
@Testcontainers
class CacheIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("wms.cache.redis.host", redis::getHost);
        registry.add("wms.cache.redis.port", redis::getFirstMappedPort);
    }

    @Test
    void cacheFlow_EndToEnd() {
        // Test cache hit/miss flow
    }
}
```

---

## Monitoring

### Key Metrics

- `cache.hits` - Cache hit count
- `cache.misses` - Cache miss count
- `cache.hit.ratio` - Hit ratio (hits / (hits + misses))
- `cache.evictions` - Cache eviction count
- `redis.connection.failures` - Connection failure count

### Health Checks

- `/actuator/health/redis` - Redis connectivity
- `/actuator/metrics/cache.*` - Cache metrics

### Alert Thresholds

- Cache hit ratio < 70% (warning)
- Cache hit ratio < 50% (critical)
- Redis connection failures > 5/min (critical)

---

## References

- [Service Implementation Checklist](./SERVICE_IMPLEMENTATION_CHECKLIST.md)
- [Production Caching Strategy](./01-production-caching-strategy.md)
- [Repository Adapter Decorator Pattern](./04-repository-adapter-decorator-pattern.md)
- [Cache Invalidation Strategy](./03-cache-invalidation-strategy.md)

---

**End of Developer Guide**
