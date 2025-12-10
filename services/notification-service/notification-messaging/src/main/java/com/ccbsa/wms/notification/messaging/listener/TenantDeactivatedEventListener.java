package com.ccbsa.wms.notification.messaging.listener;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.valueobject.EmailAddress;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.notification.application.service.command.CreateNotificationCommandHandler;
import com.ccbsa.wms.notification.application.service.command.dto.CreateNotificationCommand;
import com.ccbsa.wms.notification.application.service.port.service.TenantServicePort;
import com.ccbsa.wms.notification.domain.core.valueobject.Message;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;
import com.ccbsa.wms.notification.domain.core.valueobject.Title;

/**
 * Event Listener: TenantDeactivatedEventListener
 * <p>
 * Listens to TenantDeactivatedEvent and creates tenant deactivation notification.
 * <p>
 * Uses local DTO to avoid tight coupling with tenant-service domain classes.
 */
@Component
public class TenantDeactivatedEventListener {
    private static final Logger logger = LoggerFactory.getLogger(TenantDeactivatedEventListener.class);

    private final CreateNotificationCommandHandler createNotificationCommandHandler;
    private final TenantServicePort tenantServicePort;

    public TenantDeactivatedEventListener(
            CreateNotificationCommandHandler createNotificationCommandHandler,
            TenantServicePort tenantServicePort) {
        this.createNotificationCommandHandler = createNotificationCommandHandler;
        this.tenantServicePort = tenantServicePort;
    }

    @KafkaListener(
            topics = "tenant-events",
            groupId = "notification-service",
            containerFactory = "externalEventKafkaListenerContainerFactory"
    )
    public void handle(@Payload Map<String, Object> eventData,
                       @Header(value = "__TypeId__", required = false) String eventType,
                       @Header(value = KafkaHeaders.RECEIVED_TOPIC) String topic,
                       Acknowledgment acknowledgment) {
        logger.info("Received event on topic {}: eventData keys={}, headerType={}, @class={}",
                topic, eventData.keySet(), eventType, eventData.get("@class"));
        try {
            // Extract and set correlation ID from event metadata for traceability
            extractAndSetCorrelationId(eventData);

            // Detect event type from payload (aggregateType field) or header
            String detectedEventType = detectEventType(eventData, eventType);
            logger.info("Event type detection: detectedType={}, headerType={}, eventDataKeys={}, aggregateType={}",
                    detectedEventType, eventType, eventData.keySet(), eventData.get("aggregateType"));
            if (!isTenantDeactivatedEvent(detectedEventType)) {
                logger.debug("Skipping event - not TenantDeactivatedEvent: detectedType={}, headerType={}",
                        detectedEventType, eventType);
                acknowledgment.acknowledge();
                return;
            }

            // Extract tenantId from event data
            // aggregateId in DomainEvent is now a String containing the tenant ID
            String tenantIdString = extractTenantIdFromEvent(eventData);
            TenantId tenantId = TenantId.of(tenantIdString);

            logger.info("Received TenantDeactivatedEvent: tenantId={}, eventId={}, eventDataKeys={}",
                    tenantId.getValue(), extractEventId(eventData), eventData.keySet());

            // Set tenant context for multi-tenant schema resolution
            TenantContext.setTenantId(tenantId);
            try {
                // Get tenant email from tenant-service
                EmailAddress tenantEmail = tenantServicePort.getTenantEmail(tenantId);

                // Create tenant deactivation notification
                // Use tenantId as recipientUserId placeholder since tenant notifications don't target a specific user
                CreateNotificationCommand command = CreateNotificationCommand.builder()
                        .tenantId(tenantId)
                        .recipientUserId(UserId.of(tenantId.getValue()))
                        .recipientEmail(tenantEmail)
                        .title(Title.of("Tenant Deactivated"))
                        .message(Message.of("Your tenant account has been deactivated. Please contact your administrator for more information."))
                        .type(NotificationType.TENANT_DEACTIVATED)
                        .build();

                createNotificationCommandHandler.handle(command);

                logger.info("Created tenant deactivation notification for tenant: tenantId={}, email={}",
                        tenantId.getValue(), tenantEmail.getValue());

                // Acknowledge message
                acknowledgment.acknowledge();
            } finally {
                // Clear tenant context to prevent memory leaks
                TenantContext.clear();
            }
        } catch (IllegalArgumentException e) {
            // Invalid event format - acknowledge to skip (don't retry malformed events)
            logger.error("Invalid event format for TenantDeactivatedEvent: eventData={}, error={}",
                    eventData, e.getMessage(), e);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Failed to process TenantDeactivatedEvent: eventData={}, error={}",
                    eventData, e.getMessage(), e);
            // Don't acknowledge - will retry for transient failures
            throw new RuntimeException("Failed to process TenantDeactivatedEvent", e);
        } finally {
            // Clear correlation context after processing
            CorrelationContext.clear();
        }
    }

    /**
     * Extracts correlation ID from event metadata and sets it in CorrelationContext.
     * This enables traceability through event chains.
     *
     * @param eventData the event data map
     */
    private void extractAndSetCorrelationId(Map<String, Object> eventData) {
        try {
            Object metadataObj = eventData.get("metadata");
            if (metadataObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) metadataObj;
                Object correlationIdObj = metadata.get("correlationId");
                if (correlationIdObj != null) {
                    String correlationId = correlationIdObj.toString();
                    CorrelationContext.setCorrelationId(correlationId);
                    logger.debug("Set correlation ID from event metadata: {}", correlationId);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract correlation ID from event metadata: {}", e.getMessage());
            // Continue processing even if correlation ID extraction fails
        }
    }

    /**
     * Detects event type from payload or header.
     * <p>
     * Since type information is disabled in serialization, we detect event type by checking
     * for event-specific fields. TenantDeactivatedEvent has no additional fields beyond base DomainEvent,
     * so we check @class field first (most reliable), then event-specific fields.
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
                logger.debug("Detected event type from @class field: {}", simpleName);
                return simpleName;
            }
            logger.debug("Detected event type from @class field: {}", className);
            return className;
        }

        // Check header if @class is not available
        if (headerType != null) {
            String simpleName = headerType.contains(".")
                    ? headerType.substring(headerType.lastIndexOf('.') + 1)
                    : headerType;
            logger.debug("Detected event type from header: {}", simpleName);
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
            // Without @class or header, default to TenantDeactivatedEvent for this listener
            // The isTenantDeactivatedEvent check will determine if we should process it
            logger.debug("No @class or header found, defaulting to TenantDeactivatedEvent for aggregateType=Tenant");
            return "TenantDeactivatedEvent";
        }

        return "Unknown";
    }

    /**
     * Checks if the event is a TenantDeactivatedEvent.
     *
     * @param eventType Event type to check
     * @return true if this is a TenantDeactivatedEvent
     */
    private boolean isTenantDeactivatedEvent(String eventType) {
        return eventType != null && eventType.contains("TenantDeactivatedEvent");
    }

    /**
     * Extracts tenantId from event data.
     * <p>
     * Extracts from the tenantId field in TenantEvent, or falls back to aggregateId
     * (which now contains the tenant ID as a String).
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
                @SuppressWarnings("unchecked")
                Map<String, Object> idMap = (Map<String, Object>) tenantIdObj;
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

