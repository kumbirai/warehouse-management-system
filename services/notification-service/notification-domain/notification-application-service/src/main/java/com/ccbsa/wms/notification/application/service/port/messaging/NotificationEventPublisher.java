package com.ccbsa.wms.notification.application.service.port.messaging;

import com.ccbsa.common.messaging.EventPublisher;
import com.ccbsa.wms.notification.domain.core.event.NotificationEvent;

/**
 * Event Publisher Port: NotificationEventPublisher
 * <p>
 * Extends common EventPublisher interface.
 * Service-specific event publishing contract.
 * Implemented by messaging adapters.
 */
public interface NotificationEventPublisher extends EventPublisher {

    /**
     * Publishes a Notification domain event.
     *
     * @param event Domain event to publish
     */
    void publish(NotificationEvent event);
}

