package com.ccbsa.wms.returns.domain.core.event;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.EventMetadata;

/**
 * Base class for all Returns domain events.
 * <p>
 * All returns events extend this class to maintain consistency.
 *
 * @param <T> The type of the aggregate root that raised this event
 */
public abstract class ReturnsEvent<T> extends DomainEvent<T> {
    /**
     * Constructor for returns events without metadata.
     *
     * @param aggregateId   Aggregate identifier (as String)
     * @param aggregateType Aggregate type name
     */
    protected ReturnsEvent(String aggregateId, String aggregateType) {
        super(aggregateId, aggregateType);
    }

    /**
     * Constructor for returns events with metadata.
     *
     * @param aggregateId   Aggregate identifier (as String)
     * @param aggregateType Aggregate type name
     * @param metadata      Event metadata for traceability
     */
    protected ReturnsEvent(String aggregateId, String aggregateType, EventMetadata metadata) {
        super(aggregateId, aggregateType, metadata);
    }
}
