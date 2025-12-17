package com.ccbsa.wms.user.application.service.port.messaging;

import com.ccbsa.common.messaging.EventPublisher;
import com.ccbsa.wms.user.domain.core.event.UserEvent;

/**
 * Port interface for publishing User domain events.
 * <p>
 * Extends the common EventPublisher interface for type safety. Implemented by the messaging layer (user-messaging).
 */
public interface UserEventPublisher
        extends EventPublisher {
    /**
     * Publishes a user domain event.
     *
     * @param event User domain event to publish
     */
    void publish(UserEvent event);
}

