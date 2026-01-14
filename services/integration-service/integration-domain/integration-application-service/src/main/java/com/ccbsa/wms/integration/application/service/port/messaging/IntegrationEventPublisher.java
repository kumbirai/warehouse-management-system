package com.ccbsa.wms.integration.application.service.port.messaging;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.messaging.EventPublisher;

/**
 * Event Publisher Port: IntegrationEventPublisher
 * <p>
 * Defines the contract for publishing Integration domain events. Extends the base EventPublisher interface from common-messaging.
 * <p>
 * This port is defined in the application service layer and implemented by messaging adapters (Kafka publishers).
 */
public interface IntegrationEventPublisher extends EventPublisher {
    /**
     * Publishes an Integration domain event.
     *
     * @param event Integration event to publish
     */
    void publish(DomainEvent<?> event);
}
