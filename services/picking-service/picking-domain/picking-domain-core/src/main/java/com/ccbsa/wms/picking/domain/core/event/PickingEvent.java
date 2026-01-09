package com.ccbsa.wms.picking.domain.core.event;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.EventMetadata;

/**
 * Base class for all Picking domain events.
 * <p>
 * All picking events extend this class to maintain consistency.
 *
 * @param <T> The type of the aggregate root that raised this event
 */
public abstract class PickingEvent<T> extends DomainEvent<T> {
    /**
     * Constructor for picking events without metadata.
     *
     * @param aggregateId   Aggregate identifier (as String)
     * @param aggregateType Aggregate type name
     */
    protected PickingEvent(String aggregateId, String aggregateType) {
        super(aggregateId, aggregateType);
    }

    /**
     * Constructor for picking events with metadata.
     *
     * @param aggregateId   Aggregate identifier (as String)
     * @param aggregateType Aggregate type name
     * @param metadata      Event metadata for traceability
     */
    protected PickingEvent(String aggregateId, String aggregateType, EventMetadata metadata) {
        super(aggregateId, aggregateType, metadata);
    }
}
