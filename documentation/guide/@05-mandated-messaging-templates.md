# Mandated Messaging Templates

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-01  
**Status:** Approved

---

## Overview

Templates for the **Messaging** module (`{service}-messaging`). Handles event-driven choreography.

---

## Package Structure

The Messaging module (`{service}-messaging`) follows a strict package structure to enforce event-driven patterns:

```
com.ccbsa.wms.{service}.messaging/
├── publisher/                         # Event publishers
│   └── {Service}EventPublisherImpl.java
├── listener/                          # Event listeners
│   ├── {ExternalEvent}Listener.java              # External event listeners
│   └── {DomainObject}ProjectionListener.java    # Projection listeners
├── mapper/                           # Event mappers (optional)
│   └── {DomainObject}EventMapper.java
└── config/                            # Kafka configuration
    └── KafkaConfig.java
```

**Package Naming Convention:**

- Base package: `com.ccbsa.wms.{service}.messaging`
- Replace `{service}` with actual service name (e.g., `stock`, `location`, `product`)
- Replace `{DomainObject}` with actual domain object name (e.g., `StockConsignment`, `Location`, `Product`)

**Package Responsibilities:**

| Package     | Responsibility      | Contains                                                                                                                        |
|-------------|---------------------|---------------------------------------------------------------------------------------------------------------------------------|
| `publisher` | Event publishers    | Implements event publisher ports, publishes domain events to Kafka, annotated with `@Component`                                 |
| `listener`  | Event listeners     | Consumes events from Kafka, processes external events and updates projections, annotated with `@Component` and `@KafkaListener` |
| `mapper`    | Event mappers       | Converts between domain events and message formats, optional if direct serialization used                                       |
| `config`    | Kafka configuration | Kafka producer/consumer configuration, annotated with `@Configuration`                                                          |

**Important Package Rules:**

- **Publisher pattern**: Publishers implement ports defined in application service layer
- **Listener pattern**: Listeners handle events asynchronously
- **Idempotency**: All event handlers must be idempotent
- **Error handling**: Dead letter queue for failed events
- **Event correlation**: Track correlation IDs for event tracing

---

## Important Domain Driven Design, Clean Hexagonal Architecture principles, CQRS and Event Driven Design Notes

**Domain-Driven Design (DDD) Principles:**

- Messaging layer is infrastructure (outer layer)
- Event publishers adapt domain events to messaging infrastructure
- Domain events represent business facts
- Event handlers maintain consistency across bounded contexts

**Clean Hexagonal Architecture Principles:**

- Event publishers implement ports defined in application service layer
- Dependency direction: Application Service → Messaging
- Domain core has no knowledge of messaging infrastructure
- Infrastructure adapts to domain events, not vice versa

**CQRS Principles:**

- **Event publishing**: Commands publish events after successful commit
- **Projection updates**: Read models updated via event projections
- **Eventual consistency**: Read models eventually consistent with write model
- **Separation**: Write model persists aggregates, read models updated via events

**Event-Driven Design Principles:**

- **Event choreography**: Services communicate through events only
- **Loose coupling**: Services don't know about each other
- **Idempotency**: Event handlers must handle duplicate events
- **Event ordering**: Events within aggregate processed in order
- **Event versioning**: Events support schema evolution
- **Traceability**: Correlation IDs tracked through event metadata for end-to-end tracing

---

## Event Publisher Implementation Template

```java
package com.ccbsa.wms.{service}.messaging.publisher;

import com.ccbsa.wms.{service}.application.service.port.messaging.{Service}EventPublisher;
import com.ccbsa.wms.{service}.domain.core.event.{Service}Event;
import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.wms.common.security.TenantContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class {Service}EventPublisherImpl implements {Service}EventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "{service}-events";
    
    public {Service}EventPublisherImpl(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    @Override
    public void publish(DomainEvent<?> event) {
        // Inject event metadata for traceability
        injectEventMetadata(event);
        
        String key = event.getAggregateId().toString();
        kafkaTemplate.send(TOPIC, key, event);
    }
    
    @Override
    public void publish(List<DomainEvent<?>> events) {
        events.forEach(this::publish);
    }
    
    @Override
    public void publish({Service}Event<?> event) {
        publish((DomainEvent<?>) event);
    }
    
    @Override
    public void publish(List<{Service}Event<?>> events) {
        events.forEach(this::publish);
    }
    
    /**
     * Injects event metadata (correlation ID, user ID) into the event for traceability.
     * Metadata is extracted from CorrelationContext and TenantContext.
     */
    private void injectEventMetadata(DomainEvent<?> event) {
        String correlationId = CorrelationContext.getCorrelationId();
        String userId = TenantContext.getUserId() != null
                ? TenantContext.getUserId().getValue()
                : null;

        if (correlationId != null || userId != null) {
            EventMetadata.Builder metadataBuilder = EventMetadata.builder();
            if (correlationId != null) {
                metadataBuilder.correlationId(correlationId);
            }
            if (userId != null) {
                metadataBuilder.userId(userId);
            }
            // Causation ID is set when events are published as a result of consuming other events
            // For command-initiated events, causation ID is null
            event.setMetadata(metadataBuilder.build());
        }
    }
}
```

## Event Listener Template

```java
package com.ccbsa.wms.{service}.messaging.listener;

import com.ccbsa.wms.{service}.domain.core.event.{ExternalEvent};
import com.ccbsa.wms.{service}.application.service.port.repository.{DomainObject}Repository;
import com.ccbsa.common.application.context.CorrelationContext;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class {ExternalEvent}Listener {
    
    private final {DomainObject}Repository repository;
    private final EventStore eventStore; // For idempotency
    
    @KafkaListener(
        topics = "{external-service}-events",
        groupId = "{service}-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handle({ExternalEvent} event, Acknowledgment acknowledgment) {
        try {
            // Extract and set correlation ID from event metadata for traceability
            extractAndSetCorrelationId(event);
            
            // 1. Check idempotency
            if (eventStore.exists(event.getEventId())) {
                acknowledgment.acknowledge();
                return; // Already processed
            }
            
            // 2. Process event
            processEvent(event);
            
            // 3. Store event ID for idempotency
            eventStore.store(event.getEventId());
            
            // 4. Acknowledge
            acknowledgment.acknowledge();
        } catch (Exception e) {
            // Handle error, don't acknowledge (will retry)
            throw e;
        } finally {
            // Clear correlation context after processing
            CorrelationContext.clear();
        }
    }
    
    private void processEvent({ExternalEvent} event) {
        // Business logic to handle event
        // Load aggregate, modify, save
    }
    
    /**
     * Extracts correlation ID from event metadata and sets it in CorrelationContext.
     * This enables traceability through event chains.
     */
    private void extractAndSetCorrelationId({ExternalEvent} event) {
        EventMetadata metadata = event.getMetadata();
        if (metadata != null && metadata.getCorrelationId() != null) {
            CorrelationContext.setCorrelationId(metadata.getCorrelationId());
        }
    }
}
```

## Projection Listener Template

```java
package com.ccbsa.wms.{service}.messaging.listener;

import com.ccbsa.wms.{service}.domain.core.event.{DomainObject}UpdatedEvent;
import com.ccbsa.wms.{service}.dataaccess.projection.{DomainObject}ViewRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class {DomainObject}ProjectionListener {
    
    private final {DomainObject}ViewRepository viewRepository;
    
    @KafkaListener(topics = "{service}-events", groupId = "{service}-projections")
    public void updateProjection({DomainObject}UpdatedEvent event) {
        // Update read model from event
        viewRepository.updateFromEvent(event);
    }
}
```

## EventMetadata Structure

Events include optional `EventMetadata` for traceability:

```java
EventMetadata metadata = EventMetadata.builder()
    .correlationId("req-123")           // Tracks entire business flow
    .causationId(UUID.fromString("...")) // Parent event ID (null for command-initiated events)
    .userId("user-456")                 // User who triggered the event
    .build();
```

**Metadata Fields:**

- **correlationId**: Tracks entire business flow across services and operations
- **causationId**: Event ID of the event that caused this event (for event chains)
- **userId**: User identifier who triggered the event

## Traceability Flow

**Event Publishing Flow:**

1. Command handler executes business logic
2. Domain events generated by aggregate
3. Event publisher extracts correlation ID from `CorrelationContext`
4. Event publisher extracts user ID from `TenantContext`
5. Event publisher creates `EventMetadata` and sets on event
6. Event published to Kafka with metadata

**Event Consumption Flow:**

1. Event listener receives event from Kafka
2. Event listener extracts correlation ID from event metadata
3. Event listener sets correlation ID in `CorrelationContext`
4. Event listener processes event (may publish new events)
5. New events published will include same correlation ID
6. Correlation context cleared after processing

---

**Document Control**

- **Version History:** v1.0 (2025-01) - Initial template creation
- **Review Cycle:** Review when messaging patterns change

