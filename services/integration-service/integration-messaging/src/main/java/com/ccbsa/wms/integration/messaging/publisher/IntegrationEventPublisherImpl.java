package com.ccbsa.wms.integration.messaging.publisher;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.wms.integration.application.service.port.messaging.IntegrationEventPublisher;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event Publisher Implementation: IntegrationEventPublisherImpl
 * <p>
 * Implements IntegrationEventPublisher for publishing Integration domain events to Kafka.
 * <p>
 * Publishes events to the `integration-events` topic with proper metadata enrichment.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Kafka template is a managed bean and treated as immutable port")
public class IntegrationEventPublisherImpl implements IntegrationEventPublisher {
    private static final String INTEGRATION_EVENTS_TOPIC = "integration-events";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(List<DomainEvent<?>> events) {
        events.forEach(this::publish);
    }

    @Override
    public void publish(DomainEvent<?> event) {
        try {
            // Create enriched event with metadata (immutable copy)
            DomainEvent<?> enrichedEvent = enrichEventWithMetadata(event);

            String key = enrichedEvent.getAggregateId();
            kafkaTemplate.send(INTEGRATION_EVENTS_TOPIC, key, enrichedEvent);
            log.debug("Published integration event: {} with key: {} [correlationId: {}]", enrichedEvent.getClass().getSimpleName(), key,
                    enrichedEvent.getMetadata() != null ? enrichedEvent.getMetadata().getCorrelationId() : "none");
        } catch (Exception e) {
            log.error("Failed to publish integration event: {}", event.getClass().getSimpleName(), e);
            throw new RuntimeException("Failed to publish integration event", e);
        }
    }

    /**
     * Enriches event with metadata by creating an immutable copy.
     *
     * @param event The original event
     * @return Enriched event with metadata, or original if enrichment fails
     */
    private DomainEvent<?> enrichEventWithMetadata(DomainEvent<?> event) {
        // Build metadata from context
        String correlationId = CorrelationContext.getCorrelationId();
        if (correlationId == null && event.getMetadata() != null) {
            correlationId = event.getMetadata().getCorrelationId();
        }

        // If event already has metadata, use it; otherwise create new metadata
        EventMetadata metadata = event.getMetadata();
        if (metadata == null && correlationId != null) {
            metadata = EventMetadata.builder().correlationId(correlationId).build();
        } else if (metadata != null && correlationId != null && !correlationId.equals(metadata.getCorrelationId())) {
            // Update correlation ID if different
            metadata = EventMetadata.builder().correlationId(correlationId).causationId(metadata.getCausationId()).userId(metadata.getUserId()).build();
        }

        // If metadata changed, create new event instance with updated metadata
        if (metadata != null && !metadata.equals(event.getMetadata())) {
            // For ReturnReconciledEvent, create a new instance with updated metadata
            if (event instanceof com.ccbsa.wms.integration.domain.core.event.ReturnReconciledEvent) {
                com.ccbsa.wms.integration.domain.core.event.ReturnReconciledEvent returnEvent = (com.ccbsa.wms.integration.domain.core.event.ReturnReconciledEvent) event;
                com.ccbsa.common.domain.valueobject.ReturnId returnId = com.ccbsa.common.domain.valueobject.ReturnId.of(returnEvent.getAggregateId());
                return new com.ccbsa.wms.integration.domain.core.event.ReturnReconciledEvent(returnId, returnEvent.getTenantId(), returnEvent.getD365ReturnOrderId(),
                        returnEvent.isInventoryAdjusted(), returnEvent.getCreditNoteId(), returnEvent.isWriteOffProcessed(), metadata);
            }
        }

        return event;
    }
}
