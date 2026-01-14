package com.ccbsa.wms.returns.messaging.publisher;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.messaging.EventEnricher;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.returns.application.service.port.messaging.ReturnsEventPublisher;
import com.ccbsa.wms.returns.domain.core.event.ReturnsEvent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event Publisher Implementation: ReturnsEventPublisherImpl
 * <p>
 * Implements ReturnsEventPublisher port interface. Publishes Returns domain events to Kafka.
 * <p>
 * Responsibilities:
 * - Publishes Returns domain events to Kafka topic
 * - Enriches events with metadata (correlation ID, user ID) for traceability
 * - Uses Kafka message key for event ordering (aggregate ID)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Kafka template is a managed bean and treated as immutable port")
public class ReturnsEventPublisherImpl implements ReturnsEventPublisher {
    private static final String RETURNS_EVENTS_TOPIC = "returns-events";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(List<DomainEvent<?>> events) {
        events.forEach(this::publish);
    }

    @Override
    public void publish(DomainEvent<?> event) {
        if (event instanceof ReturnsEvent) {
            publish((ReturnsEvent<?>) event);
        } else {
            throw new IllegalArgumentException(String.format("Event must be a ReturnsEvent: %s", event.getClass().getName()));
        }
    }

    @Override
    public void publish(ReturnsEvent<?> event) {
        try {
            // Create enriched event with metadata (immutable copy)
            ReturnsEvent<?> enrichedEvent = enrichEventWithMetadata(event);

            String key = enrichedEvent.getAggregateId();
            kafkaTemplate.send(RETURNS_EVENTS_TOPIC, key, enrichedEvent);
            log.info("Published returns event: {} with key: {} to topic: {} [correlationId: {}]", enrichedEvent.getClass().getSimpleName(), key, RETURNS_EVENTS_TOPIC,
                    enrichedEvent.getMetadata() != null ? enrichedEvent.getMetadata().getCorrelationId() : "none");
        } catch (Exception e) {
            log.error("Failed to publish returns event: {}", event.getClass().getSimpleName(), e);
            throw new RuntimeException("Failed to publish returns event", e);
        }
    }

    /**
     * Enriches event with metadata by creating an immutable copy. Uses EventEnricher utility to add correlation ID and user ID.
     *
     * @param event The original event
     * @return Enriched event with metadata, or original if enrichment fails
     */
    private ReturnsEvent<?> enrichEventWithMetadata(ReturnsEvent<?> event) {
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
