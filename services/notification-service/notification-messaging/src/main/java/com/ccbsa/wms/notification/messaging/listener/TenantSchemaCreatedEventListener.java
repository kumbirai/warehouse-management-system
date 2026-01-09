package com.ccbsa.wms.notification.messaging.listener;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.common.security.TenantContext;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event Listener: TenantSchemaCreatedEventListener
 * <p>
 * Listens to TenantSchemaCreatedEvent and creates tenant schema with tables in notification database.
 * <p>
 * This listener implements schema-per-tenant pattern by delegating to {@link TenantSchemaProvisioner}
 * to create the tenant schema and run Flyway migrations. This ensures consistent schema creation
 * logic across both event-driven and on-demand provisioning scenarios.
 * <p>
 * Uses local DTO to avoid tight coupling with tenant-service domain classes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantSchemaCreatedEventListener {
    private static final String TENANT_SCHEMA_CREATED_EVENT = "TenantSchemaCreatedEvent";

    private final TenantSchemaProvisioner schemaProvisioner;

    @PostConstruct
    public void init() {
        log.info("TenantSchemaCreatedEventListener initialized and ready to consume TenantSchemaCreatedEvent from topic 'tenant-events' with groupId 'notification-service'");
    }

    @KafkaListener(topics = "tenant-events", groupId = "notification-service-schema-creation", containerFactory = "externalEventKafkaListenerContainerFactory")
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
            if (!isTenantSchemaCreatedEvent(detectedEventType, eventData)) {
                log.debug("Skipping event - not TenantSchemaCreatedEvent: detectedType={}, headerType={}", detectedEventType, eventType);
                acknowledgment.acknowledge();
                return;
            }

            // Extract tenant ID and schema name from event data
            // aggregateId in DomainEvent is now a String containing the tenant ID
            String tenantIdString = extractTenantIdFromEvent(eventData);
            TenantId tenantId = TenantId.of(tenantIdString);
            String schemaName = extractSchemaName(eventData);

            if (schemaName == null || schemaName.trim().isEmpty()) {
                log.error("Schema name is missing in TenantSchemaCreatedEvent: tenantId={}, eventId={}", tenantId.getValue(), extractEventId(eventData));
                acknowledgment.acknowledge();
                return;
            }

            log.info("Received TenantSchemaCreatedEvent: tenantId={}, schemaName={}, eventId={}", tenantId.getValue(), schemaName, extractEventId(eventData));

            // Set tenant context for multi-tenant schema resolution
            TenantContext.setTenantId(tenantId);
            try {
                // Delegate to TenantSchemaProvisioner to create schema and run Flyway migrations
                // TenantSchemaProvisioner.ensureSchemaReady() uses @Transactional(propagation = NOT_SUPPORTED)
                // to suspend any transaction context and prevent connection leaks when Flyway manages its own connections
                // This listener must NOT be @Transactional to allow NOT_SUPPORTED to work correctly
                schemaProvisioner.ensureSchemaReady(schemaName);

                log.info("Successfully created tenant schema and ran migrations: tenantId={}, schemaName={}", tenantId.getValue(), schemaName);
            } finally {
                // Clear tenant context after processing
                TenantContext.clear();
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing TenantSchemaCreatedEvent: eventId={}, error={}", extractEventId(eventData), e.getMessage(), e);
            // Acknowledge even on error to prevent infinite retries for schema creation failures
            // Schema creation failures should be handled manually or via monitoring alerts
            acknowledgment.acknowledge();
        }
    }

    /**
     * Extracts and sets correlation ID from event metadata.
     *
     * @param eventData The event data map
     */
    private void extractAndSetCorrelationId(Map<String, Object> eventData) {
        @SuppressWarnings("unchecked") Map<String, Object> metadata = (Map<String, Object>) eventData.get("metadata");
        if (metadata != null) {
            Object correlationIdObj = metadata.get("correlationId");
            if (correlationIdObj != null) {
                CorrelationContext.setCorrelationId(correlationIdObj.toString());
            }
        }
    }

    /**
     * Detects event type from event data or header.
     *
     * @param eventData  The event data map
     * @param headerType The event type from header
     * @return The detected event type
     */
    private String detectEventType(Map<String, Object> eventData, String headerType) {
        if (headerType != null && !headerType.isEmpty()) {
            return headerType;
        }

        // Prioritize @class field over aggregateType for accurate event type detection
        Object classObj = eventData.get("@class");
        if (classObj != null) {
            String className = classObj.toString();
            // Extract simple class name from fully qualified name
            int lastDot = className.lastIndexOf('.');
            if (lastDot >= 0) {
                return className.substring(lastDot + 1);
            }
            return className;
        }

        Object eventTypeObj = eventData.get("eventType");
        if (eventTypeObj != null) {
            return eventTypeObj.toString();
        }

        // aggregateType is less specific (e.g., "Tenant" for all tenant events)
        // Only use as fallback
        Object aggregateTypeObj = eventData.get("aggregateType");
        if (aggregateTypeObj != null) {
            return aggregateTypeObj.toString();
        }

        return "Unknown";
    }

    /**
     * Checks if the event is a TenantSchemaCreatedEvent.
     *
     * @param detectedEventType The detected event type
     * @param eventData         The event data map
     * @return true if the event is a TenantSchemaCreatedEvent
     */
    private boolean isTenantSchemaCreatedEvent(String detectedEventType, Map<String, Object> eventData) {
        // Check if event type matches TenantSchemaCreatedEvent
        if (TENANT_SCHEMA_CREATED_EVENT.equals(detectedEventType)) {
            return true;
        }

        // Check for TenantSchemaCreatedEvent-specific field (has schemaName)
        if (eventData.containsKey("schemaName")) {
            return true;
        }

        // Check @class field
        Object eventClass = eventData.get("@class");
        if (eventClass != null && eventClass.toString().contains(TENANT_SCHEMA_CREATED_EVENT)) {
            return true;
        }

        return false;
    }

    /**
     * Extracts tenantId from event data.
     * <p>
     * Extracts from the tenantId field (added to TenantEvent to preserve original string value). This is a required field - events without tenantId are invalid.
     *
     * @param eventData Event data map
     * @return String tenant ID
     * @throws IllegalArgumentException if tenantId is missing or invalid
     */
    private String extractTenantIdFromEvent(Map<String, Object> eventData) {
        Object tenantIdObj = eventData.get("tenantId");
        if (tenantIdObj == null) {
            throw new IllegalArgumentException("tenantId is required but missing in event");
        }

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

    /**
     * Extracts schema name from event data.
     *
     * @param eventData The event data map
     * @return The schema name, or null if not found
     */
    private String extractSchemaName(Map<String, Object> eventData) {
        // Try to extract schemaName from event data
        Object schemaNameObj = eventData.get("schemaName");
        if (schemaNameObj != null) {
            return schemaNameObj.toString();
        }

        // Fallback: try to extract from nested structure
        @SuppressWarnings("unchecked") Map<String, Object> data = (Map<String, Object>) eventData.get("data");
        if (data != null) {
            Object schemaName = data.get("schemaName");
            if (schemaName != null) {
                return schemaName.toString();
            }
        }

        return null;
    }

    /**
     * Extracts event ID from event data.
     *
     * @param eventData The event data map
     * @return The event ID, or "unknown" if not found
     */
    private String extractEventId(Map<String, Object> eventData) {
        Object eventIdObj = eventData.get("eventId");
        if (eventIdObj != null) {
            return eventIdObj.toString();
        }

        Object idObj = eventData.get("id");
        if (idObj != null) {
            return idObj.toString();
        }

        return "unknown";
    }
}
