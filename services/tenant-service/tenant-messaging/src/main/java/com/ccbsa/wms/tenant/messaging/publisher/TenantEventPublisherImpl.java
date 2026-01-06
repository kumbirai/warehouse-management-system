package com.ccbsa.wms.tenant.messaging.publisher;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.tenant.application.service.port.messaging.TenantEventPublisher;
import com.ccbsa.wms.tenant.domain.core.event.TenantActivatedEvent;
import com.ccbsa.wms.tenant.domain.core.event.TenantConfigurationUpdatedEvent;
import com.ccbsa.wms.tenant.domain.core.event.TenantCreatedEvent;
import com.ccbsa.wms.tenant.domain.core.event.TenantDeactivatedEvent;
import com.ccbsa.wms.tenant.domain.core.event.TenantEvent;
import com.ccbsa.wms.tenant.domain.core.event.TenantSchemaCreatedEvent;
import com.ccbsa.wms.tenant.domain.core.event.TenantSuspendedEvent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event Publisher Implementation: TenantEventPublisherImpl
 * <p>
 * Implements TenantEventPublisher port interface. Publishes tenant domain events to Kafka.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Kafka template is a managed bean and treated as immutable port")
public class TenantEventPublisherImpl implements TenantEventPublisher {
    private static final String TENANT_EVENTS_TOPIC = "tenant-events";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(List<DomainEvent<?>> events) {
        events.forEach(this::publish);
    }

    @Override
    public void publish(DomainEvent<?> event) {
        if (event instanceof TenantEvent) {
            publish((TenantEvent<?>) event);
        } else {
            throw new IllegalArgumentException(String.format("Event must be a TenantEvent: %s", event.getClass().getName()));
        }
    }

    @Override
    public void publish(TenantEvent<?> event) {
        try {
            // Create enriched event with metadata (immutable copy)
            TenantEvent<?> enrichedEvent = enrichEventWithMetadata(event);

            String key = enrichedEvent.getAggregateId();
            String eventType = enrichedEvent.getClass().getSimpleName();
            String correlationId = enrichedEvent.getMetadata() != null ? enrichedEvent.getMetadata().getCorrelationId() : "none";

            // Send event and wait for completion to ensure it's published
            // Using get() with timeout to make it synchronous and catch any send failures immediately
            // Timeout of 30 seconds should be sufficient for Kafka send operations
            SendResult<String, Object> sendResult = kafkaTemplate.send(TENANT_EVENTS_TOPIC, key, enrichedEvent).get(30, TimeUnit.SECONDS);

            log.info("Published tenant event: {} with key: {} to topic: {} [partition: {}, offset: {}, correlationId: {}]", eventType, key, TENANT_EVENTS_TOPIC,
                    sendResult.getRecordMetadata().partition(), sendResult.getRecordMetadata().offset(), correlationId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while publishing tenant event: {} to topic: {}", event.getClass().getSimpleName(), TENANT_EVENTS_TOPIC, e);
            throw new RuntimeException("Failed to publish tenant event: interrupted", e);
        } catch (ExecutionException e) {
            log.error("Failed to publish tenant event: {} to topic: {}", event.getClass().getSimpleName(), TENANT_EVENTS_TOPIC, e);
            throw new RuntimeException("Failed to publish tenant event", e.getCause() != null ? e.getCause() : e);
        } catch (TimeoutException e) {
            log.error("Timeout while publishing tenant event: {} to topic: {}", event.getClass().getSimpleName(), TENANT_EVENTS_TOPIC, e);
            throw new RuntimeException("Failed to publish tenant event: timeout", e);
        } catch (Exception e) {
            log.error("Unexpected error while publishing tenant event: {} to topic: {}", event.getClass().getSimpleName(), TENANT_EVENTS_TOPIC, e);
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
        TenantId tenantId = TenantId.of(event.getAggregateId());

        // Create enriched copy based on event type
        if (event instanceof TenantCreatedEvent) {
            TenantCreatedEvent tenantCreatedEvent = (TenantCreatedEvent) event;
            return new TenantCreatedEvent(tenantId, tenantCreatedEvent.getName(), tenantCreatedEvent.getStatus(), tenantCreatedEvent.getEmail().orElse(null), metadata);
        } else if (event instanceof TenantActivatedEvent) {
            return new TenantActivatedEvent(tenantId, metadata);
        } else if (event instanceof TenantDeactivatedEvent) {
            return new TenantDeactivatedEvent(tenantId, metadata);
        } else if (event instanceof TenantSuspendedEvent) {
            return new TenantSuspendedEvent(tenantId, metadata);
        } else if (event instanceof TenantSchemaCreatedEvent) {
            TenantSchemaCreatedEvent tenantSchemaCreatedEvent = (TenantSchemaCreatedEvent) event;
            return new TenantSchemaCreatedEvent(tenantId, tenantSchemaCreatedEvent.getSchemaName(), metadata);
        } else if (event instanceof TenantConfigurationUpdatedEvent) {
            TenantConfigurationUpdatedEvent tenantConfigurationUpdatedEvent = (TenantConfigurationUpdatedEvent) event;
            return new TenantConfigurationUpdatedEvent(tenantId, tenantConfigurationUpdatedEvent.getConfiguration(), metadata);
        }

        // Unknown event type, return original
        log.warn("Unknown tenant event type: {}. Event will be published without metadata.", event.getClass().getName());
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

