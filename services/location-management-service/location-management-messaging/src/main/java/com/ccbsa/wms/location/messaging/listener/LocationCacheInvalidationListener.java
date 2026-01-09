package com.ccbsa.wms.location.messaging.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ccbsa.common.cache.invalidation.CacheInvalidationEventListener;
import com.ccbsa.common.cache.invalidation.LocalCacheInvalidator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.wms.location.application.service.port.service.LocationHierarchyCacheInvalidationPort;
import com.ccbsa.wms.location.domain.core.event.LocationAssignedEvent;
import com.ccbsa.wms.location.domain.core.event.LocationBlockedEvent;
import com.ccbsa.wms.location.domain.core.event.LocationCreatedEvent;
import com.ccbsa.wms.location.domain.core.event.LocationStatusChangedEvent;
import com.ccbsa.wms.location.domain.core.event.LocationUnblockedEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Location Management Service Cache Invalidation Listener.
 * <p>
 * MANDATORY: Listens to location-management domain events and invalidates affected caches.
 * <p>
 * Invalidation Strategy:
 * - LocationCreatedEvent: No invalidation (cache-aside pattern)
 * - LocationStatusChangedEvent: Invalidate entity + collections
 * - LocationAssignedEvent: Invalidate entity + collections
 * - LocationBlockedEvent: Invalidate entity + collections
 * - LocationUnblockedEvent: Invalidate entity + collections
 */
@Slf4j
@Component
public class LocationCacheInvalidationListener extends CacheInvalidationEventListener {
    private final LocationHierarchyCacheInvalidationPort hierarchyCacheInvalidationPort;

    public LocationCacheInvalidationListener(LocalCacheInvalidator cacheInvalidator, LocationHierarchyCacheInvalidationPort hierarchyCacheInvalidationPort) {
        super(cacheInvalidator);
        this.hierarchyCacheInvalidationPort = hierarchyCacheInvalidationPort;
    }

    @KafkaListener(topics = "location-management-events", groupId = "location-management-service-cache-invalidation", containerFactory =
            "externalEventKafkaListenerContainerFactory")
    public void handleLocationEvent(Object event) {
        if (event instanceof LocationCreatedEvent created) {
            // Location created - evict hierarchy caches to reflect new location in hierarchy
            log.debug("Location created, evicting hierarchy caches");
            hierarchyCacheInvalidationPort.evictAll();
        } else if (event instanceof LocationStatusChangedEvent statusChanged) {
            // Invalidate entity cache and all collection caches
            invalidateForEvent(statusChanged, CacheNamespace.LOCATIONS.getValue());
            // Evict hierarchy caches as status changes affect hierarchy queries
            hierarchyCacheInvalidationPort.evictAll();
        } else if (event instanceof LocationAssignedEvent assigned) {
            // Invalidate entity cache and all collection caches
            invalidateForEvent(assigned, CacheNamespace.LOCATIONS.getValue());
            // Evict hierarchy caches as assignment affects hierarchy queries
            hierarchyCacheInvalidationPort.evictAll();
        } else if (event instanceof LocationBlockedEvent blocked) {
            // Invalidate entity cache and all collection caches
            invalidateForEvent(blocked, CacheNamespace.LOCATIONS.getValue());
            // Evict hierarchy caches as blocking affects hierarchy queries
            hierarchyCacheInvalidationPort.evictAll();
        } else if (event instanceof LocationUnblockedEvent unblocked) {
            // Invalidate entity cache and all collection caches
            invalidateForEvent(unblocked, CacheNamespace.LOCATIONS.getValue());
            // Evict hierarchy caches as unblocking affects hierarchy queries
            hierarchyCacheInvalidationPort.evictAll();
        }
    }
}

