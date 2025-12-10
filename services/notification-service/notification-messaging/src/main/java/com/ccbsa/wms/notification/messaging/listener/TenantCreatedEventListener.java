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
 * Event Listener: TenantCreatedEventListener
 * <p>
 * Listens to TenantCreatedEvent and creates tenant creation notification.
 * <p>
 * Uses local DTO to avoid tight coupling with tenant-service domain classes.
 */
@Component
public class TenantCreatedEventListener {
    private static final Logger logger = LoggerFactory.getLogger(TenantCreatedEventListener.class);

    private final CreateNotificationCommandHandler createNotificationCommandHandler;
    private final TenantServicePort tenantServicePort;

    public TenantCreatedEventListener(
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

            // Detect event type from payload (class name or aggregateType field) or header
            String detectedEventType = detectEventType(eventData, eventType);
            logger.info("Event type detection: detectedType={}, headerType={}, eventDataKeys={}, aggregateType={}",
                    detectedEventType, eventType, eventData.keySet(), eventData.get("aggregateType"));
            if (!isTenantCreatedEvent(detectedEventType)) {
                logger.debug("Skipping event - not TenantCreatedEvent: detectedType={}, headerType={}",
                        detectedEventType, eventType);
                acknowledgment.acknowledge();
                return;
            }

            // Extract tenantId from event data
            // aggregateId in DomainEvent is now a String containing the tenant ID
            String tenantIdString = extractTenantIdFromEvent(eventData);
            TenantId tenantId = TenantId.of(tenantIdString);

            logger.info("Received TenantCreatedEvent: tenantId={}, eventId={}, eventDataKeys={}",
                    tenantId.getValue(), extractEventId(eventData), eventData.keySet());

            // Set tenant context for multi-tenant schema resolution
            TenantContext.setTenantId(tenantId);
            try {
                // Get tenant email from event payload or tenant-service
                EmailAddress tenantEmail = extractEmailFromEvent(eventData, tenantId);

                // Create tenant creation notification
                // Use tenantId as recipientUserId placeholder since tenant notifications don't target a specific user
                CreateNotificationCommand command = CreateNotificationCommand.builder()
                        .tenantId(tenantId)
                        .recipientUserId(UserId.of(tenantId.getValue()))
                        .recipientEmail(tenantEmail)
                        .title(Title.of("New Tenant Created"))
                        .message(Message.of("A new tenant account has been created. Please wait for activation."))
                        .type(NotificationType.TENANT_CREATED)
                        .build();

                createNotificationCommandHandler.handle(command);

                logger.info("Created tenant creation notification for tenant: tenantId={}, email={}",
                        tenantId.getValue(), tenantEmail.getValue());

                // Acknowledge message
                acknowledgment.acknowledge();
            } finally {
                // Clear tenant context to prevent memory leaks
                TenantContext.clear();
            }
        } catch (IllegalArgumentException e) {
            // Invalid event format - acknowledge to skip (don't retry malformed events)
            logger.error("Invalid event format for TenantCreatedEvent: eventData={}, error={}",
                    eventData, e.getMessage(), e);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Failed to process TenantCreatedEvent: eventData={}, error={}",
                    eventData, e.getMessage(), e);
            // Don't acknowledge - will retry for transient failures
            throw new RuntimeException("Failed to process TenantCreatedEvent", e);
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
     * for event-specific fields. TenantCreatedEvent has 'name', 'status', and optionally 'email' fields.
     *
     * @param eventData  Event data map
     * @param headerType Event type from header (may be null)
     * @return Detected event type
     */
    private String detectEventType(Map<String, Object> eventData, String headerType) {
        // Check for TenantCreatedEvent-specific fields (name, status, email)
        if (eventData.containsKey("name") && eventData.containsKey("status")) {
            return "TenantCreatedEvent";
        }

        // Try to get from @class field (if JsonSerializer includes this)
        Object classObj = eventData.get("@class");
        if (classObj instanceof String) {
            String className = (String) classObj;
            // Extract simple class name from FQCN
            if (className.contains(".")) {
                return className.substring(className.lastIndexOf('.') + 1);
            }
            return className;
        }

        // Fall back to header
        if (headerType != null) {
            // Extract simple class name from FQCN if present
            if (headerType.contains(".")) {
                return headerType.substring(headerType.lastIndexOf('.') + 1);
            }
            return headerType;
        }

        return "Unknown";
    }

    /**
     * Checks if the event is a TenantCreatedEvent.
     *
     * @param eventType Event type to check
     * @return true if this is a TenantCreatedEvent
     */
    private boolean isTenantCreatedEvent(String eventType) {
        return eventType != null && eventType.contains("TenantCreatedEvent");
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

    /**
     * Extracts email from event payload or fetches from tenant service.
     *
     * @param eventData Event data map
     * @param tenantId  Tenant ID
     * @return Email address
     */
    private EmailAddress extractEmailFromEvent(Map<String, Object> eventData, TenantId tenantId) {
        // Try to extract email from event payload first
        Object emailObj = eventData.get("email");
        if (emailObj != null) {
            if (emailObj instanceof Map) {
                // Email might be serialized as a value object with a "value" field
                @SuppressWarnings("unchecked")
                Map<String, Object> emailMap = (Map<String, Object>) emailObj;
                Object valueObj = emailMap.get("value");
                if (valueObj instanceof String) {
                    return EmailAddress.of((String) valueObj);
                }
            } else if (emailObj instanceof String) {
                return EmailAddress.of((String) emailObj);
            }
        }

        // Fallback to tenant service
        return tenantServicePort.getTenantEmail(tenantId);
    }
}

