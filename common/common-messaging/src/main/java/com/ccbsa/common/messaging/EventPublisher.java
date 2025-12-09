package com.ccbsa.common.messaging;

import java.util.List;

import com.ccbsa.common.domain.DomainEvent;

/**
 * Interface for publishing domain events.
 * Implementations handle the actual event publishing mechanism (e.g., Kafka).
 */
public interface EventPublisher {
    /**
     * Publishes a single domain event.
     *
     * @param event The domain event to publish
     */
    void publish(DomainEvent<?> event);

    /**
     * Publishes multiple domain events.
     *
     * @param events The list of domain events to publish
     */
    void publish(List<DomainEvent<?>> events);
}

