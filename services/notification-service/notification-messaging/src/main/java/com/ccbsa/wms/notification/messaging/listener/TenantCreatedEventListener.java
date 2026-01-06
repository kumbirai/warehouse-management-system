package com.ccbsa.wms.notification.messaging.listener;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event Listener: TenantCreatedEventListener
 * <p>
 * Listens to TenantCreatedEvent and creates tenant creation notification.
 * <p>
 * Uses local DTO to avoid tight coupling with tenant-service domain classes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantCreatedEventListener {
    private final CreateNotificationCommandHandler createNotificationCommandHandler;
    private final TenantServicePort tenantServicePort;

    /**
     * Initializes the listener and logs registration status.
     * <p>
     * This method is called after dependency injection to verify the listener is properly registered.
     */
    @PostConstruct
    public void init() {
        log.info("TenantCreatedEventListener initialized and ready to consume TenantCreatedEvent from topic 'tenant-events' with groupId 'notification-service'");
    }

    @KafkaListener(topics = "tenant-events", groupId = "notification-service", containerFactory = "externalEventKafkaListenerContainerFactory")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handle(@Payload Map<String, Object> eventData, @Header(value = "__TypeId__", required = false) String eventType,
                       @Header(value = KafkaHeaders.RECEIVED_TOPIC) String topic, Acknowledgment acknowledgment) {
        log.info("Received event on topic {}: eventData keys={}, headerType={}, @class={}", topic, eventData.keySet(), eventType, eventData.get("@class"));
        try {
            // Extract and set correlation ID from event metadata for traceability
            extractAndSetCorrelationId(eventData);

            // Detect event type from payload (class name or aggregateType field) or header
            String detectedEventType = detectEventType(eventData, eventType);
            log.info("Event type detection: detectedType={}, headerType={}, eventDataKeys={}, aggregateType={}", detectedEventType, eventType, eventData.keySet(),
                    eventData.get("aggregateType"));
            if (!isTenantCreatedEvent(detectedEventType)) {
                log.debug("Skipping event - not TenantCreatedEvent: detectedType={}, headerType={}", detectedEventType, eventType);
                acknowledgment.acknowledge();
                return;
            }

            // Extract tenantId from event data
            // aggregateId in DomainEvent is now a String containing the tenant ID
            String tenantIdString = extractTenantIdFromEvent(eventData);
            TenantId tenantId = TenantId.of(tenantIdString);

            log.info("Received TenantCreatedEvent: tenantId={}, eventId={}, eventDataKeys={}", tenantId.getValue(), extractEventId(eventData), eventData.keySet());

            // Set tenant context for multi-tenant schema resolution
            TenantContext.setTenantId(tenantId);
            try {
                // Get tenant email from event payload or tenant-service (optional)
                Optional<EmailAddress> tenantEmailOpt = extractEmailFromEvent(eventData, tenantId);

                // Create tenant creation notification
                // Use system user ID (00000000-0000-0000-0000-000000000000) for tenant notifications since they don't target a specific user
                CreateNotificationCommand.Builder commandBuilder =
                        CreateNotificationCommand.builder().tenantId(tenantId).recipientUserId(UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000000")))
                                .title(Title.of("New Tenant Created")).message(Message.of("A new tenant account has been created. Please wait for activation."))
                                .type(NotificationType.TENANT_CREATED);

                // Set email if available
                tenantEmailOpt.ifPresent(commandBuilder::recipientEmail);

                CreateNotificationCommand command = commandBuilder.build();

                // Process notification creation in a separate transaction to ensure proper connection management
                // This ensures connections are properly released after each transaction, preventing connection leaks
                createNotificationInTransaction(command, tenantId);

                if (tenantEmailOpt.isPresent()) {
                    log.info("Created tenant creation notification for tenant: tenantId={}, email={}", tenantId.getValue(), tenantEmailOpt.get().getValue());
                } else {
                    log.warn("Created tenant creation notification for tenant without email: tenantId={}. Email delivery will be skipped.", tenantId.getValue());
                }

                // Acknowledge message
                acknowledgment.acknowledge();
            } finally {
                // Clear tenant context to prevent memory leaks
                TenantContext.clear();
            }
        } catch (IllegalArgumentException e) {
            // Invalid event format - acknowledge to skip (don't retry malformed events)
            log.error("Invalid event format for TenantCreatedEvent: eventData={}, error={}", eventData, e.getMessage(), e);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process TenantCreatedEvent: eventData={}, error={}", eventData, e.getMessage(), e);
            // Don't acknowledge - will retry for transient failures
            throw new RuntimeException("Failed to process TenantCreatedEvent", e);
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
     * Since type information is disabled in serialization, we detect event type by checking for event-specific fields. TenantCreatedEvent has 'name', 'status', and optionally
     * 'email' fields.
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

    /**
     * Extracts email from event payload or fetches from tenant service.
     * <p>
     * Email is optional for tenant creation notifications. If email is not available in the event payload
     * and tenant-service is not reachable, returns empty Optional. The notification will still be created
     * but email delivery will be skipped.
     *
     * @param eventData Event data map
     * @param tenantId  Tenant ID
     * @return Optional email address, empty if not available
     */
    private Optional<EmailAddress> extractEmailFromEvent(Map<String, Object> eventData, TenantId tenantId) {
        // Try to extract email from event payload first
        Object emailObj = eventData.get("email");
        if (emailObj != null) {
            // Handle null value (email field present but null)
            if (emailObj instanceof Map) {
                // Email might be serialized as a value object with a "value" field
                @SuppressWarnings("unchecked") Map<String, Object> emailMap = (Map<String, Object>) emailObj;
                Object valueObj = emailMap.get("value");
                if (valueObj instanceof String && !((String) valueObj).trim().isEmpty()) {
                    try {
                        return Optional.of(EmailAddress.of((String) valueObj));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid email format in event payload: tenantId={}, email={}", tenantId.getValue(), valueObj, e);
                    }
                }
            } else if (emailObj instanceof String && !((String) emailObj).trim().isEmpty()) {
                try {
                    return Optional.of(EmailAddress.of((String) emailObj));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid email format in event payload: tenantId={}, email={}", tenantId.getValue(), emailObj, e);
                }
            }
        }

        // Fallback to tenant service (only if email not found in event)
        try {
            EmailAddress email = tenantServicePort.getTenantEmail(tenantId);
            return Optional.of(email);
        } catch (Exception e) {
            // Log warning but don't fail - email is optional for tenant notifications
            log.warn("Failed to retrieve tenant email from tenant-service: tenantId={}, error={}. Notification will be created without email.", tenantId.getValue(),
                    e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Creates notification in a separate transaction to ensure proper connection management.
     * <p>
     * This method runs in its own transaction (REQUIRES_NEW), ensuring connections are properly released
     * after the transaction completes, preventing connection leaks.
     *
     * @param command  Create notification command
     * @param tenantId Tenant identifier
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void createNotificationInTransaction(CreateNotificationCommand command, TenantId tenantId) {
        // Ensure tenant context is set (may be cleared between method calls)
        TenantId currentTenantId = TenantContext.getTenantId();
        if (currentTenantId == null || !currentTenantId.equals(tenantId)) {
            log.warn("Tenant context missing or incorrect: expected={}, actual={}. Re-setting.", tenantId.getValue(),
                    currentTenantId != null ? currentTenantId.getValue() : "null");
            TenantContext.setTenantId(tenantId);
        }
        log.debug("Creating notification in transaction: tenantId={}", tenantId.getValue());

        createNotificationCommandHandler.handle(command);
    }
}

