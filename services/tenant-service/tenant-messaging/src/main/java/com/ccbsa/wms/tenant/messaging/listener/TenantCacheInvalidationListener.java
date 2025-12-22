package com.ccbsa.wms.tenant.messaging.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ccbsa.common.cache.invalidation.CacheInvalidationEventListener;
import com.ccbsa.common.cache.invalidation.LocalCacheInvalidator;
import com.ccbsa.common.cache.key.CacheKeyGenerator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.tenant.domain.core.event.TenantActivatedEvent;
import com.ccbsa.wms.tenant.domain.core.event.TenantConfigurationUpdatedEvent;
import com.ccbsa.wms.tenant.domain.core.event.TenantCreatedEvent;
import com.ccbsa.wms.tenant.domain.core.event.TenantDeactivatedEvent;
import com.ccbsa.wms.tenant.domain.core.event.TenantSuspendedEvent;

/**
 * Tenant Service Cache Invalidation Listener.
 * <p>
 * MANDATORY: Listens to tenant domain events and invalidates affected caches.
 * <p>
 * Note: Tenants use global cache keys (not tenant-aware), so invalidation uses global keys.
 * <p>
 * Invalidation Strategy:
 * - TenantCreatedEvent: No invalidation (cache-aside pattern)
 * - TenantActivatedEvent: Invalidate tenant entity
 * - TenantDeactivatedEvent: Invalidate tenant entity
 * - TenantSuspendedEvent: Invalidate tenant entity
 * - TenantConfigurationUpdatedEvent: Invalidate tenant entity
 */
@Component
public class TenantCacheInvalidationListener extends CacheInvalidationEventListener {

    private static final Logger log = LoggerFactory.getLogger(TenantCacheInvalidationListener.class);

    public TenantCacheInvalidationListener(LocalCacheInvalidator cacheInvalidator) {
        super(cacheInvalidator);
    }

    @KafkaListener(topics = "tenant-events", groupId = "tenant-service-cache-invalidation", containerFactory = "externalEventKafkaListenerContainerFactory")
    public void handleTenantEvent(Object event) {
        if (event instanceof TenantCreatedEvent) {
            // No invalidation - cache-aside pattern
            log.debug("Tenant created, no cache invalidation needed");
        } else if (event instanceof TenantActivatedEvent activated) {
            // Invalidate tenant entity using global cache key
            invalidateTenantEntity(TenantId.of(activated.getTenantId()));
        } else if (event instanceof TenantDeactivatedEvent deactivated) {
            // Invalidate tenant entity using global cache key
            invalidateTenantEntity(TenantId.of(deactivated.getTenantId()));
        } else if (event instanceof TenantSuspendedEvent suspended) {
            // Invalidate tenant entity using global cache key
            invalidateTenantEntity(TenantId.of(suspended.getTenantId()));
        } else if (event instanceof TenantConfigurationUpdatedEvent configUpdated) {
            // Invalidate tenant entity using global cache key
            invalidateTenantEntity(TenantId.of(configUpdated.getTenantId()));
        }
    }

    /**
     * Invalidates tenant entity cache using global cache key.
     * <p>
     * Tenants are not tenant-aware, so we use global cache keys.
     */
    private void invalidateTenantEntity(TenantId tenantId) {
        String cacheKey = CacheKeyGenerator.forGlobal(CacheNamespace.TENANTS.getValue(), tenantId.getValue());
        cacheInvalidator.invalidateByKey(cacheKey);
        log.debug("Tenant cache invalidated for key: {}", cacheKey);
    }
}

