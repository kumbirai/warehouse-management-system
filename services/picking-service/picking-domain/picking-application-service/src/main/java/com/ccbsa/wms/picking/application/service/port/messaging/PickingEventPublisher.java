package com.ccbsa.wms.picking.application.service.port.messaging;

import com.ccbsa.common.messaging.EventPublisher;
import com.ccbsa.wms.picking.domain.core.event.PickingEvent;

/**
 * Event Publisher Port: PickingEventPublisher
 * <p>
 * Defines the contract for publishing Picking domain events. Extends the base EventPublisher interface from common-messaging.
 * <p>
 * This port is defined in the application service layer and implemented by messaging adapters (Kafka publishers).
 */
public interface PickingEventPublisher extends EventPublisher {
    /**
     * Publishes a Picking domain event.
     *
     * @param event Picking event to publish
     */
    void publish(PickingEvent<?> event);
}
