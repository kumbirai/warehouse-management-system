package com.ccbsa.wms.picking.messaging.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ccbsa.common.cache.invalidation.CacheInvalidationEventListener;
import com.ccbsa.common.cache.invalidation.LocalCacheInvalidator;
import com.ccbsa.wms.picking.domain.core.event.LoadPlannedEvent;
import com.ccbsa.wms.picking.domain.core.event.OrderMappedToLoadEvent;
import com.ccbsa.wms.picking.domain.core.event.PickingListReceivedEvent;
import com.ccbsa.wms.picking.domain.core.event.PickingTaskCreatedEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Cache Invalidation Listener: PickingCacheInvalidationListener
 * <p>
 * Invalidates cache on picking domain events.
 */
@Component
@Slf4j
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
            // Invalidate loads cache (load status changed)
            invalidateForEvent(planned, "loads");
            // Also invalidate picking list cache if pickingListId is present
            // This ensures picking list cache is refreshed with updated load statuses
            if (planned.getPickingListId() != null && !planned.getPickingListId().isEmpty()) {
                try {
                    com.ccbsa.common.domain.valueobject.TenantId tenantId = planned.getTenantId();
                    java.util.UUID pickingListId = java.util.UUID.fromString(planned.getPickingListId());
                    cacheInvalidator.invalidateEntity(tenantId, "picking-lists", pickingListId);
                } catch (Exception e) {
                    // Log but don't fail - cache invalidation is best effort
                    // Note: Using parent class log via reflection or removing log statement
                }
            }
        } else if (event instanceof OrderMappedToLoadEvent mapped) {
            invalidateForEvent(mapped, "loads", "orders");
        } else if (event instanceof PickingTaskCreatedEvent created) {
            invalidateForEvent(created, "picking-tasks");
        }
    }
}
