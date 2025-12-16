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
import com.ccbsa.common.domain.valueobject.Message;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.Title;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.notification.application.service.command.CreateNotificationCommandHandler;
import com.ccbsa.wms.notification.application.service.command.dto.CreateNotificationCommand;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;

/**
 * Event Listener: UserUpdatedEventListener
 * <p>
 * Listens to UserUpdatedEvent and creates profile update notification.
 * <p>
 * Uses local Map deserialization to avoid tight coupling with user-service domain classes.
 */
@Component
public class UserUpdatedEventListener {
    private static final Logger logger = LoggerFactory.getLogger(UserUpdatedEventListener.class);

    private final CreateNotificationCommandHandler createNotificationCommandHandler;

    public UserUpdatedEventListener(CreateNotificationCommandHandler createNotificationCommandHandler) {
        this.createNotificationCommandHandler = createNotificationCommandHandler;
    }

    @KafkaListener(
            topics = "user-events",
            groupId = "notification-service",
            containerFactory = "externalEventKafkaListenerContainerFactory"
    )
    public void handle(@Payload Map<String, Object> eventData,
                       @Header(value = "__TypeId__", required = false) String eventType,
                       @Header(value = KafkaHeaders.RECEIVED_TOPIC) String topic,
                       Acknowledgment acknowledgment) {
        try {
            // Extract and set correlation ID from event metadata for traceability
            extractAndSetCorrelationId(eventData);

            // Detect event type from payload (aggregateType field) or header
            String detectedEventType = detectEventType(eventData, eventType);
            if (!isUserUpdatedEvent(detectedEventType)) {
                logger.debug("Skipping event - not UserUpdatedEvent: detectedType={}, headerType={}",
                        detectedEventType, eventType);
                acknowledgment.acknowledge();
                return;
            }

            // Extract and validate data from event (avoids dependency on user-service domain classes)
            String aggregateId = extractAggregateId(eventData);
            TenantId tenantId = extractTenantId(eventData);
            EmailAddress recipientEmail = extractEmail(eventData);
            String description = extractDescription(eventData);

            logger.info("Received UserUpdatedEvent: userId={}, tenantId={}, email={}, eventId={}, eventDataKeys={}",
                    aggregateId, tenantId.getValue(), recipientEmail.getValue(), extractEventId(eventData), eventData.keySet());

            // Set tenant context for multi-tenant schema resolution
            TenantContext.setTenantId(tenantId);
            try {
                // Determine notification type based on event description
                NotificationType notificationType;
                String title;
                String message;

                if (description != null && description.contains("User activated")) {
                    notificationType = NotificationType.USER_ACTIVATED;
                    title = "Account Activated";
                    message = "Your account has been activated. You can now access the Warehouse Management System.";
                } else if (description != null && description.contains("User suspended")) {
                    notificationType = NotificationType.USER_SUSPENDED;
                    title = "Account Suspended";
                    message = "Your account has been suspended. Please contact your administrator for more information.";
                } else {
                    // Default to USER_UPDATED for other profile updates
                    notificationType = NotificationType.USER_UPDATED;
                    title = "Profile Updated";
                    message = description != null ? description : "Your profile has been updated.";
                }

                // Create notification
                CreateNotificationCommand command = CreateNotificationCommand.builder()
                        .tenantId(tenantId)
                        .recipientUserId(UserId.of(aggregateId))
                        .recipientEmail(recipientEmail)
                        .title(Title.of(title))
                        .message(Message.of(message))
                        .type(notificationType)
                        .build();

                createNotificationCommandHandler.handle(command);

                logger.info("Created {} notification for user: userId={}, email={}", notificationType, aggregateId, recipientEmail.getValue());

                // Acknowledge message
                acknowledgment.acknowledge();
            } finally {
                // Clear tenant context to prevent memory leaks
                TenantContext.clear();
            }
        } catch (IllegalArgumentException e) {
            // Invalid event format - acknowledge to skip (don't retry malformed events)
            logger.error("Invalid event format for UserUpdatedEvent: eventData={}, error={}",
                    eventData, e.getMessage(), e);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Failed to process UserUpdatedEvent: eventData={}, error={}",
                    eventData, e.getMessage(), e);
            // Don't acknowledge - will retry for transient failures
            throw new RuntimeException("Failed to process UserUpdatedEvent", e);
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
     * for @class field first (most reliable), then header, then event-specific fields.
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

        // Check for UserUpdatedEvent-specific fields (has description)
        if (eventData.containsKey("description")) {
            return "UserUpdatedEvent";
        }

        return "Unknown";
    }

    /**
     * Checks if the event is a UserUpdatedEvent.
     */
    private boolean isUserUpdatedEvent(String eventType) {
        return eventType != null && eventType.contains("UserUpdatedEvent");
    }

    /**
     * Extracts and validates aggregateId from event data.
     *
     * @param eventData Event data map
     * @return String aggregate ID
     * @throws IllegalArgumentException if aggregateId is missing or invalid
     */
    private String extractAggregateId(Map<String, Object> eventData) {
        Object aggregateIdObj = eventData.get("aggregateId");
        if (aggregateIdObj == null) {
            throw new IllegalArgumentException("aggregateId is required but missing in event");
        }

        // aggregateId is now a String in DomainEvent
        return aggregateIdObj.toString();
    }

    /**
     * Extracts and validates tenantId from event data.
     * Handles both simple string serialization and value object serialization.
     *
     * @param eventData Event data map
     * @return TenantId
     * @throws IllegalArgumentException if tenantId is missing or invalid
     */
    private TenantId extractTenantId(Map<String, Object> eventData) {
        Object tenantIdObj = eventData.get("tenantId");
        if (tenantIdObj == null) {
            throw new IllegalArgumentException(String.format("tenantId is required but missing in event. Available keys: %s", eventData.keySet()));
        }

        // Handle value object serialization (Map with "value" field)
        if (tenantIdObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> tenantIdMap = (Map<String, Object>) tenantIdObj;
            Object valueObj = tenantIdMap.get("value");
            if (valueObj != null) {
                return TenantId.of(valueObj.toString());
            }
        }

        // Handle simple string serialization
        return TenantId.of(tenantIdObj.toString());
    }

    /**
     * Extracts email address from event data.
     * Handles both direct email field and nested emailAddress object.
     */
    private EmailAddress extractEmail(Map<String, Object> eventData) {
        // Try direct email field first
        Object emailObj = eventData.get("email");
        if (emailObj == null) {
            // Try emailAddress field
            emailObj = eventData.get("emailAddress");
        }
        if (emailObj == null) {
            throw new IllegalArgumentException("email or emailAddress is required but missing in event");
        }

        // Handle nested object with value field (EmailAddress value object)
        if (emailObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> emailMap = (Map<String, Object>) emailObj;
            Object valueObj = emailMap.get("value");
            if (valueObj != null) {
                return EmailAddress.of(valueObj.toString());
            }
        }

        // Handle direct string value
        return EmailAddress.of(emailObj.toString());
    }

    /**
     * Extracts description from event data.
     * Handles both direct description field and nested description object.
     */
    private String extractDescription(Map<String, Object> eventData) {
        Object descriptionObj = eventData.get("description");
        if (descriptionObj == null) {
            return null;
        }

        // Handle nested object with value field (Description value object)
        if (descriptionObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> descriptionMap = (Map<String, Object>) descriptionObj;
            Object valueObj = descriptionMap.get("value");
            if (valueObj != null) {
                return valueObj.toString();
            }
        }

        // Handle direct string value
        return descriptionObj.toString();
    }

    /**
     * Extracts eventId from event data for logging.
     */
    private String extractEventId(Map<String, Object> eventData) {
        Object eventIdObj = eventData.get("eventId");
        if (eventIdObj == null) {
            return "unknown";
        }
        return eventIdObj.toString();
    }
}
