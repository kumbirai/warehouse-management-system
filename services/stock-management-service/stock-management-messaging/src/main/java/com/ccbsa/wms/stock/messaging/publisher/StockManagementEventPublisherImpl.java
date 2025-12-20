package com.ccbsa.wms.stock.messaging.publisher;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.messaging.EventEnricher;
import com.ccbsa.wms.stock.application.service.port.messaging.StockManagementEventPublisher;
import com.ccbsa.wms.stock.domain.core.event.StockManagementEvent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Event Publisher Implementation: StockManagementEventPublisherImpl
 * <p>
 * Implements StockManagementEventPublisher port interface. Publishes Stock Management domain events to Kafka.
 * <p>
 * Responsibilities: - Publishes Stock Management domain events to Kafka topic - Enriches events with metadata (correlation ID, user ID) for traceability - Uses Kafka message key
 * for event ordering (aggregate ID)
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Kafka template is a managed bean and treated as immutable port")
public class StockManagementEventPublisherImpl implements StockManagementEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(StockManagementEventPublisherImpl.class);
    private static final String STOCK_MANAGEMENT_EVENTS_TOPIC = "stock-management-events";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public StockManagementEventPublisherImpl(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(List<DomainEvent<?>> events) {
        events.forEach(this::publish);
    }

    @Override
    public void publish(DomainEvent<?> event) {
        if (event instanceof StockManagementEvent) {
            publish((StockManagementEvent<?>) event);
        } else {
            throw new IllegalArgumentException(String.format("Event must be a StockManagementEvent: %s", event.getClass().getName()));
        }
    }

    @Override
    public void publish(StockManagementEvent<?> event) {
        try {
            // Create enriched event with metadata (immutable copy)
            StockManagementEvent<?> enrichedEvent = enrichEventWithMetadata(event);

            String key = enrichedEvent.getAggregateId();
            kafkaTemplate.send(STOCK_MANAGEMENT_EVENTS_TOPIC, key, enrichedEvent);
            logger.debug("Published stock management event: {} with key: {} [correlationId: {}]", enrichedEvent.getClass().getSimpleName(), key,
                    enrichedEvent.getMetadata() != null ? enrichedEvent.getMetadata().getCorrelationId() : "none");
        } catch (Exception e) {
            logger.error("Failed to publish stock management event: {}", event.getClass().getSimpleName(), e);
            throw new RuntimeException("Failed to publish stock management event", e);
        }
    }

    /**
     * Enriches event with metadata by creating an immutable copy. Uses EventEnricher utility to add correlation ID and user ID.
     *
     * @param event The original event
     * @return Enriched event with metadata, or original if enrichment fails
     */
    private StockManagementEvent<?> enrichEventWithMetadata(StockManagementEvent<?> event) {
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

        // Use EventEnricher to create enriched copy
        return EventEnricher.enrich(event, metadata);
    }

    /**
     * Builds event metadata from CorrelationContext.
     *
     * @return EventMetadata, or null if no metadata available
     */
    private EventMetadata buildEventMetadata() {
        String correlationId = CorrelationContext.getCorrelationId();
        // User ID can be added from security context if needed
        // For now, we'll just use correlation ID

        if (correlationId == null) {
            return null;
        }

        EventMetadata.Builder metadataBuilder = EventMetadata.builder();
        metadataBuilder.correlationId(correlationId);
        // Causation ID is set when events are published as a result of consuming other events
        // For command-initiated events, causation ID is null
        return metadataBuilder.build();
    }
}

