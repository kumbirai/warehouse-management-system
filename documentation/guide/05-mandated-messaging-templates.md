# Mandated Messaging Templates

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.1
**Date:** 2025-01
**Status:** Approved

---

## Overview

Templates for the **Messaging** module (`{service}-messaging`). Handles event-driven choreography.

## Lombok Usage in Messaging Layer

**RECOMMENDED**: Use Lombok for event publishers and listeners to reduce boilerplate.

**Event Publishers**:
- Use `@Slf4j` for logging
- Use `@RequiredArgsConstructor` for dependency injection
- Use `@Component` for Spring bean registration

**Event Listeners**:
- Use `@Slf4j` for logging
- Use `@RequiredArgsConstructor` for dependency injection (if needed)
- Use `@Component` for Spring bean registration
- Combine with `@KafkaListener` for event consumption

**Configuration Classes**:
- Use `@Configuration` for Spring configuration
- Use `@RequiredArgsConstructor` if configuration has dependencies

**Example:**
```java
// Event Publisher with Lombok
@Slf4j
@Component
@RequiredArgsConstructor
public class StockManagementEventPublisherImpl implements StockManagementEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(List<DomainEvent<?>> events) {
        log.info("Publishing {} events", events.size());
        // implementation...
    }
}

// Event Listener with Lombok
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationAssignedEventListener {
    private final StockItemRepository repository;

    @KafkaListener(topics = "location-events", groupId = "stock-service")
    public void onLocationAssigned(LocationAssignedEvent event) {
        log.info("Processing LocationAssignedEvent: {}", event.getLocationId());
        // implementation...
    }
}
```

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

## Kafka Configuration Template (Service-Specific Consumer Factories)

**CRITICAL**: All services using Kafka MUST import `KafkaConfig` and use `@Qualifier("kafkaObjectMapper")` for explicit ObjectMapper injection.

```java
package com.ccbsa.wms.{service}.container.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.ExponentialBackOff;

import com.ccbsa.common.messaging.config.KafkaConfig;
import com.ccbsa.wms.common.dataaccess.config.MultiTenantDataAccessConfig;
import com.ccbsa.wms.common.security.ServiceSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {Service} Configuration
 * <p>
 * Imports Kafka configuration for messaging infrastructure (provides kafkaObjectMapper bean).
 * <p>
 * The {@link KafkaConfig} provides the {@code kafkaObjectMapper} bean used for Kafka message
 * serialization/deserialization with type information.
 */
@Configuration
@Import({ServiceSecurityConfig.class, MultiTenantDataAccessConfig.class, KafkaConfig.class})
public class {Service}Configuration {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.max-poll-records:500}")
    private Integer maxPollRecords;

    @Value("${spring.kafka.consumer.max-poll-interval-ms:300000}")
    private Integer maxPollIntervalMs;

    @Value("${spring.kafka.consumer.session-timeout-ms:30000}")
    private Integer sessionTimeoutMs;

    @Value("${spring.kafka.listener.concurrency:3}")
    private Integer concurrency;

    @Value("${spring.kafka.error-handling.max-retries:3}")
    private Integer maxRetries;

    @Value("${spring.kafka.error-handling.initial-interval:1000}")
    private Long initialInterval;

    @Value("${spring.kafka.error-handling.multiplier:2.0}")
    private Double multiplier;

    @Value("${spring.kafka.error-handling.max-interval:10000}")
    private Long maxInterval;

    /**
     * Custom consumer factory for external events.
     * <p>
     * Uses typed deserialization with @class property from JSON payload.
     * The ObjectMapper is configured with JsonTypeInfo to include @class property,
     * allowing proper typed deserialization across service boundaries.
     * <p>
     * Uses ErrorHandlingDeserializer to gracefully handle deserialization errors.
     * <p>
     * Explicitly uses kafkaObjectMapper to ensure type information is properly deserialized.
     *
     * @param kafkaObjectMapper Kafka ObjectMapper for JSON deserialization (configured with JsonTypeInfo)
     * @return Consumer factory configured for typed deserialization
     */
    @Bean("externalEventConsumerFactory")
    public ConsumerFactory<String, Object> externalEventConsumerFactory(
            @org.springframework.beans.factory.annotation.Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "{service}-service");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        // Configure JsonDeserializer to properly handle polymorphic deserialization
        // When deserializing to Object.class, Jackson uses the ObjectMapper's type information
        // configuration (mixin with @class property in As.PROPERTY format)
        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>(Object.class, kafkaObjectMapper);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.setUseTypeHeaders(false);
        jsonDeserializer.setRemoveTypeHeaders(true);

        ErrorHandlingDeserializer<Object> errorHandlingDeserializer =
                new ErrorHandlingDeserializer<>(jsonDeserializer);

        DefaultKafkaConsumerFactory<String, Object> factory =
                new DefaultKafkaConsumerFactory<>(configProps);
        factory.setValueDeserializer(errorHandlingDeserializer);
        return factory;
    }

    /**
     * Custom listener container factory for external events.
     *
     * @param externalEventConsumerFactory Consumer factory for external events
     * @return Listener container factory
     */
    @Bean("externalEventKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> externalEventKafkaListenerContainerFactory(
            ConsumerFactory<String, Object> externalEventConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(externalEventConsumerFactory);

        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(concurrency);

        BackOff backOff = createExponentialBackOff();
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(backOff);
        errorHandler.addNotRetryableExceptions(
                org.apache.kafka.common.errors.SerializationException.class,
                org.springframework.kafka.support.serializer.DeserializationException.class
        );
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    private BackOff createExponentialBackOff() {
        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(initialInterval);
        backOff.setMultiplier(multiplier);
        backOff.setMaxInterval(maxInterval);
        backOff.setMaxElapsedTime(maxInterval * maxRetries);
        return backOff;
    }
}
```

**ObjectMapper Usage Requirements:**

1. **Import KafkaConfig**: All services using Kafka MUST import `KafkaConfig` via `@Import(KafkaConfig.class)`
2. **Use @Qualifier**: All Kafka-related beans MUST inject `kafkaObjectMapper` using `@Qualifier("kafkaObjectMapper")`
3. **Never use @Primary**: The `kafkaObjectMapper` is explicitly named, never use `@Primary` for Kafka ObjectMapper
4. **Type Information**: Kafka ObjectMapper includes type information (@class property) for polymorphic event deserialization

**See**: `documentation/05-development/ObjectMapper_Separation_Strategy.md` for complete details.

---

## Implementation Checklist

- [ ] Event publishers implement port interfaces from application-service layer
- [ ] Event listeners annotated with `@KafkaListener` and `@Component`
- [ ] Kafka configuration uses `@Qualifier("kafkaObjectMapper")` for ObjectMapper injection
- [ ] Correlation ID extracted from events and set in `CorrelationContext`
- [ ] Idempotency checks implemented in listeners
- [ ] **Event publishers use Lombok** (`@Slf4j`, `@RequiredArgsConstructor`, `@Component`)
- [ ] **Event listeners use Lombok** (`@Slf4j`, `@Component`, optionally `@RequiredArgsConstructor`)
- [ ] **Configuration classes use Lombok** (`@Configuration`, optionally `@RequiredArgsConstructor`)

---

**Document Control**

- **Version History:**
  - v1.1 (2025-01) - Added comprehensive Lombok usage guidelines for publishers, listeners, and configuration
  - v1.0 (2025-01) - Initial template creation
- **Review Cycle:** Review when messaging patterns change

