package com.ccbsa.wms.location.messaging.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ccbsa.common.cache.invalidation.CacheInvalidationEventListener;
import com.ccbsa.common.cache.invalidation.LocalCacheInvalidator;
import com.ccbsa.common.cache.key.CacheNamespace;
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
    public LocationCacheInvalidationListener(LocalCacheInvalidator cacheInvalidator) {
        super(cacheInvalidator);
    }

    @KafkaListener(topics = "location-management-events", groupId = "location-management-service-cache-invalidation", containerFactory =
            "externalEventKafkaListenerContainerFactory")
    public void handleLocationEvent(Object event) {
        if (event instanceof LocationCreatedEvent) {
            // No invalidation - cache-aside pattern
            log.debug("Location created, no cache invalidation needed");
        } else if (event instanceof LocationStatusChangedEvent statusChanged) {
            // Invalidate entity cache and all collection caches
            invalidateForEvent(statusChanged, CacheNamespace.LOCATIONS.getValue());
        } else if (event instanceof LocationAssignedEvent assigned) {
            // Invalidate entity cache and all collection caches
            invalidateForEvent(assigned, CacheNamespace.LOCATIONS.getValue());
        } else if (event instanceof LocationBlockedEvent blocked) {
            // Invalidate entity cache and all collection caches
            invalidateForEvent(blocked, CacheNamespace.LOCATIONS.getValue());
        } else if (event instanceof LocationUnblockedEvent unblocked) {
            // Invalidate entity cache and all collection caches
            invalidateForEvent(unblocked, CacheNamespace.LOCATIONS.getValue());
        }
    }
}

