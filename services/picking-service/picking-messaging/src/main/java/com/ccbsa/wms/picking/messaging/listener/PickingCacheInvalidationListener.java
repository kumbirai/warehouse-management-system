package com.ccbsa.wms.picking.messaging.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ccbsa.common.cache.invalidation.CacheInvalidationEventListener;
import com.ccbsa.common.cache.invalidation.LocalCacheInvalidator;
import com.ccbsa.wms.picking.domain.core.event.LoadPlannedEvent;
import com.ccbsa.wms.picking.domain.core.event.OrderMappedToLoadEvent;
import com.ccbsa.wms.picking.domain.core.event.PickingListReceivedEvent;
import com.ccbsa.wms.picking.domain.core.event.PickingTaskCreatedEvent;

/**
 * Cache Invalidation Listener: PickingCacheInvalidationListener
 * <p>
 * Invalidates cache on picking domain events.
 */
@Component
public class PickingCacheInvalidationListener extends CacheInvalidationEventListener {

    public PickingCacheInvalidationListener(LocalCacheInvalidator cacheInvalidator) {
        super(cacheInvalidator);
    }

    @KafkaListener(topics = "picking-events", groupId = "picking-cache-invalidation", containerFactory = "kafkaListenerContainerFactory")
    public void handlePickingEvent(Object event) {
        if (event instanceof PickingListReceivedEvent) {
            // No invalidation - cache-aside pattern
            // Cache will be populated on next read
        } else if (event instanceof LoadPlannedEvent planned) {
            invalidateForEvent(planned, "loads");
        } else if (event instanceof OrderMappedToLoadEvent mapped) {
            invalidateForEvent(mapped, "loads", "orders");
        } else if (event instanceof PickingTaskCreatedEvent created) {
            invalidateForEvent(created, "picking-tasks");
        }
    }
}
