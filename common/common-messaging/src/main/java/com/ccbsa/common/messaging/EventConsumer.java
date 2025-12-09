package com.ccbsa.common.messaging;

import com.ccbsa.common.domain.DomainEvent;

/**
 * Interface for consuming domain events.
 * Implementations handle event consumption and processing.
 *
 * @param <T> The type of domain event this consumer handles
 */
public interface EventConsumer<T extends DomainEvent<?>> {
    /**
     * Handles a domain event.
     *
     * @param event The domain event to handle
     */
    void handle(T event);

    /**
     * Returns the event type this consumer handles.
     *
     * @return The class of the event type
     */
    Class<T> getEventType();
}

