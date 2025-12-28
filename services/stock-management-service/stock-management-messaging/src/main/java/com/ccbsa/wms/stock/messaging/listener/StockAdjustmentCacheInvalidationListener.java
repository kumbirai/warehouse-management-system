package com.ccbsa.wms.stock.messaging.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ccbsa.common.cache.invalidation.CacheInvalidationEventListener;
import com.ccbsa.common.cache.invalidation.LocalCacheInvalidator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.wms.stock.domain.core.event.StockAdjustedEvent;

/**
 * Stock Management Service Cache Invalidation Listener for Stock Adjustments.
 * <p>
 * MANDATORY: Listens to stock-management domain events and invalidates affected caches.
 * <p>
 * Invalidation Strategy:
 * - StockAdjustedEvent: Invalidate entity + collections (also invalidate stock items as adjustment affects stock levels)
 */
@Component
public class StockAdjustmentCacheInvalidationListener extends CacheInvalidationEventListener {

    public StockAdjustmentCacheInvalidationListener(LocalCacheInvalidator cacheInvalidator) {
        super(cacheInvalidator);
    }

    @KafkaListener(topics = "stock-management-events", groupId = "stock-management-service-cache-invalidation-adjustments", containerFactory =
            "externalEventKafkaListenerContainerFactory")
    public void handleStockAdjustmentEvent(Object event) {
        if (event instanceof StockAdjustedEvent adjusted) {
            // Invalidate adjustment entity cache and all collection caches
            invalidateForEvent(adjusted, CacheNamespace.STOCK_ADJUSTMENTS.getValue());
            // Also invalidate stock items as adjustment affects stock levels
            invalidateForEvent(adjusted, CacheNamespace.STOCK_ITEMS.getValue());
        }
    }
}

