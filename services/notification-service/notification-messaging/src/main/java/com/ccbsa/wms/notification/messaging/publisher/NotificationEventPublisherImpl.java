package com.ccbsa.wms.notification.messaging.publisher;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.notification.application.service.port.messaging.NotificationEventPublisher;
import com.ccbsa.wms.notification.domain.core.event.NotificationEvent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Event Publisher Implementation: NotificationEventPublisherImpl
 * <p>
 * Implements NotificationEventPublisher port interface. Publishes notification domain events to Kafka.
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Kafka template is a managed bean and treated as immutable port")
public class NotificationEventPublisherImpl implements NotificationEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(NotificationEventPublisherImpl.class);
    private static final String NOTIFICATION_EVENTS_TOPIC = "notification-events";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotificationEventPublisherImpl(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(List<DomainEvent<?>> events) {
        events.forEach(this::publish);
    }

    @Override
    public void publish(DomainEvent<?> event) {
        if (event instanceof NotificationEvent) {
            publish((NotificationEvent) event);
        } else {
            throw new IllegalArgumentException(String.format("Event must be a NotificationEvent: %s", event.getClass().getName()));
        }
    }

    @Override
    public void publish(NotificationEvent event) {
        try {
            // Create enriched event with metadata (immutable copy)
            NotificationEvent enrichedEvent = enrichEventWithMetadata(event);

            String key = enrichedEvent.getAggregateId();
            kafkaTemplate.send(NOTIFICATION_EVENTS_TOPIC, key, enrichedEvent);
            logger.debug("Published notification event: {} with key: {} [correlationId: {}]", enrichedEvent.getClass().getSimpleName(), key,
                    enrichedEvent.getMetadata() != null ? enrichedEvent.getMetadata().getCorrelationId() : "none");
        } catch (Exception e) {
            logger.error("Failed to publish notification event: {}", event.getClass().getSimpleName(), e);
            throw new RuntimeException("Failed to publish notification event", e);
        }
    }

    /**
     * Enriches event with metadata by creating an immutable copy. Uses event-specific logic to create enriched copies.
     *
     * @param event The original event
     * @return Enriched event with metadata, or original if enrichment fails
     */
    private NotificationEvent enrichEventWithMetadata(NotificationEvent event) {
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

        // Extract notificationId from aggregateId (now a String)
        com.ccbsa.wms.notification.domain.core.valueobject.NotificationId notificationId =
                com.ccbsa.wms.notification.domain.core.valueobject.NotificationId.of(event.getAggregateId());

        // Create enriched copy based on event type
        if (event instanceof com.ccbsa.wms.notification.domain.core.event.NotificationCreatedEvent) {
            com.ccbsa.wms.notification.domain.core.event.NotificationCreatedEvent notificationCreatedEvent =
                    (com.ccbsa.wms.notification.domain.core.event.NotificationCreatedEvent) event;
            return new com.ccbsa.wms.notification.domain.core.event.NotificationCreatedEvent(notificationId, notificationCreatedEvent.getTenantId(),
                    notificationCreatedEvent.getType(), metadata);
        } else if (event instanceof com.ccbsa.wms.notification.domain.core.event.NotificationSentEvent) {
            com.ccbsa.wms.notification.domain.core.event.NotificationSentEvent notificationSentEvent = (com.ccbsa.wms.notification.domain.core.event.NotificationSentEvent) event;
            return new com.ccbsa.wms.notification.domain.core.event.NotificationSentEvent(notificationId, notificationSentEvent.getChannel(), notificationSentEvent.getSentAt(),
                    metadata);
        }

        // Unknown event type, return original
        logger.warn("Unknown notification event type: {}. Event will be published without metadata.", event.getClass().getName());
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

