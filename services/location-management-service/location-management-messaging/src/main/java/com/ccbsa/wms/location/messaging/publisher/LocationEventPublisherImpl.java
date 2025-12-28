package com.ccbsa.wms.location.messaging.publisher;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.location.application.service.port.messaging.LocationEventPublisher;
import com.ccbsa.wms.location.domain.core.event.LocationAssignedEvent;
import com.ccbsa.wms.location.domain.core.event.LocationBlockedEvent;
import com.ccbsa.wms.location.domain.core.event.LocationCreatedEvent;
import com.ccbsa.wms.location.domain.core.event.LocationManagementEvent;
import com.ccbsa.wms.location.domain.core.event.LocationStatusChangedEvent;
import com.ccbsa.wms.location.domain.core.event.LocationUnblockedEvent;
import com.ccbsa.wms.location.domain.core.event.StockMovementCancelledEvent;
import com.ccbsa.wms.location.domain.core.event.StockMovementCompletedEvent;
import com.ccbsa.wms.location.domain.core.event.StockMovementInitiatedEvent;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Event Publisher Implementation: LocationEventPublisherImpl
 * <p>
 * Implements LocationEventPublisher port interface. Publishes location domain events to Kafka.
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Kafka template is a managed bean and treated as immutable port")
public class LocationEventPublisherImpl implements LocationEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(LocationEventPublisherImpl.class);
    private static final String LOCATION_EVENTS_TOPIC = "location-management-events";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public LocationEventPublisherImpl(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(List<DomainEvent<?>> events) {
        events.forEach(this::publish);
    }

    @Override
    public void publish(DomainEvent<?> event) {
        if (event instanceof LocationManagementEvent) {
            publish((LocationManagementEvent) event);
        } else {
            throw new IllegalArgumentException(String.format("Event must be a LocationManagementEvent: %s", event.getClass().getName()));
        }
    }

    @Override
    public void publish(LocationManagementEvent event) {
        try {
            // Create enriched event with metadata (immutable copy)
            LocationManagementEvent enrichedEvent = enrichEventWithMetadata(event);

            String key = enrichedEvent.getAggregateId();
            kafkaTemplate.send(LOCATION_EVENTS_TOPIC, key, enrichedEvent);
            logger.debug("Published location event: {} with key: {} [correlationId: {}]", enrichedEvent.getClass().getSimpleName(), key,
                    enrichedEvent.getMetadata() != null ? enrichedEvent.getMetadata().getCorrelationId() : "none");
        } catch (Exception e) {
            logger.error("Failed to publish location event: {}", event.getClass().getSimpleName(), e);
            throw new RuntimeException("Failed to publish location event", e);
        }
    }

    /**
     * Enriches event with metadata by creating an immutable copy. Uses event-specific logic to create enriched copies.
     *
     * @param event The original event
     * @return Enriched event with metadata, or original if enrichment fails
     */
    private LocationManagementEvent enrichEventWithMetadata(LocationManagementEvent event) {
        // Build metadata from context
        EventMetadata metadata = buildEventMetadata();
        if (metadata == null) {
            // No metadata to add, return original event
            return event;
        }

        // If event already has metadata, return as-is
        if (event.getMetadata() != null) {
            return event;
        }

        // Extract locationId from aggregateId (now a String)
        LocationId locationId = LocationId.of(event.getAggregateId());

        // Create enriched copy based on event type
        if (event instanceof LocationCreatedEvent) {
            LocationCreatedEvent locationCreatedEvent = (LocationCreatedEvent) event;
            return new LocationCreatedEvent(locationId, locationCreatedEvent.getTenantId(), locationCreatedEvent.getBarcode(), locationCreatedEvent.getCoordinates(),
                    locationCreatedEvent.getStatus(), metadata);
        }

        if (event instanceof LocationAssignedEvent) {
            LocationAssignedEvent locationAssignedEvent = (LocationAssignedEvent) event;
            return new LocationAssignedEvent(locationId, locationAssignedEvent.getTenantId(), locationAssignedEvent.getStockItemId(), locationAssignedEvent.getQuantity(),
                    metadata);
        }

        if (event instanceof LocationStatusChangedEvent) {
            LocationStatusChangedEvent locationStatusChangedEvent = (LocationStatusChangedEvent) event;
            return new LocationStatusChangedEvent(locationId, locationStatusChangedEvent.getTenantId(), locationStatusChangedEvent.getOldStatus(),
                    locationStatusChangedEvent.getNewStatus(), metadata);
        }

        if (event instanceof StockMovementInitiatedEvent) {
            StockMovementInitiatedEvent stockMovementInitiatedEvent = (StockMovementInitiatedEvent) event;
            return new StockMovementInitiatedEvent(stockMovementInitiatedEvent.getMovementId(), stockMovementInitiatedEvent.getTenantId(),
                    stockMovementInitiatedEvent.getStockItemId(), stockMovementInitiatedEvent.getProductId(), stockMovementInitiatedEvent.getSourceLocationId(),
                    stockMovementInitiatedEvent.getDestinationLocationId(), stockMovementInitiatedEvent.getQuantity(), stockMovementInitiatedEvent.getMovementType(),
                    stockMovementInitiatedEvent.getReason(), stockMovementInitiatedEvent.getInitiatedBy(), stockMovementInitiatedEvent.getInitiatedAt(), metadata);
        }

        if (event instanceof StockMovementCompletedEvent) {
            StockMovementCompletedEvent stockMovementCompletedEvent = (StockMovementCompletedEvent) event;
            return new StockMovementCompletedEvent(stockMovementCompletedEvent.getMovementId(), stockMovementCompletedEvent.getTenantId(),
                    stockMovementCompletedEvent.getStockItemId(), stockMovementCompletedEvent.getProductId(), stockMovementCompletedEvent.getSourceLocationId(),
                    stockMovementCompletedEvent.getDestinationLocationId(), stockMovementCompletedEvent.getQuantity(), stockMovementCompletedEvent.getMovementType(),
                    stockMovementCompletedEvent.getReason(), stockMovementCompletedEvent.getInitiatedBy(), stockMovementCompletedEvent.getInitiatedAt(),
                    stockMovementCompletedEvent.getCompletedBy(), stockMovementCompletedEvent.getCompletedAt(), metadata);
        }

        if (event instanceof StockMovementCancelledEvent) {
            StockMovementCancelledEvent stockMovementCancelledEvent = (StockMovementCancelledEvent) event;
            return new StockMovementCancelledEvent(stockMovementCancelledEvent.getMovementId(), stockMovementCancelledEvent.getTenantId(),
                    stockMovementCancelledEvent.getStockItemId(), stockMovementCancelledEvent.getSourceLocationId(), stockMovementCancelledEvent.getDestinationLocationId(),
                    stockMovementCancelledEvent.getCancelledBy(), stockMovementCancelledEvent.getCancelledAt(), stockMovementCancelledEvent.getCancellationReason(), metadata);
        }

        if (event instanceof LocationBlockedEvent) {
            LocationBlockedEvent locationBlockedEvent = (LocationBlockedEvent) event;
            return new LocationBlockedEvent(locationBlockedEvent.getLocationId(), locationBlockedEvent.getTenantId(), locationBlockedEvent.getBlockedBy(),
                    locationBlockedEvent.getReason(), locationBlockedEvent.getBlockedAt(), metadata);
        }

        if (event instanceof LocationUnblockedEvent) {
            LocationUnblockedEvent locationUnblockedEvent = (LocationUnblockedEvent) event;
            return new LocationUnblockedEvent(locationUnblockedEvent.getLocationId(), locationUnblockedEvent.getTenantId(), locationUnblockedEvent.getUnblockedBy(),
                    locationUnblockedEvent.getNewStatus(), locationUnblockedEvent.getUnblockedAt(), metadata);
        }

        // Unknown event type, return original
        logger.warn("Unknown location event type: {}. Event will be published without metadata.", event.getClass().getName());
        return event;
    }

    /**
     * Builds event metadata from CorrelationContext and TenantContext.
     *
     * @return EventMetadata, or null if no metadata available
     */
    private EventMetadata buildEventMetadata() {
        String correlationId = CorrelationContext.getCorrelationId();
        String userId = TenantContext.getUserId() != null ? TenantContext.getUserId().getValue() : null;

        if (correlationId == null && userId == null) {
            return null;
        }

        EventMetadata.Builder metadataBuilder = EventMetadata.builder();
        if (correlationId != null) {
            metadataBuilder.correlationId(correlationId);
        }
        if (userId != null) {
            metadataBuilder.userId(userId);
        }
        // Causation ID is set when events are published as a result of consuming other events
        // For command-initiated events, causation ID is null
        return metadataBuilder.build();
    }
}

