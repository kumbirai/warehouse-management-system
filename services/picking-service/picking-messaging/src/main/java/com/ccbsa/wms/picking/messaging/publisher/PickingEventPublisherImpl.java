package com.ccbsa.wms.picking.messaging.publisher;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.picking.application.service.port.messaging.PickingEventPublisher;
import com.ccbsa.wms.picking.domain.core.event.LoadPlannedEvent;
import com.ccbsa.wms.picking.domain.core.event.OrderMappedToLoadEvent;
import com.ccbsa.wms.picking.domain.core.event.PickingEvent;
import com.ccbsa.wms.picking.domain.core.event.PickingListReceivedEvent;
import com.ccbsa.wms.picking.domain.core.event.PickingTaskCreatedEvent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event Publisher Adapter: PickingEventPublisherImpl
 * <p>
 * Implements PickingEventPublisher port interface. Publishes Picking domain events to Kafka.
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Kafka template is a managed bean")
@Slf4j
@RequiredArgsConstructor
public class PickingEventPublisherImpl implements PickingEventPublisher {
    private static final String PICKING_EVENTS_TOPIC = "picking-events";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(List<DomainEvent<?>> events) {
        events.forEach(this::publish);
    }

    @Override
    public void publish(DomainEvent<?> event) {
        if (event instanceof PickingEvent) {
            publish((PickingEvent<?>) event);
        } else {
            throw new IllegalArgumentException(String.format("Event must be a PickingEvent: %s", event.getClass().getName()));
        }
    }

    @Override
    public void publish(PickingEvent<?> event) {
        try {
            PickingEvent<?> enrichedEvent = enrichEventWithMetadata(event);
            String key = enrichedEvent.getAggregateId();
            kafkaTemplate.send(PICKING_EVENTS_TOPIC, key, enrichedEvent);
            log.debug("Published picking event: {} with key: {} [correlationId: {}]", enrichedEvent.getClass().getSimpleName(), key,
                    enrichedEvent.getMetadata() != null ? enrichedEvent.getMetadata().getCorrelationId() : "none");
        } catch (Exception e) {
            log.error("Failed to publish picking event: {}", event.getClass().getSimpleName(), e);
            throw new RuntimeException("Failed to publish picking event", e);
        }
    }

    private PickingEvent<?> enrichEventWithMetadata(PickingEvent<?> event) {
        EventMetadata metadata = buildEventMetadata();
        if (metadata == null || event.getMetadata() != null) {
            return event;
        }

        // Create enriched copy based on event type
        if (event instanceof PickingListReceivedEvent) {
            PickingListReceivedEvent e = (PickingListReceivedEvent) event;
            return new PickingListReceivedEvent(e.getAggregateId(), e.getTenantId(), e.getLoadIds(), metadata);
        } else if (event instanceof LoadPlannedEvent) {
            LoadPlannedEvent e = (LoadPlannedEvent) event;
            return new LoadPlannedEvent(e.getAggregateId(), e.getTenantId(), e.getPickingListId(), e.getPickingTaskIds(), metadata);
        } else if (event instanceof PickingTaskCreatedEvent) {
            PickingTaskCreatedEvent e = (PickingTaskCreatedEvent) event;
            return new PickingTaskCreatedEvent(e.getAggregateId(), e.getTenantId(), e.getLoadId(), e.getOrderId(), e.getProductCode(), e.getLocationId(), e.getQuantity(),
                    e.getSequence(), metadata);
        } else if (event instanceof OrderMappedToLoadEvent) {
            OrderMappedToLoadEvent e = (OrderMappedToLoadEvent) event;
            return new OrderMappedToLoadEvent(e.getAggregateId(), e.getTenantId(), e.getLoadId(), e.getOrderNumber(), metadata);
        }

        log.warn("Unknown picking event type: {}. Event will be published without metadata.", event.getClass().getName());
        return event;
    }

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
        return metadataBuilder.build();
    }
}
