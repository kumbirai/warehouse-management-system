package com.ccbsa.wms.notification.messaging.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ccbsa.common.cache.invalidation.CacheInvalidationEventListener;
import com.ccbsa.common.cache.invalidation.LocalCacheInvalidator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.wms.notification.domain.core.event.NotificationCreatedEvent;
import com.ccbsa.wms.notification.domain.core.event.NotificationSentEvent;

/**
 * Notification Service Cache Invalidation Listener.
 * <p>
 * MANDATORY: Listens to notification domain events and invalidates affected caches.
 * <p>
 * Invalidation Strategy:
 * - NotificationCreatedEvent: No invalidation (cache-aside pattern)
 * - NotificationSentEvent: Invalidate entity + collections
 */
@Component
public class NotificationCacheInvalidationListener extends CacheInvalidationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationCacheInvalidationListener.class);

    public NotificationCacheInvalidationListener(LocalCacheInvalidator cacheInvalidator) {
        super(cacheInvalidator);
    }

    @KafkaListener(topics = "notification-events", groupId = "notification-service-cache-invalidation", containerFactory = "externalEventKafkaListenerContainerFactory")
    public void handleNotificationEvent(Object event) {
        if (event instanceof NotificationCreatedEvent) {
            // No invalidation - cache-aside pattern
            log.debug("Notification created, no cache invalidation needed");
        } else if (event instanceof NotificationSentEvent sent) {
            // Invalidate entity cache and all collection caches
            invalidateForEvent(sent, CacheNamespace.NOTIFICATIONS.getValue());
        }
    }
}

