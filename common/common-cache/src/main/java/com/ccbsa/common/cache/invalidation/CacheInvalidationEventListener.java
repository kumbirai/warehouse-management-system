package com.ccbsa.common.cache.invalidation;

import java.lang.reflect.Method;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.security.TenantContext;

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
     * Invalidates multiple namespaces for a single event.
     * <p>
     * Use for cascade invalidation (e.g., product update affects stock caches).
     */
    protected void invalidateForEvent(DomainEvent<?> event, String... namespaces) {
        for (String namespace : namespaces) {
            invalidateForEvent(event, namespace);
        }
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
    protected void invalidateForEvent(DomainEvent<?> event, String namespace) {
        TenantId tenantId = extractTenantId(event);

        log.debug("Invalidating caches for event: {} in namespace: {}",
                event.getClass().getSimpleName(), namespace);

        // Strategy 1: Invalidate single entity if event contains aggregate ID
        if (event.getAggregateId() != null) {
            try {
                UUID aggregateId = UUID.fromString(event.getAggregateId());
                cacheInvalidator.invalidateEntity(tenantId, namespace, aggregateId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid aggregate ID format in event: {}", event.getAggregateId());
            }
        }

        // Strategy 2: Invalidate all collection caches for this namespace
        cacheInvalidator.invalidateCollection(tenantId, namespace);
    }

    /**
     * Extracts tenant ID from domain event.
     * <p>
     * Tries multiple strategies:
     * 1. Check for getTenantId() method (UserEvent, etc.)
     * 2. For TenantEvent, aggregateId IS the tenant ID
     * 3. Fallback to TenantContext if available
     * <p>
     * Override if your events use a different field name.
     */
    protected TenantId extractTenantId(DomainEvent<?> event) {
        // Strategy 1: Check for getTenantId() method (UserEvent, ProductEvent, etc.)
        try {
            Method getTenantIdMethod = event.getClass().getMethod("getTenantId");
            Object tenantIdObj = getTenantIdMethod.invoke(event);
            if (tenantIdObj instanceof TenantId tenantId) {
                return tenantId;
            } else if (tenantIdObj instanceof String tenantIdStr) {
                return TenantId.of(tenantIdStr);
            }
        } catch (NoSuchMethodException e) {
            // Method doesn't exist, try next strategy
        } catch (Exception e) {
            log.warn("Failed to extract tenant ID via getTenantId() method from event: {}",
                    event.getClass().getSimpleName(), e);
        }

        // Strategy 2: For TenantEvent, aggregateId IS the tenant ID
        if ("Tenant".equals(event.getAggregateType()) && event.getAggregateId() != null) {
            try {
                return TenantId.of(event.getAggregateId());
            } catch (Exception e) {
                log.warn("Failed to extract tenant ID from aggregateId: {}", event.getAggregateId(), e);
            }
        }

        // Strategy 3: Fallback - try to get from TenantContext (may not be available in async context)
        try {
            TenantId tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                return tenantId;
            }
        } catch (Exception e) {
            // TenantContext not available
        }

        log.error("Failed to extract tenant ID from event: {}. Event: {}",
                event.getClass().getSimpleName(), event);
        throw new IllegalArgumentException("Event must contain tenant ID",
                new IllegalStateException("Cannot determine tenant ID from event"));
    }
}
