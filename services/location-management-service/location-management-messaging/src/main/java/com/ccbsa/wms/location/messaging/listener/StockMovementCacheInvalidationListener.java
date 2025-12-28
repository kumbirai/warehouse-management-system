package com.ccbsa.wms.location.messaging.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ccbsa.common.cache.invalidation.CacheInvalidationEventListener;
import com.ccbsa.common.cache.invalidation.LocalCacheInvalidator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.wms.location.domain.core.event.StockMovementCancelledEvent;
import com.ccbsa.wms.location.domain.core.event.StockMovementCompletedEvent;
import com.ccbsa.wms.location.domain.core.event.StockMovementInitiatedEvent;

/**
 * Location Management Service Cache Invalidation Listener for Stock Movements.
 * <p>
 * MANDATORY: Listens to location-management domain events and invalidates affected caches.
 * <p>
 * Invalidation Strategy:
 * - StockMovementInitiatedEvent: Invalidate entity + collections
 * - StockMovementCompletedEvent: Invalidate entity + collections (also invalidate locations as movement affects location capacity)
 * - StockMovementCancelledEvent: Invalidate entity + collections
 */
@Component
public class StockMovementCacheInvalidationListener extends CacheInvalidationEventListener {

    public StockMovementCacheInvalidationListener(LocalCacheInvalidator cacheInvalidator) {
        super(cacheInvalidator);
    }

    @KafkaListener(topics = "location-management-events", groupId = "location-management-service-cache-invalidation-movements", containerFactory =
            "externalEventKafkaListenerContainerFactory")
    public void handleStockMovementEvent(Object event) {
        if (event instanceof StockMovementInitiatedEvent initiated) {
            // Invalidate entity cache and all collection caches
            invalidateForEvent(initiated, CacheNamespace.STOCK_MOVEMENTS.getValue());
        } else if (event instanceof StockMovementCompletedEvent completed) {
            // Invalidate movement entity cache and all collection caches
            invalidateForEvent(completed, CacheNamespace.STOCK_MOVEMENTS.getValue());
            // Also invalidate locations as movement affects location capacity
            invalidateForEvent(completed, CacheNamespace.LOCATIONS.getValue());
        } else if (event instanceof StockMovementCancelledEvent cancelled) {
            // Invalidate entity cache and all collection caches
            invalidateForEvent(cancelled, CacheNamespace.STOCK_MOVEMENTS.getValue());
        }
    }
}

