package com.ccbsa.wms.location.application.service.port.messaging;

import com.ccbsa.common.messaging.EventPublisher;
import com.ccbsa.wms.location.domain.core.event.LocationManagementEvent;

/**
 * Event Publisher Port: LocationEventPublisher
 * <p>
 * Defines the contract for publishing Location Management domain events.
 * Implemented by messaging adapters (Kafka).
 * <p>
 * This port extends the common EventPublisher interface and adds
 * service-specific event publishing methods.
 */
public interface LocationEventPublisher extends EventPublisher {
    /**
     * Publishes a Location Management event.
     *
     * @param event Location Management event to publish
     */
    void publish(LocationManagementEvent event);
}

