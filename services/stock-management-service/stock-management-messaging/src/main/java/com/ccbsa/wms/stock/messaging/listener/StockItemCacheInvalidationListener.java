package com.ccbsa.wms.stock.messaging.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ccbsa.common.cache.invalidation.CacheInvalidationEventListener;
import com.ccbsa.common.cache.invalidation.LocalCacheInvalidator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.wms.stock.domain.core.event.LocationAssignedEvent;
import com.ccbsa.wms.stock.domain.core.event.StockClassifiedEvent;
import com.ccbsa.wms.stock.domain.core.event.StockExpiredEvent;
import com.ccbsa.wms.stock.domain.core.event.StockExpiringAlertEvent;

/**
 * Stock Management Service Cache Invalidation Listener for Stock Items.
 * <p>
 * MANDATORY: Listens to stock-management domain events and invalidates affected caches.
 * <p>
 * Invalidation Strategy:
 * - StockClassifiedEvent: Invalidate entity + collections
 * - LocationAssignedEvent: Invalidate entity + collections
 * - StockExpiringAlertEvent: No invalidation (read-only alert)
 * - StockExpiredEvent: Invalidate entity + collections
 */
@Component
public class StockItemCacheInvalidationListener extends CacheInvalidationEventListener {

    private static final Logger log = LoggerFactory.getLogger(StockItemCacheInvalidationListener.class);

    public StockItemCacheInvalidationListener(LocalCacheInvalidator cacheInvalidator) {
        super(cacheInvalidator);
    }

    @KafkaListener(topics = "stock-management-events", groupId = "stock-management-service-cache-invalidation-items", containerFactory =
            "externalEventKafkaListenerContainerFactory")
    public void handleStockItemEvent(Object event) {
        if (event instanceof StockClassifiedEvent classified) {
            // Invalidate entity cache and all collection caches
            invalidateForEvent(classified, CacheNamespace.STOCK_ITEMS.getValue());
        } else if (event instanceof LocationAssignedEvent assigned) {
            // Invalidate entity cache and all collection caches
            invalidateForEvent(assigned, CacheNamespace.STOCK_ITEMS.getValue());
        } else if (event instanceof StockExpiringAlertEvent) {
            // No invalidation - read-only alert event
            log.debug("Stock expiring alert, no cache invalidation needed");
        } else if (event instanceof StockExpiredEvent expired) {
            // Invalidate entity cache and all collection caches
            invalidateForEvent(expired, CacheNamespace.STOCK_ITEMS.getValue());
        }
    }
}

