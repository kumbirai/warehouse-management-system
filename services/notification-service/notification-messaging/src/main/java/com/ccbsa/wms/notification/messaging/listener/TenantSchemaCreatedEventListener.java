package com.ccbsa.wms.notification.messaging.listener;

import java.util.Map;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.security.TenantContext;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.PostConstruct;

/**
 * Event Listener: TenantSchemaCreatedEventListener
 * <p>
 * Listens to TenantSchemaCreatedEvent and creates tenant schema with tables in notification database.
 * <p>
 * This listener implements schema-per-tenant pattern by:
 * 1. Creating the tenant schema in notification database
 * 2. Running Flyway migrations in the tenant schema to create tables and indexes
 * <p>
 * Uses Flyway programmatically to ensure migrations are properly tracked in Flyway's history table.
 * Uses local DTO to avoid tight coupling with tenant-service domain classes.
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "DataSource is a Spring-managed bean and treated as immutable infrastructure component")
public class TenantSchemaCreatedEventListener {
    private static final Logger logger = LoggerFactory.getLogger(TenantSchemaCreatedEventListener.class);

    private static final String TENANT_SCHEMA_CREATED_EVENT = "TenantSchemaCreatedEvent";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public TenantSchemaCreatedEventListener(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @PostConstruct
    public void init() {
        logger.info("TenantSchemaCreatedEventListener initialized and ready to consume TenantSchemaCreatedEvent from topic 'tenant-events' with groupId 'notification-service'");
    }

    @KafkaListener(
            topics = "tenant-events",
            groupId = "notification-service-schema-creation",
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
            if (!isTenantSchemaCreatedEvent(detectedEventType, eventData)) {
                logger.debug("Skipping event - not TenantSchemaCreatedEvent: detectedType={}, headerType={}",
                        detectedEventType, eventType);
                acknowledgment.acknowledge();
                return;
            }

            // Extract tenant ID and schema name from event data
            // aggregateId in DomainEvent is now a String containing the tenant ID
            String tenantIdString = extractTenantIdFromEvent(eventData);
            TenantId tenantId = TenantId.of(tenantIdString);
            String schemaName = extractSchemaName(eventData);

            if (schemaName == null || schemaName.trim().isEmpty()) {
                logger.error("Schema name is missing in TenantSchemaCreatedEvent: tenantId={}, eventId={}",
                        tenantId.getValue(), extractEventId(eventData));
                acknowledgment.acknowledge();
                return;
            }

            logger.info("Received TenantSchemaCreatedEvent: tenantId={}, schemaName={}, eventId={}",
                    tenantId.getValue(), schemaName, extractEventId(eventData));

            // Set tenant context for multi-tenant schema resolution
            TenantContext.setTenantId(tenantId);
            try {
                // Create schema and run Flyway migrations in tenant schema
                createTenantSchema(schemaName);
                runFlywayMigrations(schemaName);

                logger.info("Successfully created tenant schema and ran migrations: tenantId={}, schemaName={}",
                        tenantId.getValue(), schemaName);
            } finally {
                // Clear tenant context after processing
                TenantContext.clear();
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Error processing TenantSchemaCreatedEvent: eventId={}, error={}",
                    extractEventId(eventData), e.getMessage(), e);
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
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) eventData.get("metadata");
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
     * Extracts from the tenantId field (added to TenantEvent to preserve original string value).
     * This is a required field - events without tenantId are invalid.
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
            @SuppressWarnings("unchecked")
            Map<String, Object> idMap = (Map<String, Object>) tenantIdObj;
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
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) eventData.get("data");
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

    /**
     * Creates the tenant schema in the notification database.
     *
     * @param schemaName The schema name to create
     */
    private void createTenantSchema(String schemaName) {
        logger.info("Creating tenant schema: schemaName={}", schemaName);
        try {
            jdbcTemplate.execute(String.format("CREATE SCHEMA IF NOT EXISTS %s", schemaName));
            logger.info("Tenant schema created successfully: schemaName={}", schemaName);
        } catch (Exception e) {
            logger.error("Failed to create tenant schema: schemaName={}, error={}", schemaName, e.getMessage(), e);
            throw new RuntimeException(String.format("Failed to create tenant schema: %s", schemaName), e);
        }
    }

    /**
     * Runs Flyway migrations in the tenant schema.
     * <p>
     * This executes all migration scripts (V1, V2, V3, etc.) in the tenant schema,
     * creating tables, indexes, and any other database objects defined in migrations.
     * Flyway tracks migration execution in its history table within the tenant schema.
     *
     * @param schemaName The schema name where migrations should be run
     */
    private void runFlywayMigrations(String schemaName) {
        logger.info("Running Flyway migrations in tenant schema: schemaName={}", schemaName);
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(schemaName)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .load();

            var migrateResult = flyway.migrate();
            int migrationsApplied = migrateResult.migrationsExecuted;
            logger.info("Flyway migrations completed in tenant schema: schemaName={}, migrationsApplied={}",
                    schemaName, migrationsApplied);
        } catch (Exception e) {
            logger.error("Failed to run Flyway migrations in tenant schema: schemaName={}, error={}",
                    schemaName, e.getMessage(), e);
            throw new RuntimeException(String.format("Failed to run Flyway migrations in schema: %s", schemaName), e);
        }
    }
}
