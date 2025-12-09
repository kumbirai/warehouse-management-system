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
import com.ccbsa.wms.notification.domain.core.valueobject.Message;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;
import com.ccbsa.wms.notification.domain.core.valueobject.Title;

/**
 * Event Listener: UserRoleRemovedEventListener
 * <p>
 * Listens to UserRoleRemovedEvent and creates role removal notification.
 * <p>
 * Uses local Map deserialization to avoid tight coupling with user-service domain classes.
 */
@Component
public class UserRoleRemovedEventListener {
    private static final Logger logger = LoggerFactory.getLogger(UserRoleRemovedEventListener.class);

    private final CreateNotificationCommandHandler createNotificationCommandHandler;

    public UserRoleRemovedEventListener(CreateNotificationCommandHandler createNotificationCommandHandler) {
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
            if (!isUserRoleRemovedEvent(detectedEventType)) {
                logger.debug("Skipping event - not UserRoleRemovedEvent: detectedType={}, headerType={}",
                        detectedEventType, eventType);
                acknowledgment.acknowledge();
                return;
            }

            // Extract and validate data from event (avoids dependency on user-service domain classes)
            String aggregateId = extractAggregateId(eventData);
            TenantId tenantId = extractTenantId(eventData);
            EmailAddress recipientEmail = extractEmail(eventData);
            String roleName = extractRoleName(eventData);

            logger.info("Received UserRoleRemovedEvent: userId={}, tenantId={}, email={}, role={}, eventId={}, eventDataKeys={}",
                    aggregateId, tenantId.getValue(), recipientEmail.getValue(), roleName, extractEventId(eventData), eventData.keySet());

            // Set tenant context for multi-tenant schema resolution
            TenantContext.setTenantId(tenantId);
            try {
                // Create role removal notification
                CreateNotificationCommand command = CreateNotificationCommand.builder()
                        .tenantId(tenantId)
                        .recipientUserId(UserId.of(aggregateId))
                        .recipientEmail(recipientEmail)
                        .title(Title.of("Role Removed"))
                        .message(Message.of(String.format("The role '%s' has been removed from your account.", roleName)))
                        .type(NotificationType.USER_ROLE_REMOVED)
                        .build();

                createNotificationCommandHandler.handle(command);

                logger.info("Created role removal notification for user: userId={}, email={}, role={}",
                        aggregateId, recipientEmail.getValue(), roleName);

                // Acknowledge message
                acknowledgment.acknowledge();
            } finally {
                // Clear tenant context to prevent memory leaks
                TenantContext.clear();
            }
        } catch (IllegalArgumentException e) {
            // Invalid event format - acknowledge to skip (don't retry malformed events)
            logger.error("Invalid event format for UserRoleRemovedEvent: eventData={}, error={}",
                    eventData, e.getMessage(), e);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Failed to process UserRoleRemovedEvent: eventData={}, error={}",
                    eventData, e.getMessage(), e);
            // Don't acknowledge - will retry for transient failures
            throw new RuntimeException("Failed to process UserRoleRemovedEvent", e);
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
     */
    private String detectEventType(Map<String, Object> eventData, String headerType) {
        Object aggregateTypeObj = eventData.get("aggregateType");
        if (aggregateTypeObj instanceof String) {
            return (String) aggregateTypeObj;
        }
        return headerType != null ? headerType : "Unknown";
    }

    /**
     * Checks if the event is a UserRoleRemovedEvent.
     */
    private boolean isUserRoleRemovedEvent(String eventType) {
        return eventType != null && eventType.contains("UserRoleRemovedEvent");
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
            throw new IllegalArgumentException("tenantId is required but missing in event. Available keys: " + eventData.keySet());
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
     * Extracts role name from event data.
     */
    private String extractRoleName(Map<String, Object> eventData) {
        Object roleNameObj = eventData.get("roleName");
        if (roleNameObj == null) {
            throw new IllegalArgumentException("roleName is required but missing in event");
        }
        return roleNameObj.toString();
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
