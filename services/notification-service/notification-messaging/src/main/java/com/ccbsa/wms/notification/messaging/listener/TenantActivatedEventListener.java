package com.ccbsa.wms.notification.messaging.listener;

import java.util.Map;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.valueobject.EmailAddress;
import com.ccbsa.common.domain.valueobject.Message;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.Title;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.notification.application.service.command.CreateNotificationCommandHandler;
import com.ccbsa.wms.notification.application.service.command.dto.CreateNotificationCommand;
import com.ccbsa.wms.notification.application.service.port.service.TenantServicePort;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event Listener: TenantActivatedEventListener
 * <p>
 * Listens to TenantActivatedEvent and creates tenant activation notification.
 * <p>
 * Uses local DTO to avoid tight coupling with tenant-service domain classes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantActivatedEventListener {
    private final CreateNotificationCommandHandler createNotificationCommandHandler;
    private final TenantServicePort tenantServicePort;

    @KafkaListener(topics = "tenant-events", groupId = "notification-service", containerFactory = "externalEventKafkaListenerContainerFactory")
    public void handle(@Payload Map<String, Object> eventData, @Header(value = "__TypeId__", required = false) String eventType,
                       @Header(value = KafkaHeaders.RECEIVED_TOPIC) String topic, Acknowledgment acknowledgment) {
        log.info("Received event on topic {}: eventData keys={}, headerType={}, @class={}", topic, eventData.keySet(), eventType, eventData.get("@class"));
        try {
            // Extract and set correlation ID from event metadata for traceability
            extractAndSetCorrelationId(eventData);

            // Detect event type from payload (aggregateType field) or header
            String detectedEventType = detectEventType(eventData, eventType);
            log.info("Event type detection: detectedType={}, headerType={}, eventDataKeys={}, aggregateType={}, @class={}", detectedEventType, eventType, eventData.keySet(),
                    eventData.get("aggregateType"), eventData.get("@class"));
            if (!isTenantActivatedEvent(detectedEventType)) {
                log.debug("Skipping event - not TenantActivatedEvent: detectedType={}, headerType={}, @class={}", detectedEventType, eventType, eventData.get("@class"));
                acknowledgment.acknowledge();
                return;
            }

            // Extract tenantId from event data
            // aggregateId in DomainEvent is now a String containing the tenant ID
            String tenantIdString = extractTenantIdFromEvent(eventData);
            TenantId tenantId = TenantId.of(tenantIdString);

            log.info("Received TenantActivatedEvent: tenantId={}, eventId={}, eventDataKeys={}", tenantId.getValue(), extractEventId(eventData), eventData.keySet());

            // Set tenant context for multi-tenant schema resolution
            TenantContext.setTenantId(tenantId);
            try {
                // Get tenant email from tenant-service
                EmailAddress tenantEmail = tenantServicePort.getTenantEmail(tenantId);

                // Create tenant activation notification
                // Use system user ID (00000000-0000-0000-0000-000000000000) for tenant notifications since they don't target a specific user
                CreateNotificationCommand command =
                        CreateNotificationCommand.builder().tenantId(tenantId).recipientUserId(UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000000")))
                                .recipientEmail(tenantEmail).title(Title.of("Tenant Activated"))
                                .message(Message.of("Your tenant account has been activated. You can now access the Warehouse Management System."))
                                .type(NotificationType.TENANT_ACTIVATED).build();

                createNotificationCommandHandler.handle(command);

                log.info("Created tenant activation notification for tenant: tenantId={}, email={}", tenantId.getValue(), tenantEmail.getValue());

                // Acknowledge message
                acknowledgment.acknowledge();
            } finally {
                // Clear tenant context to prevent memory leaks
                TenantContext.clear();
            }
        } catch (IllegalArgumentException e) {
            // Invalid event format - acknowledge to skip (don't retry malformed events)
            log.error("Invalid event format for TenantActivatedEvent: eventData={}, error={}", eventData, e.getMessage(), e);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process TenantActivatedEvent: eventData={}, error={}", eventData, e.getMessage(), e);
            // Don't acknowledge - will retry for transient failures
            throw new RuntimeException("Failed to process TenantActivatedEvent", e);
        } finally {
            // Clear correlation context after processing
            CorrelationContext.clear();
        }
    }

    /**
     * Extracts correlation ID from event metadata and sets it in CorrelationContext. This enables traceability through event chains.
     *
     * @param eventData the event data map
     */
    private void extractAndSetCorrelationId(Map<String, Object> eventData) {
        try {
            Object metadataObj = eventData.get("metadata");
            if (metadataObj instanceof Map) {
                @SuppressWarnings("unchecked") Map<String, Object> metadata = (Map<String, Object>) metadataObj;
                Object correlationIdObj = metadata.get("correlationId");
                if (correlationIdObj != null) {
                    String correlationId = correlationIdObj.toString();
                    CorrelationContext.setCorrelationId(correlationId);
                    log.debug("Set correlation ID from event metadata: {}", correlationId);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract correlation ID from event metadata: {}", e.getMessage());
            // Continue processing even if correlation ID extraction fails
        }
    }

    /**
     * Detects event type from payload or header.
     * <p>
     * Since type information is disabled in serialization, we detect event type by checking for event-specific fields. TenantActivatedEvent has no additional fields beyond base
     * DomainEvent, so we check @class field first (most reliable),
     * then event-specific fields.
     *
     * @param eventData  Event data map
     * @param headerType Event type from header (may be null)
     * @return Detected event type
     */
    private String detectEventType(Map<String, Object> eventData, String headerType) {
        // Check @class field first (most reliable source of event type)
        Object classObj = eventData.get("@class");
        if (classObj instanceof String) {
            String className = (String) classObj;
            if (className.contains(".")) {
                String simpleName = className.substring(className.lastIndexOf('.') + 1);
                log.debug("Detected event type from @class field: {}", simpleName);
                return simpleName;
            }
            log.debug("Detected event type from @class field: {}", className);
            return className;
        }

        // Check header if @class is not available
        if (headerType != null) {
            String simpleName = headerType.contains(".") ? headerType.substring(headerType.lastIndexOf('.') + 1) : headerType;
            log.debug("Detected event type from header: {}", simpleName);
            return simpleName;
        }

        // Check for TenantCreatedEvent-specific fields (has name and status)
        if (eventData.containsKey("name") && eventData.containsKey("status")) {
            return "TenantCreatedEvent"; // Not our event, will be skipped
        }

        // Check for TenantSchemaCreatedEvent-specific field (has schemaName)
        if (eventData.containsKey("schemaName")) {
            return "TenantSchemaCreatedEvent"; // Not our event, will be skipped
        }

        // For events without unique fields, check aggregateType
        Object aggregateType = eventData.get("aggregateType");
        if ("Tenant".equals(aggregateType)) {
            // This could be TenantActivatedEvent, TenantDeactivatedEvent, or TenantSuspendedEvent
            // Without @class or header, default to TenantActivatedEvent for this listener
            // The isTenantActivatedEvent check will determine if we should process it
            log.debug("No @class or header found, defaulting to TenantActivatedEvent for aggregateType=Tenant");
            return "TenantActivatedEvent";
        }

        return "Unknown";
    }

    /**
     * Checks if the event is a TenantActivatedEvent.
     *
     * @param eventType Event type to check
     * @return true if this is a TenantActivatedEvent
     */
    private boolean isTenantActivatedEvent(String eventType) {
        return eventType != null && eventType.contains("TenantActivatedEvent");
    }

    /**
     * Extracts tenantId from event data.
     * <p>
     * Extracts from the tenantId field in TenantEvent, or falls back to aggregateId (which now contains the tenant ID as a String).
     *
     * @param eventData Event data map
     * @return String tenant ID
     * @throws IllegalArgumentException if tenantId is missing or invalid
     */
    private String extractTenantIdFromEvent(Map<String, Object> eventData) {
        // Try tenantId field first (from TenantEvent.getTenantId())
        Object tenantIdObj = eventData.get("tenantId");
        if (tenantIdObj != null) {
            if (tenantIdObj instanceof String) {
                return (String) tenantIdObj;
            }
            // Handle value object serialization (Map with "value" field)
            if (tenantIdObj instanceof Map) {
                @SuppressWarnings("unchecked") Map<String, Object> idMap = (Map<String, Object>) tenantIdObj;
                Object valueObj = idMap.get("value");
                if (valueObj != null) {
                    return valueObj.toString();
                }
            }
            return tenantIdObj.toString();
        }

        // Fallback to aggregateId (which now contains the tenant ID as String)
        Object aggregateIdObj = eventData.get("aggregateId");
        if (aggregateIdObj != null) {
            return aggregateIdObj.toString();
        }

        throw new IllegalArgumentException(String.format("tenantId is required but missing in event. Available keys: %s", eventData.keySet()));
    }

    /**
     * Extracts eventId from event data for logging.
     *
     * @param eventData Event data map
     * @return Event ID as string, or "unknown" if not found
     */
    private String extractEventId(Map<String, Object> eventData) {
        Object eventIdObj = eventData.get("eventId");
        if (eventIdObj == null) {
            return "unknown";
        }
        return eventIdObj.toString();
    }
}

