package com.ccbsa.wms.product.messaging.publisher;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.product.application.service.port.messaging.ProductEventPublisher;
import com.ccbsa.wms.product.domain.core.event.ProductCreatedEvent;
import com.ccbsa.wms.product.domain.core.event.ProductEvent;
import com.ccbsa.wms.product.domain.core.event.ProductUpdatedEvent;
import com.ccbsa.wms.product.domain.core.valueobject.ProductId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Event Publisher Adapter: ProductEventPublisherAdapter
 * <p>
 * Implements ProductEventPublisher port interface. Publishes Product domain events to Kafka.
 * <p>
 * Responsibilities: - Publishes Product domain events to Kafka topic - Enriches events with metadata (correlation ID, user ID) for traceability - Uses Kafka message key for event
 * ordering (aggregate ID)
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Kafka template is a managed bean and treated as immutable port")
public class ProductEventPublisherAdapter implements ProductEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(ProductEventPublisherAdapter.class);
    private static final String PRODUCT_EVENTS_TOPIC = "product-events";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ProductEventPublisherAdapter(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(List<DomainEvent<?>> events) {
        events.forEach(this::publish);
    }

    @Override
    public void publish(DomainEvent<?> event) {
        if (event instanceof ProductEvent) {
            publish((ProductEvent) event);
        } else {
            throw new IllegalArgumentException(String.format("Event must be a ProductEvent: %s", event.getClass().getName()));
        }
    }

    @Override
    public void publish(ProductEvent event) {
        try {
            // Create enriched event with metadata (immutable copy)
            ProductEvent enrichedEvent = enrichEventWithMetadata(event);

            String key = enrichedEvent.getAggregateId();
            kafkaTemplate.send(PRODUCT_EVENTS_TOPIC, key, enrichedEvent);
            logger.debug("Published product event: {} with key: {} [correlationId: {}]", enrichedEvent.getClass().getSimpleName(), key,
                    enrichedEvent.getMetadata() != null ? enrichedEvent.getMetadata().getCorrelationId() : "none");
        } catch (Exception e) {
            logger.error("Failed to publish product event: {}", event.getClass().getSimpleName(), e);
            throw new RuntimeException("Failed to publish product event", e);
        }
    }

    /**
     * Enriches event with metadata by creating an immutable copy. Uses event-specific logic to create enriched copies.
     *
     * @param event The original event
     * @return Enriched event with metadata, or original if enrichment fails
     */
    private ProductEvent enrichEventWithMetadata(ProductEvent event) {
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

        // Extract productId from aggregateId (now a String)
        ProductId productId = ProductId.of(event.getAggregateId());

        // Create enriched copy based on event type
        if (event instanceof ProductCreatedEvent) {
            ProductCreatedEvent productCreatedEvent = (ProductCreatedEvent) event;
            return new ProductCreatedEvent(productId, productCreatedEvent.getTenantId(), productCreatedEvent.getProductCode(), productCreatedEvent.getDescription(),
                    productCreatedEvent.getPrimaryBarcode(), productCreatedEvent.getSecondaryBarcodes(), productCreatedEvent.getUnitOfMeasure(), productCreatedEvent.getCategory(),
                    productCreatedEvent.getBrand(), metadata);
        } else if (event instanceof ProductUpdatedEvent) {
            ProductUpdatedEvent productUpdatedEvent = (ProductUpdatedEvent) event;
            return new ProductUpdatedEvent(productId, productUpdatedEvent.getTenantId(), productUpdatedEvent.getProductCode(), productUpdatedEvent.getDescription(),
                    productUpdatedEvent.getPrimaryBarcode(), productUpdatedEvent.getSecondaryBarcodes(), productUpdatedEvent.getUnitOfMeasure(), productUpdatedEvent.getCategory(),
                    productUpdatedEvent.getBrand(), metadata);
        }

        // Unknown event type, return original
        logger.warn("Unknown product event type: {}. Event will be published without metadata.", event.getClass().getName());
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

