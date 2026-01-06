package com.ccbsa.wms.notification.messaging.listener;

import java.util.Map;

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
import com.ccbsa.wms.notification.application.service.port.service.UserServicePort;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event Listener: UserRoleAssignedEventListener
 * <p>
 * Listens to UserRoleAssignedEvent and creates role assignment notification.
 * <p>
 * Uses local Map deserialization to avoid tight coupling with user-service domain classes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRoleAssignedEventListener {
    private final CreateNotificationCommandHandler createNotificationCommandHandler;
    private final UserServicePort userServicePort;

    @KafkaListener(topics = "user-events", groupId = "notification-service", containerFactory = "externalEventKafkaListenerContainerFactory")
    public void handle(@Payload Map<String, Object> eventData, @Header(value = "__TypeId__", required = false) String eventType,
                       @Header(value = KafkaHeaders.RECEIVED_TOPIC) String topic, Acknowledgment acknowledgment) {
        try {
            // Extract and set correlation ID from event metadata for traceability
            extractAndSetCorrelationId(eventData);

            // Detect event type from payload (aggregateType field) or header
            String detectedEventType = detectEventType(eventData, eventType);
            if (!isUserRoleAssignedEvent(detectedEventType)) {
                log.debug("Skipping event - not UserRoleAssignedEvent: detectedType={}, headerType={}", detectedEventType, eventType);
                acknowledgment.acknowledge();
                return;
            }

            // Extract and validate data from event (avoids dependency on user-service domain classes)
            String aggregateId = extractAggregateId(eventData);
            TenantId tenantId = extractTenantId(eventData);
            UserId userId = UserId.of(aggregateId);
            String roleName = extractRoleName(eventData);

            // Log event receipt
            log.info("Received UserRoleAssignedEvent: userId={}, tenantId={}, role={}, eventId={}, eventDataKeys={}", aggregateId, tenantId.getValue(), roleName,
                    extractEventId(eventData), eventData.keySet());

            // Set tenant context for multi-tenant schema resolution BEFORE making service calls
            TenantContext.setTenantId(tenantId);
            try {
                // Extract email after TenantContext is set (needed for service-to-service calls)
                java.util.Optional<EmailAddress> recipientEmailOpt = extractEmail(eventData, userId);
                String emailStr = recipientEmailOpt.map(EmailAddress::getValue).orElse("(not set)");
                // Create role assignment notification (email is optional - handler supports null email)
                CreateNotificationCommand.Builder commandBuilder =
                        CreateNotificationCommand.builder().tenantId(tenantId).recipientUserId(UserId.of(aggregateId)).title(Title.of("Role Assigned"))
                                .message(Message.of(String.format("The role '%s' has been assigned to your account.", roleName))).type(NotificationType.USER_ROLE_ASSIGNED);

                // Set email if available (optional)
                recipientEmailOpt.ifPresent(commandBuilder::recipientEmail);

                CreateNotificationCommand command = commandBuilder.build();
                createNotificationCommandHandler.handle(command);

                log.info("Created role assignment notification for user: userId={}, email={}, role={}", aggregateId, emailStr, roleName);

                // Acknowledge message
                acknowledgment.acknowledge();
            } finally {
                // Clear tenant context to prevent memory leaks
                TenantContext.clear();
            }
        } catch (IllegalArgumentException e) {
            // Invalid event format - acknowledge to skip (don't retry malformed events)
            log.error("Invalid event format for UserRoleAssignedEvent: eventData={}, error={}", eventData, e.getMessage(), e);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process UserRoleAssignedEvent: eventData={}, error={}", eventData, e.getMessage(), e);
            // Don't acknowledge - will retry for transient failures
            throw new RuntimeException("Failed to process UserRoleAssignedEvent", e);
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
     * Since type information is disabled in serialization, we detect event type by checking for @class field first (most reliable), then header, then event-specific fields.
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

        // Check for UserRoleAssignedEvent-specific fields (has roleName)
        if (eventData.containsKey("roleName")) {
            // Could be UserRoleAssignedEvent or UserRoleRemovedEvent
            // Default to UserRoleAssignedEvent for this listener
            return "UserRoleAssignedEvent";
        }

        return "Unknown";
    }

    /**
     * Checks if the event is a UserRoleAssignedEvent.
     */
    private boolean isUserRoleAssignedEvent(String eventType) {
        return eventType != null && eventType.contains("UserRoleAssignedEvent");
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
     * Extracts and validates tenantId from event data. Handles both simple string serialization and value object serialization.
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
            @SuppressWarnings("unchecked") Map<String, Object> tenantIdMap = (Map<String, Object>) tenantIdObj;
            Object valueObj = tenantIdMap.get("value");
            if (valueObj != null) {
                return TenantId.of(valueObj.toString());
            }
        }

        // Handle simple string serialization
        return TenantId.of(tenantIdObj.toString());
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

    /**
     * Extracts email address from event data. Handles both direct email field and nested emailAddress object.
     * Falls back to querying UserServicePort if email is not present in event.
     * <p>
     * Email is optional - returns empty Optional if email is not available in event or from user service.
     *
     * @param eventData Event data map
     * @param userId    User identifier for fallback query
     * @return Optional containing EmailAddress if available, empty if email not found
     */
    private java.util.Optional<EmailAddress> extractEmail(Map<String, Object> eventData, UserId userId) {
        // Try direct email field first
        Object emailObj = eventData.get("email");
        if (emailObj == null) {
            // Try emailAddress field
            emailObj = eventData.get("emailAddress");
        }

        // If email found in event, parse and return it
        if (emailObj != null) {
            // Handle nested object with value field (EmailAddress value object)
            if (emailObj instanceof Map) {
                @SuppressWarnings("unchecked") Map<String, Object> emailMap = (Map<String, Object>) emailObj;
                Object valueObj = emailMap.get("value");
                if (valueObj != null) {
                    return java.util.Optional.of(EmailAddress.of(valueObj.toString()));
                }
            }

            // Handle direct string value
            String emailStr = emailObj.toString();
            if (!emailStr.isEmpty()) {
                return java.util.Optional.of(EmailAddress.of(emailStr));
            }
        }

        // Fallback: Query user service to get email address
        // Note: tenantId is required for X-Tenant-Id header in service-to-service calls
        TenantId tenantId = extractTenantId(eventData);
        log.debug("Email not found in event, querying user-service: userId={}, tenantId={}", userId.getValue(), tenantId.getValue());
        try {
            java.util.Optional<EmailAddress> emailOpt = userServicePort.getUserEmail(userId, tenantId);
            if (emailOpt.isPresent()) {
                log.debug("Retrieved email from user-service: userId={}, email={}", userId.getValue(), emailOpt.get().getValue());
            } else {
                log.warn("User email not available from user-service: userId={}, tenantId={}", userId.getValue(), tenantId.getValue());
            }
            return emailOpt;
        } catch (Exception e) {
            log.error("Failed to retrieve email from user-service: userId={}, tenantId={}, error={}", userId.getValue(), tenantId.getValue(), e.getMessage(), e);
            // Return empty Optional instead of throwing - allows notification creation without email
            return java.util.Optional.empty();
        }
    }
}
