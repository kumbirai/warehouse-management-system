package com.ccbsa.wms.returns.application.service.port.messaging;

import com.ccbsa.common.messaging.EventPublisher;
import com.ccbsa.wms.returns.domain.core.event.ReturnsEvent;

/**
 * Event Publisher Port: ReturnsEventPublisher
 * <p>
 * Defines the contract for publishing Returns domain events. Extends the base EventPublisher interface from common-messaging.
 * <p>
 * This port is defined in the application service layer and implemented by messaging adapters (Kafka publishers).
 */
public interface ReturnsEventPublisher extends EventPublisher {
    /**
     * Publishes a Returns domain event.
     *
     * @param event Returns event to publish
     */
    void publish(ReturnsEvent<?> event);
}
