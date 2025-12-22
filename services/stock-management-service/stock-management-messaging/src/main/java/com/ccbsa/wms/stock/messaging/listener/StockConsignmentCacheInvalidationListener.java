package com.ccbsa.wms.stock.messaging.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ccbsa.common.cache.invalidation.CacheInvalidationEventListener;
import com.ccbsa.common.cache.invalidation.LocalCacheInvalidator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.wms.stock.domain.core.event.StockConsignmentConfirmedEvent;
import com.ccbsa.wms.stock.domain.core.event.StockConsignmentReceivedEvent;

/**
 * Stock Management Service Cache Invalidation Listener for Stock Consignments.
 * <p>
 * MANDATORY: Listens to stock-management domain events and invalidates affected caches.
 * <p>
 * Invalidation Strategy:
 * - StockConsignmentReceivedEvent: No invalidation (cache-aside pattern)
 * - StockConsignmentConfirmedEvent: Invalidate entity + collections
 */
@Component
public class StockConsignmentCacheInvalidationListener extends CacheInvalidationEventListener {

    private static final Logger log = LoggerFactory.getLogger(StockConsignmentCacheInvalidationListener.class);

    public StockConsignmentCacheInvalidationListener(LocalCacheInvalidator cacheInvalidator) {
        super(cacheInvalidator);
    }

    @KafkaListener(topics = "stock-management-events", groupId = "stock-management-service-cache-invalidation-consignments", containerFactory =
            "externalEventKafkaListenerContainerFactory")
    public void handleStockConsignmentEvent(Object event) {
        if (event instanceof StockConsignmentReceivedEvent) {
            // No invalidation - cache-aside pattern
            log.debug("Stock consignment received, no cache invalidation needed");
        } else if (event instanceof StockConsignmentConfirmedEvent confirmed) {
            // Invalidate entity cache and all collection caches
            invalidateForEvent(confirmed, CacheNamespace.STOCK_CONSIGNMENTS.getValue());
        }
    }
}

