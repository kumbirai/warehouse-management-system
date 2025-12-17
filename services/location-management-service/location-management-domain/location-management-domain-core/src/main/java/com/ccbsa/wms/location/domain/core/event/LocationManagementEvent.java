package com.ccbsa.wms.location.domain.core.event;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Base class for all Location Management domain events.
 * <p>
 * All location-specific events extend this class.
 */
public abstract class LocationManagementEvent
        extends DomainEvent<LocationId> {
    /**
     * Constructor for Location Management events without metadata.
     *
     * @param aggregateId Aggregate identifier (LocationId)
     * @throws IllegalArgumentException if aggregateId is null
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW",
            justification = "Location events must fail fast if aggregate identifiers are invalid")
    protected LocationManagementEvent(LocationId aggregateId) {
        super(extractLocationIdString(aggregateId), "Location");
    }

    /**
     * Extracts the LocationId string value from the aggregate identifier.
     *
     * @param locationId Location identifier
     * @return String representation of location ID
     */
    private static String extractLocationIdString(LocationId locationId) {
        if (locationId == null) {
            throw new IllegalArgumentException("LocationId cannot be null");
        }
        return locationId.getValue()
                .toString();
    }

    /**
     * Constructor for Location Management events with metadata.
     *
     * @param aggregateId Aggregate identifier (LocationId)
     * @param metadata    Event metadata for traceability
     * @throws IllegalArgumentException if aggregateId is null
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW",
            justification = "Location events must fail fast if aggregate identifiers are invalid")
    protected LocationManagementEvent(LocationId aggregateId, EventMetadata metadata) {
        super(extractLocationIdString(aggregateId), "Location", metadata);
    }
}

