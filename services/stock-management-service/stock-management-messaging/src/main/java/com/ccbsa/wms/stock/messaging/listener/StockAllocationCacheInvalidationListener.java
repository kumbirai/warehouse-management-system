package com.ccbsa.wms.stock.messaging.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ccbsa.common.cache.invalidation.CacheInvalidationEventListener;
import com.ccbsa.common.cache.invalidation.LocalCacheInvalidator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.wms.stock.domain.core.event.StockAllocatedEvent;
import com.ccbsa.wms.stock.domain.core.event.StockAllocationReleasedEvent;

/**
 * Stock Management Service Cache Invalidation Listener for Stock Allocations.
 * <p>
 * MANDATORY: Listens to stock-management domain events and invalidates affected caches.
 * <p>
 * Invalidation Strategy:
 * - StockAllocatedEvent: Invalidate entity + collections
 * - StockAllocationReleasedEvent: Invalidate entity + collections
 */
@Component
public class StockAllocationCacheInvalidationListener extends CacheInvalidationEventListener {

    public StockAllocationCacheInvalidationListener(LocalCacheInvalidator cacheInvalidator) {
        super(cacheInvalidator);
    }

    @KafkaListener(topics = "stock-management-events", groupId = "stock-management-service-cache-invalidation-allocations", containerFactory =
            "externalEventKafkaListenerContainerFactory")
    public void handleStockAllocationEvent(Object event) {
        if (event instanceof StockAllocatedEvent allocated) {
            // Invalidate entity cache and all collection caches
            invalidateForEvent(allocated, CacheNamespace.STOCK_ALLOCATIONS.getValue());
        } else if (event instanceof StockAllocationReleasedEvent released) {
            // Invalidate entity cache and all collection caches
            invalidateForEvent(released, CacheNamespace.STOCK_ALLOCATIONS.getValue());
        }
    }
}

