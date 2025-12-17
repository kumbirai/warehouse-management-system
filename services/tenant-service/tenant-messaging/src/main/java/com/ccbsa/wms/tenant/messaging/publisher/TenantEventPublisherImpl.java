package com.ccbsa.wms.tenant.messaging.publisher;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.tenant.application.service.port.messaging.TenantEventPublisher;
import com.ccbsa.wms.tenant.domain.core.event.TenantEvent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Event Publisher Implementation: TenantEventPublisherImpl
 * <p>
 * Implements TenantEventPublisher port interface. Publishes tenant domain events to Kafka.
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Kafka template is a managed bean and treated as immutable port")
public class TenantEventPublisherImpl
        implements TenantEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(TenantEventPublisherImpl.class);
    private static final String TENANT_EVENTS_TOPIC = "tenant-events";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TenantEventPublisherImpl(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(List<DomainEvent<?>> events) {
        events.forEach(this::publish);
    }

    @Override
    public void publish(DomainEvent<?> event) {
        if (event instanceof TenantEvent) {
            publish((TenantEvent<?>) event);
        } else {
            throw new IllegalArgumentException(String.format("Event must be a TenantEvent: %s", event.getClass()
                    .getName()));
        }
    }

    @Override
    public void publish(TenantEvent<?> event) {
        try {
            // Create enriched event with metadata (immutable copy)
            TenantEvent<?> enrichedEvent = enrichEventWithMetadata(event);

            String key = enrichedEvent.getAggregateId();
            kafkaTemplate.send(TENANT_EVENTS_TOPIC, key, enrichedEvent);
            logger.debug("Published tenant event: {} with key: {} [correlationId: {}]", enrichedEvent.getClass()
                    .getSimpleName(), key, enrichedEvent.getMetadata() != null ? enrichedEvent.getMetadata()
                    .getCorrelationId() : "none");
        } catch (Exception e) {
            logger.error("Failed to publish tenant event: {}", event.getClass()
                    .getSimpleName(), e);
            throw new RuntimeException("Failed to publish tenant event", e);
        }
    }

    /**
     * Enriches event with metadata by creating an immutable copy. Uses event-specific logic to create enriched copies.
     *
     * @param event The original event
     * @return Enriched event with metadata, or original if enrichment fails
     */
    private TenantEvent<?> enrichEventWithMetadata(TenantEvent<?> event) {
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

        // Extract tenantId from aggregateId (now a String)
        com.ccbsa.common.domain.valueobject.TenantId tenantId = com.ccbsa.common.domain.valueobject.TenantId.of(event.getAggregateId());

        // Create enriched copy based on event type
        if (event instanceof com.ccbsa.wms.tenant.domain.core.event.TenantCreatedEvent) {
            com.ccbsa.wms.tenant.domain.core.event.TenantCreatedEvent tenantCreatedEvent = (com.ccbsa.wms.tenant.domain.core.event.TenantCreatedEvent) event;
            return new com.ccbsa.wms.tenant.domain.core.event.TenantCreatedEvent(tenantId, tenantCreatedEvent.getName(), tenantCreatedEvent.getStatus(),
                    tenantCreatedEvent.getEmail()
                            .orElse(null), metadata);
        } else if (event instanceof com.ccbsa.wms.tenant.domain.core.event.TenantActivatedEvent) {
            return new com.ccbsa.wms.tenant.domain.core.event.TenantActivatedEvent(tenantId, metadata);
        } else if (event instanceof com.ccbsa.wms.tenant.domain.core.event.TenantDeactivatedEvent) {
            return new com.ccbsa.wms.tenant.domain.core.event.TenantDeactivatedEvent(tenantId, metadata);
        } else if (event instanceof com.ccbsa.wms.tenant.domain.core.event.TenantSuspendedEvent) {
            return new com.ccbsa.wms.tenant.domain.core.event.TenantSuspendedEvent(tenantId, metadata);
        } else if (event instanceof com.ccbsa.wms.tenant.domain.core.event.TenantSchemaCreatedEvent) {
            com.ccbsa.wms.tenant.domain.core.event.TenantSchemaCreatedEvent tenantSchemaCreatedEvent = (com.ccbsa.wms.tenant.domain.core.event.TenantSchemaCreatedEvent) event;
            return new com.ccbsa.wms.tenant.domain.core.event.TenantSchemaCreatedEvent(tenantId, tenantSchemaCreatedEvent.getSchemaName(), metadata);
        } else if (event instanceof com.ccbsa.wms.tenant.domain.core.event.TenantConfigurationUpdatedEvent) {
            com.ccbsa.wms.tenant.domain.core.event.TenantConfigurationUpdatedEvent tenantConfigurationUpdatedEvent =
                    (com.ccbsa.wms.tenant.domain.core.event.TenantConfigurationUpdatedEvent) event;
            return new com.ccbsa.wms.tenant.domain.core.event.TenantConfigurationUpdatedEvent(tenantId, tenantConfigurationUpdatedEvent.getConfiguration(), metadata);
        }

        // Unknown event type, return original
        logger.warn("Unknown tenant event type: {}. Event will be published without metadata.", event.getClass()
                .getName());
        return event;
    }

    /**
     * Builds event metadata from CorrelationContext and TenantContext.
     *
     * @return EventMetadata, or null if no metadata available
     */
    private EventMetadata buildEventMetadata() {
        String correlationId = CorrelationContext.getCorrelationId();
        String userId = TenantContext.getUserId() != null ? TenantContext.getUserId()
                .getValue() : null;

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

