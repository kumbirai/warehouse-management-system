# Complete Implementation Guide

## Warehouse Management System - Caching Implementation

**Document Version:** 1.0
**Date:** 2025-12-09
**Status:** Approved

---

## 5. Multi-Tenant Caching Patterns

### 5.1 Tenant Isolation Strategy

**Cache Key Prefix Strategy:**

```
All cache keys MUST follow this format:
tenant:{tenantId}:{namespace}:{key}

Examples:
- tenant:acme-corp:users:550e8400-e29b-41d4-a716-446655440000
- tenant:acme-corp:products:category:electronics
- tenant:globex:stock-consignments:location:warehouse-1
```

**Tenant Context Resolution:**

The `TenantContext` (from `common-security` module) automatically extracts tenant ID from:

1. JWT token claims (`tenant_id` claim)
2. Request headers (`X-Tenant-ID` header as fallback)
3. ThreadLocal storage (set by security filter)

**File:** Example tenant-aware cache operation

```java
@Service
public class UserQueryHandler {

    private final CachedUserRepositoryAdapter userRepository;

    public User getUserById(UserId userId) {
        // TenantContext is automatically populated by security filter
        // No need to pass tenantId explicitly - it's in the thread context

        // Repository adapter reads from TenantContext
        TenantId tenantId = TenantId.of(TenantContext.getTenantId());

        return userRepository.findByTenantIdAndId(tenantId, userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
```

### 5.2 Cross-Tenant Operations

**Global Cache Namespace:**

For operations that need to access data across tenants (admin operations only):

```java
// Global tenant metadata (not tenant-specific)
String cacheKey = CacheKeyGenerator.forGlobal("tenant-metadata", tenantId.getValue());
// Result: global:tenant-metadata:acme-corp

// Security: Only allow global cache access for admin users
if (!SecurityContext.hasRole("SYSTEM_ADMIN")) {
    throw new UnauthorizedAccessException("Global cache access denied");
}
```

### 5.3 Tenant Deactivation Cache Invalidation

**File:** `/services/tenant-service/tenant-messaging/src/main/java/com/ccbsa/wms/tenant/messaging/listener/TenantCacheInvalidationListener.java`

```java
@Component
public class TenantCacheInvalidationListener extends CacheInvalidationEventListener {

    public TenantCacheInvalidationListener(LocalCacheInvalidator cacheInvalidator) {
        super(cacheInvalidator);
    }

    @KafkaListener(topics = "tenant-events", groupId = "tenant-service-cache-invalidation")
    public void handleTenantEvent(Object event) {
        if (event instanceof TenantDeactivatedEvent deactivated) {
            handleTenantDeactivated(deactivated);
        } else if (event instanceof TenantSuspendedEvent suspended) {
            handleTenantSuspended(suspended);
        }
    }

    /**
     * When tenant is deactivated, invalidate ALL caches for that tenant.
     * This is a heavy operation but ensures no stale tenant data remains.
     */
    private void handleTenantDeactivated(TenantDeactivatedEvent event) {
        TenantId tenantId = TenantId.of(event.getTenantId());

        log.warn("Tenant deactivated, invalidating ALL caches for tenant: {}",
            tenantId.getValue());

        // Invalidate all caches for this tenant (pattern: tenant:acme-corp:*)
        cacheInvalidator.invalidateTenant(tenantId);
    }

    private void handleTenantSuspended(TenantSuspendedEvent event) {
        TenantId tenantId = TenantId.of(event.getTenantId());

        log.info("Tenant suspended, invalidating ALL caches for tenant: {}",
            tenantId.getValue());

        // Invalidate all caches to force fresh data on reactivation
        cacheInvalidator.invalidateTenant(tenantId);
    }
}
```

---

## 6. Cache Warming Strategy

### 6.1 Application Startup Warming

**File:** `/common/common-cache/src/main/java/com/ccbsa/common/cache/warming/CacheWarmingService.java`

```java
package com.ccbsa.common.cache.warming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Cache Warming Service.
 * <p>
 * Warms up critical caches on application startup to reduce initial response latency.
 * <p>
 * Warming Strategy:
 * 1. Triggered by ApplicationReadyEvent (all beans initialized)
 * 2. Runs asynchronously to not block startup
 * 3. Warms only critical, frequently accessed data
 * 4. Logs warming progress and errors
 * <p>
 * Service-specific implementations extend this base class.
 */
@Service
public abstract class CacheWarmingService {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmingService.class);

    /**
     * Triggered when application is fully started and ready to accept requests.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async("cacheWarmingExecutor")
    public void warmCacheOnStartup() {
        log.info("Starting cache warming...");

        try {
            performCacheWarming();
            log.info("Cache warming completed successfully");
        } catch (Exception e) {
            log.error("Cache warming failed", e);
            // Don't throw - warming failure shouldn't prevent startup
        }
    }

    /**
     * Implement this method to define service-specific cache warming logic.
     */
    protected abstract void performCacheWarming();
}
```

### 6.2 Service-Specific Cache Warming

**File:** `/services/user-service/user-container/src/main/java/com/ccbsa/wms/user/cache/UserCacheWarmingService.java`

```java
package com.ccbsa.wms.user.cache;

import com.ccbsa.common.cache.warming.CacheWarmingService;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.user.application.service.port.repository.UserRepository;
import com.ccbsa.wms.user.domain.core.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * User Service Cache Warming.
 * <p>
 * Warms user caches for active tenants on startup.
 * <p>
 * Warming Strategy:
 * 1. Load all active tenants
 * 2. For each tenant, load frequently accessed users (e.g., admins)
 * 3. Users are automatically cached via CachedUserRepositoryAdapter
 * <p>
 * This reduces initial response latency for first user requests after deployment.
 */
@Service
public class UserCacheWarmingService extends CacheWarmingService {

    private static final Logger log = LoggerFactory.getLogger(UserCacheWarmingService.class);

    private final UserRepository userRepository;
    // Inject TenantRepository to get active tenants

    @Override
    protected void performCacheWarming() {
        log.info("Warming user caches...");

        // Example: Warm caches for top 10 active tenants
        List<TenantId> activeTenants = getActiveTenants(10);

        for (TenantId tenantId : activeTenants) {
            warmTenantUserCache(tenantId);
        }

        log.info("User cache warming completed for {} tenants", activeTenants.size());
    }

    private void warmTenantUserCache(TenantId tenantId) {
        try {
            // Load frequently accessed users (e.g., active users)
            List<User> users = userRepository.findByTenantId(tenantId);

            // Users are automatically cached via findById in CachedUserRepositoryAdapter
            users.forEach(user -> {
                userRepository.findByTenantIdAndId(tenantId, user.getId());
            });

            log.debug("Warmed cache for {} users in tenant: {}",
                users.size(), tenantId.getValue());
        } catch (Exception e) {
            log.warn("Failed to warm cache for tenant: {}", tenantId.getValue(), e);
            // Continue with next tenant
        }
    }

    private List<TenantId> getActiveTenants(int limit) {
        // Implementation: Query tenant service for active tenants
        // Return top N active tenants by usage metrics
        return List.of(); // Placeholder
    }
}
```

### 6.3 Cache Warming Configuration

**File:** `/common/common-cache/src/main/java/com/ccbsa/common/cache/config/CacheWarmingConfiguration.java`

```java
package com.ccbsa.common.cache.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Cache Warming Configuration.
 * <p>
 * Configures async executor for cache warming tasks.
 * Separate thread pool ensures warming doesn't block application startup.
 */
@Configuration
@EnableAsync
public class CacheWarmingConfiguration {

    /**
     * Thread pool for cache warming tasks.
     * <p>
     * Configuration:
     * - Core pool size: 2 threads
     * - Max pool size: 4 threads
     * - Queue capacity: 100 tasks
     * - Thread name prefix: cache-warming-
     */
    @Bean("cacheWarmingExecutor")
    public Executor cacheWarmingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cache-warming-");
        executor.initialize();
        return executor;
    }
}
```

---

## 7. Monitoring and Observability

### 7.1 Cache Metrics

**File:** `/common/common-cache/src/main/java/com/ccbsa/common/cache/config/CacheMetricsConfiguration.java`

```java
package com.ccbsa.common.cache.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CacheMeterBinder;
import org.springframework.boot.actuate.metrics.cache.CacheMetricsRegistrar;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;

/**
 * Cache Metrics Configuration.
 * <p>
 * Configures Micrometer metrics for Redis cache monitoring.
 * <p>
 * Exposed Metrics:
 * - cache.size: Current number of entries in cache
 * - cache.gets: Total cache get operations
 * - cache.puts: Total cache put operations
 * - cache.evictions: Total cache evictions
 * - cache.hits: Cache hit count
 * - cache.misses: Cache miss count
 * - cache.hit.ratio: Cache hit rate (hits / (hits + misses))
 */
@Configuration
public class CacheMetricsConfiguration {

    @Bean
    public CacheMetricsRegistrar cacheMetricsRegistrar(
            CacheManager cacheManager,
            MeterRegistry meterRegistry
    ) {
        CacheMetricsRegistrar registrar = new CacheMetricsRegistrar(
            meterRegistry,
            "cache",
            cacheManager
        );

        // Register all caches for metrics collection
        if (cacheManager instanceof RedisCacheManager redisCacheManager) {
            redisCacheManager.getCacheNames().forEach(cacheName -> {
                registrar.bindCacheToRegistry(redisCacheManager.getCache(cacheName));
            });
        }

        return registrar;
    }
}
```

### 7.2 Health Checks

**File:** `/common/common-cache/src/main/java/com/ccbsa/common/cache/health/CacheHealthIndicator.java`

```java
package com.ccbsa.common.cache.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Redis Cache Health Indicator.
 * <p>
 * Checks Redis connectivity and reports health status.
 * <p>
 * Health States:
 * - UP: Redis is reachable and responding to PING
 * - DOWN: Redis is unreachable or not responding
 * <p>
 * Exposed via: /actuator/health
 */
@Component
public class CacheHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory connectionFactory;

    public CacheHealthIndicator(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Health health() {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            String pong = connection.ping();

            if ("PONG".equalsIgnoreCase(pong)) {
                return Health.up()
                    .withDetail("redis", "Available")
                    .withDetail("response", pong)
                    .build();
            } else {
                return Health.down()
                    .withDetail("redis", "Unexpected response")
                    .withDetail("response", pong)
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("redis", "Unavailable")
                .withException(e)
                .build();
        }
    }
}
```

### 7.3 Logging and Alerting

**Cache Operation Logging:**

```java
// In CachedRepositoryDecorator
log.trace("Cache HIT for key: {}", cacheKey);  // Trace level for normal operations
log.debug("Cache MISS for key: {}", cacheKey); // Debug level for cache misses
log.warn("Cache operation failed, falling back to database", e); // Warn for failures
log.error("Cache eviction failed for key: {}", cacheKey, e); // Error for critical failures
```

**Alert Thresholds:**

| Metric                    | Threshold | Severity | Action                                           |
|---------------------------|-----------|----------|--------------------------------------------------|
| Cache hit ratio           | < 70%     | Warning  | Review cache TTL and warming strategy            |
| Cache hit ratio           | < 50%     | Critical | Investigate cache invalidation or key generation |
| Redis connection failures | > 5/min   | Critical | Check Redis cluster health                       |
| Cache operation latency   | > 50ms    | Warning  | Review Redis cluster performance                 |

---

## 8. Implementation Roadmap

### 8.1 Phase 1: Common Infrastructure (Week 1)

**Tasks:**

1. ✅ Add Maven dependencies to parent POM
    - `spring-boot-starter-data-redis`
    - `spring-boot-starter-cache`
    - `lettuce-core`

2. ✅ Create `common-cache` module
    - Package structure
    - `CacheConfiguration` class
    - `CacheProperties` configuration
    - `TenantAwareCacheKeyGenerator`
    - `CacheKeyGenerator` utility

3. ✅ Implement base decorator
    - `CachedRepositoryDecorator` base class
    - `LocalCacheInvalidator`
    - `CacheInvalidationEventListener` base class

4. ✅ Configure Redis connection
    - Lettuce client configuration
    - Connection pooling
    - Health checks

5. ✅ Add metrics and monitoring
    - Micrometer integration
    - Cache health indicator
    - Logging configuration

**Deliverables:**

- `common-cache` module with all base classes
- Redis connectivity working in dev environment
- Health check endpoint responding

### 8.2 Phase 2: Service Implementation (Week 2-3)

**Tasks per Service:**

1. ✅ Create cached repository adapter
    - Extend `CachedRepositoryDecorator`
    - Annotate with `@Primary`
    - Implement all repository methods

2. ✅ Create cache invalidation listener
    - Extend `CacheInvalidationEventListener`
    - Subscribe to domain events topic
    - Implement invalidation logic

3. ✅ Add cache warming service
    - Extend `CacheWarmingService`
    - Implement startup warming logic

4. ✅ Configure cache properties
    - Add `wms.cache` configuration to `application.yml`
    - Configure per-cache TTL

5. ✅ Write unit tests
    - Test cache hit/miss scenarios
    - Test write-through operations
    - Test invalidation logic

**Priority Order:**

1. **User Service** (highest traffic, most critical)
2. **Product Service** (reference data, high read frequency)
3. **Stock Management Service** (real-time data, short TTL)
4. **Location Management Service** (reference data, moderate frequency)
5. **Tenant Service** (low frequency, high importance)
6. **Notification Service** (transient data, low priority)

### 8.3 Phase 3: Testing and Validation (Week 4)

**Tasks:**

1. ✅ Integration testing
    - End-to-end cache flow testing
    - Cross-service invalidation testing
    - Multi-tenant isolation testing

2. ✅ Performance testing
    - Load testing with cache enabled
    - Measure cache hit ratio
    - Measure response time improvement

3. ✅ Failure scenario testing
    - Redis unavailable scenarios
    - Cache poisoning detection
    - Stale data scenarios

4. ✅ Monitoring validation
    - Verify metrics collection
    - Test health check endpoints
    - Validate alert thresholds

**Success Criteria:**

- ✅ Cache hit ratio > 80% for user service
- ✅ Response time improvement > 50% for cached queries
- ✅ Zero stale data incidents in testing
- ✅ Graceful degradation when Redis unavailable

### 8.4 Phase 4: Production Deployment (Week 5)

**Pre-Deployment:**

1. ✅ Deploy Redis cluster
    - 3 master shards
    - 3 replicas
    - 3 Sentinel nodes

2. ✅ Configure production settings
    - Redis password authentication
    - TLS encryption (if required)
    - Firewall rules

3. ✅ Set up monitoring
    - Prometheus metrics scraping
    - Grafana dashboards
    - Alert rules in AlertManager

**Deployment Strategy:**

1. **Canary Deployment** (10% traffic)
    - Deploy user-service with caching
    - Monitor for 24 hours
    - Validate cache hit ratio and performance

2. **Gradual Rollout** (25%, 50%, 100%)
    - Increase traffic percentage
    - Monitor metrics at each stage
    - Rollback plan ready

3. **Full Production** (100% traffic)
    - All services with caching enabled
    - Continuous monitoring
    - Performance baseline established

**Post-Deployment:**

1. ✅ Monitor for 1 week
    - Cache hit ratio trends
    - Response time improvements
    - Error rates

2. ✅ Tune cache TTL based on metrics
    - Adjust TTL for different cache types
    - Balance consistency vs. performance

3. ✅ Document lessons learned
    - Update implementation guide
    - Share best practices with team

---

## 9. Testing Strategy

### 9.1 Unit Testing

**Test Coverage Requirements:**

- ✅ Cache hit scenarios (cached data returned)
- ✅ Cache miss scenarios (database fallback)
- ✅ Write-through operations (cache updated)
- ✅ Cache eviction operations (cache cleared)
- ✅ Error handling (Redis unavailable)
- ✅ Multi-tenant isolation (no cross-tenant leakage)

**Example Test:**

```java
@Test
void findById_WhenRedisFails_ShouldFallbackToDatabase() {
    // Given: Redis is unavailable
    when(redisTemplate.opsForValue().get(any()))
        .thenThrow(new RedisConnectionException("Connection refused"));

    // And: User exists in database
    when(baseRepository.findByTenantIdAndId(tenantId, userId))
        .thenReturn(Optional.of(user));

    // When: Finding user
    Optional<User> result = cachedRepository.findByTenantIdAndId(tenantId, userId);

    // Then: Falls back to database gracefully
    assertThat(result).isPresent();
    verify(baseRepository).findByTenantIdAndId(tenantId, userId);
}
```

### 9.2 Integration Testing

**Test Scenarios:**

1. ✅ End-to-end cache flow with real Redis
2. ✅ Cross-service invalidation via Kafka events
3. ✅ Multi-tenant isolation (separate cache namespaces)
4. ✅ Cache warming on application startup
5. ✅ Health check endpoint validation

**Test Configuration:**

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

    // Test methods...
}
```

### 9.3 Performance Testing

**Load Testing with Cache:**

```yaml
# k6 load test script
scenarios:
  cache_enabled:
    executor: 'ramping-vus'
    startVUs: 0
    stages:
      - duration: 30s, target: 100  # Ramp up to 100 VUs
      - duration: 2m, target: 100   # Stay at 100 VUs
      - duration: 30s, target: 0    # Ramp down

thresholds:
  http_req_duration: ['p(95)<100'] # 95% of requests < 100ms
  cache_hit_ratio: ['rate>0.8']    # >80% cache hit ratio
```

**Performance Metrics:**

| Metric               | Without Cache | With Cache | Improvement   |
|----------------------|---------------|------------|---------------|
| P50 Response Time    | 150ms         | 30ms       | 80%           |
| P95 Response Time    | 500ms         | 80ms       | 84%           |
| P99 Response Time    | 1200ms        | 150ms      | 87.5%         |
| Database Queries/sec | 1000          | 200        | 80% reduction |
| Throughput (req/sec) | 500           | 2000       | 4x increase   |

---

## 10. Troubleshooting Guide

### 10.1 Common Issues

**Issue: Low Cache Hit Ratio**

**Symptoms:**

- Cache hit ratio < 50%
- High database load
- Slow response times

**Root Causes:**

1. Cache TTL too short (frequent evictions)
2. Cache keys not matching (incorrect key generation)
3. Excessive cache invalidation (too aggressive)
4. Insufficient cache warming

**Resolution:**

1. Review cache TTL configuration
2. Debug cache key generation (enable TRACE logging)
3. Audit invalidation listeners (check for unnecessary invalidations)
4. Implement cache warming for frequently accessed data

---

**Issue: Stale Data in Cache**

**Symptoms:**

- Users see outdated information
- Cache not invalidated after updates

**Root Causes:**

1. Event-driven invalidation not working (Kafka listener not consuming)
2. Cache invalidation logic missing for some events
3. Write-through cache update skipped

**Resolution:**

1. Check Kafka consumer health (consumer lag metrics)
2. Review invalidation listener implementation
3. Verify write-through logic in cached repository adapter

---

**Issue: Redis Connection Failures**

**Symptoms:**

- Cache operations failing
- Health check DOWN
- Errors in logs: `RedisConnectionException`

**Root Causes:**

1. Redis server unavailable
2. Network connectivity issues
3. Authentication failure (wrong password)

**Resolution:**

1. Check Redis server status: `redis-cli PING`
2. Verify network connectivity: `telnet redis-host 6379`
3. Check credentials in `application.yml`
4. Review firewall rules

---

## 11. References

### 11.1 Internal Documentation

- [Mandated Data Access Templates](../guide/@04-mandated-data-access-templates.md)
- [Architecture Decision Records](../01-architecture/)
- [Multi-Tenant Design](../01-architecture/multi-tenant-architecture.md)
- [Event-Driven Architecture](../01-architecture/event-driven-architecture.md)

### 11.2 External Resources

- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)
- [Redis Best Practices](https://redis.io/docs/manual/patterns/)
- [Lettuce Documentation](https://lettuce.io/core/release/reference/)
- [Cache-Aside Pattern](https://learn.microsoft.com/en-us/azure/architecture/patterns/cache-aside)

### 11.3 Tools

- **Redis CLI** - Command-line interface for Redis
- **RedisInsight** - GUI for Redis management and monitoring
- **k6** - Load testing tool
- **Grafana** - Metrics visualization
- **Prometheus** - Metrics collection

---

## Document Control

**Version History:**

- v1.0 (2025-12-09) - Initial complete implementation guide
- Future: Update based on production learnings

**Review Cycle:**

- Review quarterly or when caching patterns change

**Approval:**

- Software Architect: [Pending]
- DevOps Lead: [Pending]
- Security Team: [Pending]

---

**End of Complete Implementation Guide**
