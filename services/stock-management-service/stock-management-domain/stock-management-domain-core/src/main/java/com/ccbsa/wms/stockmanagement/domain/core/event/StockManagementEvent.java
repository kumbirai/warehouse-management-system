package com.ccbsa.wms.stockmanagement.domain.core.event;

import com.ccbsa.common.domain.DomainEvent;

/**
 * Base class for all Stock Management domain events.
 * <p>
 * All stock management events extend this class to maintain consistency.
 *
 * @param <T> The type of the aggregate root that raised this event
 */
public abstract class StockManagementEvent<T> extends DomainEvent<T> {
    /**
     * Constructor for stock management events without metadata.
     *
     * @param aggregateId   Aggregate identifier (as String)
     * @param aggregateType Aggregate type name
     */
    protected StockManagementEvent(String aggregateId, String aggregateType) {
        super(aggregateId, aggregateType);
    }

    /**
     * Constructor for stock management events with metadata.
     *
     * @param aggregateId   Aggregate identifier (as String)
     * @param aggregateType Aggregate type name
     * @param metadata      Event metadata for traceability
     */
    protected StockManagementEvent(String aggregateId, String aggregateType,
                                   com.ccbsa.common.domain.EventMetadata metadata) {
        super(aggregateId, aggregateType, metadata);
    }
}

