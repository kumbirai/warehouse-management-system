package com.ccbsa.wms.user.messaging.publisher;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.user.application.service.port.messaging.UserEventPublisher;
import com.ccbsa.wms.user.domain.core.event.UserCreatedEvent;
import com.ccbsa.wms.user.domain.core.event.UserDeactivatedEvent;
import com.ccbsa.wms.user.domain.core.event.UserEvent;
import com.ccbsa.wms.user.domain.core.event.UserRoleAssignedEvent;
import com.ccbsa.wms.user.domain.core.event.UserRoleRemovedEvent;
import com.ccbsa.wms.user.domain.core.event.UserUpdatedEvent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event Publisher Implementation: UserEventPublisherImpl
 * <p>
 * Implements UserEventPublisher port interface. Publishes user domain events to Kafka.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Kafka template is a managed bean and treated as immutable port")
public class UserEventPublisherImpl implements UserEventPublisher {
    private static final String USER_EVENTS_TOPIC = "user-events";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(List<DomainEvent<?>> events) {
        events.forEach(this::publish);
    }

    @Override
    public void publish(DomainEvent<?> event) {
        if (event instanceof UserEvent) {
            publish((UserEvent) event);
        } else {
            throw new IllegalArgumentException(String.format("Event must be a UserEvent: %s", event.getClass().getName()));
        }
    }

    @Override
    public void publish(UserEvent event) {
        try {
            // Create enriched event with metadata (immutable copy)
            UserEvent enrichedEvent = enrichEventWithMetadata(event);

            String key = enrichedEvent.getAggregateId();
            kafkaTemplate.send(USER_EVENTS_TOPIC, key, enrichedEvent);
            log.debug("Published user event: {} with key: {} [correlationId: {}]", enrichedEvent.getClass().getSimpleName(), key,
                    enrichedEvent.getMetadata() != null ? enrichedEvent.getMetadata().getCorrelationId() : "none");
        } catch (Exception e) {
            log.error("Failed to publish user event: {}", event.getClass().getSimpleName(), e);
            throw new RuntimeException("Failed to publish user event", e);
        }
    }

    /**
     * Enriches event with metadata by creating an immutable copy. Uses event-specific logic to create enriched copies.
     *
     * @param event The original event
     * @return Enriched event with metadata, or original if enrichment fails
     */
    private UserEvent enrichEventWithMetadata(UserEvent event) {
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

        // Extract userId from aggregateId (now a String)
        UserId userId = UserId.of(event.getAggregateId());

        // Create enriched copy based on event type
        if (event instanceof UserCreatedEvent) {
            UserCreatedEvent userCreatedEvent = (UserCreatedEvent) event;
            return new UserCreatedEvent(userId, userCreatedEvent.getTenantId(), userCreatedEvent.getUsername(), userCreatedEvent.getEmail(), userCreatedEvent.getStatus(),
                    metadata);
        } else if (event instanceof UserUpdatedEvent) {
            UserUpdatedEvent userUpdatedEvent = (UserUpdatedEvent) event;
            return new UserUpdatedEvent(userId, userUpdatedEvent.getTenantId(), userUpdatedEvent.getStatus(), userUpdatedEvent.getDescription(), metadata);
        } else if (event instanceof UserDeactivatedEvent) {
            UserDeactivatedEvent userDeactivatedEvent = (UserDeactivatedEvent) event;
            return new UserDeactivatedEvent(userId, userDeactivatedEvent.getTenantId(), metadata);
        } else if (event instanceof UserRoleAssignedEvent) {
            UserRoleAssignedEvent userRoleAssignedEvent = (UserRoleAssignedEvent) event;
            return new UserRoleAssignedEvent(userId, userRoleAssignedEvent.getTenantId(), userRoleAssignedEvent.getRoleName(), metadata);
        } else if (event instanceof UserRoleRemovedEvent) {
            UserRoleRemovedEvent userRoleRemovedEvent = (UserRoleRemovedEvent) event;
            return new UserRoleRemovedEvent(userId, userRoleRemovedEvent.getTenantId(), userRoleRemovedEvent.getRoleName(), metadata);
        }

        // Unknown event type, return original
        log.warn("Unknown user event type: {}. Event will be published without metadata.", event.getClass().getName());
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

