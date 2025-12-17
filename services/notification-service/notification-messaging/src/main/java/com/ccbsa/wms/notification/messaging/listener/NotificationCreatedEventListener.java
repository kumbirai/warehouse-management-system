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
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.notification.application.service.command.SendNotificationCommandHandler;
import com.ccbsa.wms.notification.application.service.command.dto.SendNotificationCommand;
import com.ccbsa.wms.notification.application.service.exception.NotificationNotFoundException;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationChannel;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationId;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;

/**
 * Event Listener: NotificationCreatedEventListener
 * <p>
 * Listens to NotificationCreatedEvent and triggers notification delivery via appropriate channel. For MVP, defaults to EMAIL channel. Future: determine channel based on user
 * preferences or notification type.
 * <p>
 * Uses Map deserialization to avoid deserialization issues when type headers are not present.
 */
@Component
public class NotificationCreatedEventListener {
    private static final Logger logger = LoggerFactory.getLogger(NotificationCreatedEventListener.class);

    private final SendNotificationCommandHandler sendHandler;

    public NotificationCreatedEventListener(SendNotificationCommandHandler sendHandler) {
        this.sendHandler = sendHandler;
    }

    @KafkaListener(topics = "notification-events",
            groupId = "notification-service",
            containerFactory = "internalEventKafkaListenerContainerFactory")
    public void handle(
            @Payload Map<String, Object> eventData,
            @Header(value = "__TypeId__",
                    required = false) String eventType,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC) String topic, Acknowledgment acknowledgment) {
        try {
            // Extract and set correlation ID from event metadata for traceability
            extractAndSetCorrelationId(eventData);

            // Detect event type from payload or header
            String detectedEventType = detectEventType(eventData, eventType);
            if (!isNotificationCreatedEvent(detectedEventType)) {
                logger.debug("Skipping event - not NotificationCreatedEvent: detectedType={}, headerType={}", detectedEventType, eventType);
                acknowledgment.acknowledge();
                return;
            }

            // Extract and validate aggregateId from event data
            String aggregateId = extractAggregateId(eventData);
            NotificationId notificationId = NotificationId.of(aggregateId);

            // Extract tenant ID and set in TenantContext for multi-tenant schema resolution
            TenantId tenantId = extractTenantId(eventData);
            TenantContext.setTenantId(tenantId);

            // Extract notification type
            NotificationType notificationType = extractNotificationType(eventData);

            logger.info("Received NotificationCreatedEvent: notificationId={}, tenantId={}, type={}, eventDataKeys={}", notificationId, tenantId.getValue(), notificationType,
                    eventData.keySet());

            // Determine delivery channel
            // For MVP: default to EMAIL
            // Future: check user preferences, notification type, etc.
            NotificationChannel channel = determineChannel(notificationType);

            // Create send command
            SendNotificationCommand command = SendNotificationCommand.builder()
                    .notificationId(notificationId)
                    .channel(channel)
                    .build();

            // Send notification via appropriate channel with retry logic
            // Retry is needed because events may be consumed before the transaction that created
            // the notification commits (eventual consistency)
            // Pass tenantId to ensure it's available during retries
            sendNotificationWithRetry(command, notificationId, channel, tenantId);

            logger.info("Notification delivery initiated: notificationId={}, channel={}", notificationId, channel);

            // Acknowledge message
            acknowledgment.acknowledge();

        } catch (IllegalArgumentException e) {
            // Invalid event format - acknowledge to skip (don't retry malformed events)
            logger.error("Invalid event format for NotificationCreatedEvent: eventData={}, error={}", eventData, e.getMessage(), e);
            acknowledgment.acknowledge();
        } catch (NotificationNotFoundException e) {
            // Notification not found after retries - likely race condition where event consumed before transaction commits
            // Log warning - Kafka retry mechanism will handle if notification appears later
            String aggregateId = extractAggregateId(eventData);
            logger.warn("Notification not found when processing NotificationCreatedEvent: notificationId={}, error={}. "
                            + "This may be a race condition. Event will be retried by Kafka if notification appears.", aggregateId,
                    e.getMessage());
            // Don't acknowledge - let Kafka retry mechanism handle it
            throw new RuntimeException("Notification not found - will retry", e);
        } catch (Exception e) {
            logger.error("Failed to process NotificationCreatedEvent: eventData={}, error={}", eventData, e.getMessage(), e);
            // Don't acknowledge - will retry for transient failures
            throw new RuntimeException("Failed to process NotificationCreatedEvent", e);
        } finally {
            // Clear correlation and tenant context after processing
            CorrelationContext.clear();
            TenantContext.clear();
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
     *
     * @param eventData  Event data map
     * @param headerType Event type from header (may be null)
     * @return Detected event type
     */
    private String detectEventType(Map<String, Object> eventData, String headerType) {
        // Check header first if available
        if (headerType != null) {
            String simpleName = headerType.contains(".") ? headerType.substring(headerType.lastIndexOf('.') + 1) : headerType;
            return simpleName;
        }

        // Check for @class field (if present)
        Object classObj = eventData.get("@class");
        if (classObj instanceof String) {
            String className = (String) classObj;
            if (className.contains(".")) {
                return className.substring(className.lastIndexOf('.') + 1);
            }
            return className;
        }

        // Check aggregateType field
        Object aggregateType = eventData.get("aggregateType");
        if ("Notification".equals(aggregateType)) {
            // Check for type field to distinguish between NotificationCreatedEvent and NotificationSentEvent
            Object typeObj = eventData.get("type");
            if (typeObj != null) {
                return "NotificationCreatedEvent";
            }
            return "NotificationCreatedEvent";
        }

        return "Unknown";
    }

    /**
     * Checks if the event is a NotificationCreatedEvent.
     *
     * @param eventType Event type to check
     * @return true if this is a NotificationCreatedEvent
     */
    private boolean isNotificationCreatedEvent(String eventType) {
        return eventType != null && eventType.contains("NotificationCreatedEvent");
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
            throw new IllegalArgumentException("tenantId is required but missing in event");
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
     * Extracts notification type from event data.
     *
     * @param eventData Event data map
     * @return NotificationType
     * @throws IllegalArgumentException if type is missing or invalid
     */
    private NotificationType extractNotificationType(Map<String, Object> eventData) {
        Object typeObj = eventData.get("type");
        if (typeObj == null) {
            throw new IllegalArgumentException("type is required but missing in event");
        }

        if (typeObj instanceof NotificationType) {
            return (NotificationType) typeObj;
        } else if (typeObj instanceof String) {
            try {
                return NotificationType.valueOf((String) typeObj);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(String.format("Invalid NotificationType: %s", typeObj), e);
            }
        } else if (typeObj instanceof Map) {
            // Type might be serialized as a value object with a "value" field
            @SuppressWarnings("unchecked")
            Map<String, Object> typeMap = (Map<String, Object>) typeObj;
            Object valueObj = typeMap.get("value");
            if (valueObj instanceof String) {
                try {
                    return NotificationType.valueOf((String) valueObj);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(String.format("Invalid NotificationType: %s", valueObj), e);
                }
            }
        }

        throw new IllegalArgumentException(String.format("Unsupported type format: %s", typeObj.getClass()
                .getName()));
    }

    /**
     * Determines the delivery channel for the notification.
     * <p>
     * For MVP: Always returns EMAIL. Future: Check user preferences, notification type, etc.
     *
     * @param notificationType Notification type
     * @return Delivery channel
     */
    private NotificationChannel determineChannel(NotificationType notificationType) {
        // For MVP: default to EMAIL
        // Future implementation:
        // - Check user notification preferences
        // - Check notification type (some types might require SMS)
        // - Check tenant configuration
        return NotificationChannel.EMAIL;
    }

    /**
     * Sends notification with retry logic to handle race conditions and eventual consistency.
     * <p>
     * Retries with exponential backoff if notification is not found. This handles: - Race conditions where event is consumed before transaction commits - Database replication lag
     * in multi-database setups - Temporary database connectivity
     * issues
     * <p>
     * Production-grade retry strategy with sufficient delays to handle transaction commit delays.
     *
     * @param command        Send notification command
     * @param notificationId Notification identifier
     * @param channel        Delivery channel
     * @param tenantId       Tenant identifier (ensures tenant context is preserved during retries)
     * @throws NotificationNotFoundException if notification not found after all retries
     */
    private void sendNotificationWithRetry(SendNotificationCommand command, NotificationId notificationId, NotificationChannel channel, TenantId tenantId) {
        // Production-grade retry configuration
        // Increased retries and delays to handle transaction commit delays and eventual consistency
        int maxRetries = 5;
        long initialDelayMs = 200; // 200ms initial delay
        long maxDelayMs = 5000; // 5 seconds max delay (handles longer transaction commit times)

        logger.info("Starting notification send with retry: notificationId={}, tenantId={}, channel={}", notificationId, tenantId.getValue(), channel);

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Ensure tenant context is set before each attempt (may be cleared between retries)
                TenantId currentTenantId = TenantContext.getTenantId();
                if (currentTenantId == null || !currentTenantId.equals(tenantId)) {
                    logger.warn("Tenant context missing or incorrect before attempt {}: notificationId={}, expected={}, actual={}. Re-setting.", attempt, notificationId,
                            tenantId.getValue(),
                            currentTenantId != null ? currentTenantId.getValue() : "null");
                    TenantContext.setTenantId(tenantId);
                }
                logger.debug("Attempt {}: tenantId={}, notificationId={}", attempt, tenantId.getValue(), notificationId);

                sendHandler.handle(command);
                logger.info("Successfully sent notification on attempt {}: notificationId={}, channel={}", attempt, notificationId, channel);
                return;
            } catch (NotificationNotFoundException e) {
                TenantId tenantIdOnError = TenantContext.getTenantId();
                if (attempt == maxRetries) {
                    // Last attempt failed - rethrow to trigger Kafka retry or dead letter queue
                    logger.warn("Notification not found after {} retries: notificationId={}, tenantId={}. This may indicate the notification was never created, was deleted, or there is a persistent consistency issue. Event will be retried by Kafka.", 
                            maxRetries, notificationId, tenantIdOnError != null ? tenantIdOnError.getValue() : "null");
                    throw e;
                }
                // Calculate delay with exponential backoff
                long delayMs = Math.min(initialDelayMs * (long) Math.pow(2, attempt - 1), maxDelayMs);
                logger.warn("Notification not found on attempt {}: notificationId={}, tenantId={}. "
                                + "Retrying in {}ms (handling eventual consistency - transaction may not have committed yet).", attempt, notificationId,
                        tenantIdOnError != null ? tenantIdOnError.getValue() : "null", delayMs);
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread()
                            .interrupt();
                    throw new RuntimeException("Interrupted while waiting to retry notification send", ie);
                }
            }
        }
    }
}

