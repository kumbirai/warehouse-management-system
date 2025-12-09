package com.ccbsa.common.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events in the system.
 * Domain events represent something that happened in the domain that domain experts care about.
 *
 * <p>Events may include metadata for traceability (correlation ID, causation ID, user ID).
 * Metadata is included in event construction to maintain immutability. Events are immutable value objects.</p>
 *
 * <p>Note: Type information for JSON serialization is configured at the infrastructure layer
 * (messaging module) to keep domain core pure Java without framework dependencies.</p>
 *
 * @param <T> The type of the aggregate root that raised this event
 */
public abstract class DomainEvent<T> {
    private final UUID eventId;
    private final String aggregateId;
    private final String aggregateType;
    private final Instant occurredOn;
    private final int version;
    private final EventMetadata metadata;

    /**
     * Constructor for domain events without metadata.
     * Metadata can be added later using event enrichment pattern in infrastructure layer.
     *
     * @param aggregateId   Aggregate identifier (as String)
     * @param aggregateType Aggregate type name
     */
    protected DomainEvent(String aggregateId,
                          String aggregateType) {
        this.eventId = UUID.randomUUID();
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.occurredOn = Instant.now();
        this.version = 1;
        this.metadata = null;
    }

    /**
     * Constructor for domain events with metadata.
     * Use this constructor when metadata is available at event creation time.
     *
     * @param aggregateId   Aggregate identifier (as String)
     * @param aggregateType Aggregate type name
     * @param metadata      Event metadata for traceability
     */
    protected DomainEvent(String aggregateId,
                          String aggregateType,
                          EventMetadata metadata) {
        this.eventId = UUID.randomUUID();
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.occurredOn = Instant.now();
        this.version = 1;
        this.metadata = metadata;
    }

    /**
     * Legacy constructor for deserialization (backward compatibility).
     *
     * @param eventId       Event identifier
     * @param aggregateId   Aggregate identifier (as String)
     * @param aggregateType Aggregate type name
     * @param occurredOn    When the event occurred
     * @param version       Event version
     * @deprecated Use constructor with EventMetadata parameter instead
     */
    @Deprecated
    protected DomainEvent(UUID eventId,
                          String aggregateId,
                          String aggregateType,
                          Instant occurredOn,
                          int version) {
        this(eventId, aggregateId, aggregateType, occurredOn, version, null);
    }

    /**
     * Constructor for domain events with full details (used for deserialization).
     *
     * @param eventId       Event identifier
     * @param aggregateId   Aggregate identifier (as String)
     * @param aggregateType Aggregate type name
     * @param occurredOn    When the event occurred
     * @param version       Event version
     * @param metadata      Event metadata for traceability
     */
    protected DomainEvent(UUID eventId,
                          String aggregateId,
                          String aggregateType,
                          Instant occurredOn,
                          int version,
                          EventMetadata metadata) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.occurredOn = occurredOn;
        this.version = version;
        this.metadata = metadata;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public Instant getOccurredOn() {
        return occurredOn;
    }

    public int getVersion() {
        return version;
    }

    /**
     * Gets the event metadata for traceability.
     * Metadata includes correlation ID, causation ID, and user ID.
     *
     * @return the event metadata, or null if not set
     */
    public EventMetadata getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DomainEvent<?> that = (DomainEvent<?>) o;
        return eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s{eventId=%s, aggregateId=%s, aggregateType='%s', occurredOn=%s, version=%d, metadata=%s}",
                getClass().getSimpleName(),
                eventId,
                aggregateId,
                aggregateType,
                occurredOn,
                version,
                metadata);
    }
}

