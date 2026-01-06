package com.ccbsa.wms.stock.messaging.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ccbsa.common.cache.invalidation.CacheInvalidationEventListener;
import com.ccbsa.common.cache.invalidation.LocalCacheInvalidator;
import com.ccbsa.wms.stock.domain.core.event.StockLevelAboveMaximumEvent;
import com.ccbsa.wms.stock.domain.core.event.StockLevelBelowMinimumEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Stock Management Service Cache Invalidation Listener for Stock Level Thresholds.
 * <p>
 * MANDATORY: Listens to stock-management domain events and invalidates affected caches.
 * <p>
 * Invalidation Strategy:
 * - StockLevelBelowMinimumEvent: No invalidation (read-only alert event)
 * - StockLevelAboveMaximumEvent: No invalidation (read-only alert event)
 * <p>
 * Note: Threshold events are alert events and don't modify the threshold aggregate itself,
 * so no cache invalidation is needed. Threshold updates would be handled through direct
 * command operations.
 */
@Slf4j
@Component
public class StockLevelThresholdCacheInvalidationListener extends CacheInvalidationEventListener {
    public StockLevelThresholdCacheInvalidationListener(LocalCacheInvalidator cacheInvalidator) {
        super(cacheInvalidator);
    }

    @KafkaListener(topics = "stock-management-events", groupId = "stock-management-service-cache-invalidation-thresholds", containerFactory =
            "externalEventKafkaListenerContainerFactory")
    public void handleStockLevelThresholdEvent(Object event) {
        if (event instanceof StockLevelBelowMinimumEvent) {
            // No invalidation - read-only alert event
            log.debug("Stock level below minimum alert, no cache invalidation needed");
        } else if (event instanceof StockLevelAboveMaximumEvent) {
            // No invalidation - read-only alert event
            log.debug("Stock level above maximum alert, no cache invalidation needed");
        }
    }
}

