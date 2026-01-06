package com.ccbsa.wms.stock.messaging.listener;

import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
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
import lombok.extern.slf4j.Slf4j;

/**
 * Event Listener: TenantSchemaCreatedEventListener
 * <p>
 * Listens to TenantSchemaCreatedEvent and creates tenant schema with tables in stock-management database.
 * <p>
 * This listener implements schema-per-tenant pattern by: 1. Creating the tenant schema in stock-management database 2. Running Flyway migrations in the tenant schema to create
 * tables and indexes
 * <p>
 * Uses Flyway programmatically to ensure migrations are properly tracked in Flyway's history table. Uses local DTO to avoid tight coupling with tenant-service domain classes.
 * <p>
 * Idempotency: This listener is idempotent - multiple concurrent executions are safe: - CREATE SCHEMA IF NOT EXISTS ensures schema creation is idempotent - Flyway tracks
 * migrations in flyway_schema_history table, ensuring migrations run
 * only once
 */
@Slf4j
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "DataSource is a Spring-managed bean and treated as immutable infrastructure component")
public class TenantSchemaCreatedEventListener {
    private static final String TENANT_SCHEMA_CREATED_EVENT = "TenantSchemaCreatedEvent";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public TenantSchemaCreatedEventListener(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @KafkaListener(topics = "tenant-events", groupId = "stock-management-service", containerFactory = "externalEventKafkaListenerContainerFactory")
    public void handle(@Payload Map<String, Object> eventData, @Header(value = "__TypeId__", required = false) String eventType,
                       @Header(value = KafkaHeaders.RECEIVED_TOPIC) String topic, Acknowledgment acknowledgment) {
        log.info("Received event on topic {}: eventData keys={}, headerType={}, @class={}", topic, eventData.keySet(), eventType, eventData.get("@class"));
        boolean shouldAcknowledge = false;
        try {
            extractAndSetCorrelationId(eventData);

            String detectedEventType = detectEventType(eventData, eventType);
            log.info("Event type detection: detectedType={}, headerType={}, eventDataKeys={}, aggregateType={}", detectedEventType, eventType, eventData.keySet(),
                    eventData.get("aggregateType"));
            if (!isTenantSchemaCreatedEvent(detectedEventType, eventData)) {
                log.debug("Skipping event - not TenantSchemaCreatedEvent: detectedType={}, headerType={}", detectedEventType, eventType);
                shouldAcknowledge = true;
                return;
            }

            // For tenant events, extract as String since TenantId is String-based and may not be a valid UUID
            String aggregateIdString = extractAggregateIdAsString(eventData);
            TenantId tenantId = TenantId.of(aggregateIdString);
            String schemaName = extractSchemaName(eventData);

            if (schemaName == null || schemaName.trim().isEmpty()) {
                log.error("Schema name is missing in TenantSchemaCreatedEvent: tenantId={}, eventId={}", tenantId.getValue(), extractEventId(eventData));
                shouldAcknowledge = true;
                return;
            }

            log.info("Received TenantSchemaCreatedEvent: tenantId={}, schemaName={}, eventId={}", tenantId.getValue(), schemaName, extractEventId(eventData));

            TenantContext.setTenantId(tenantId);
            try {
                createTenantSchema(schemaName);
                runFlywayMigrations(schemaName);

                log.info("Successfully created tenant schema and ran migrations: tenantId={}, schemaName={}", tenantId.getValue(), schemaName);
                shouldAcknowledge = true;
            } finally {
                TenantContext.clear();
            }

        } catch (Exception e) {
            log.error("Error processing TenantSchemaCreatedEvent: eventId={}, error={}", extractEventId(eventData), e.getMessage(), e);
            // Acknowledge even on error to prevent infinite reprocessing of events that will always fail
            shouldAcknowledge = true;
        } finally {
            // Acknowledge only after all operations complete and context is cleared
            if (shouldAcknowledge) {
                acknowledgment.acknowledge();
            }
        }
    }

    private void extractAndSetCorrelationId(Map<String, Object> eventData) {
        @SuppressWarnings("unchecked") Map<String, Object> metadata = (Map<String, Object>) eventData.get("metadata");
        if (metadata != null) {
            Object correlationIdObj = metadata.get("correlationId");
            if (correlationIdObj != null) {
                CorrelationContext.setCorrelationId(correlationIdObj.toString());
            }
        }
    }

    private String detectEventType(Map<String, Object> eventData, String headerType) {
        if (headerType != null && !headerType.isEmpty()) {
            return headerType;
        }

        // Prioritize @class field over aggregateType for accurate event type detection
        Object classObj = eventData.get("@class");
        if (classObj != null) {
            String className = classObj.toString();
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

    private boolean isTenantSchemaCreatedEvent(String detectedEventType, Map<String, Object> eventData) {
        if (TENANT_SCHEMA_CREATED_EVENT.equals(detectedEventType)) {
            return true;
        }
        if (eventData.containsKey("schemaName")) {
            return true;
        }
        Object eventClass = eventData.get("@class");
        if (eventClass != null && eventClass.toString().contains(TENANT_SCHEMA_CREATED_EVENT)) {
            return true;
        }
        return false;
    }

    /**
     * Extracts and validates aggregateId from event data as String.
     * <p>
     * For tenant events, aggregateId is extracted as String since TenantId is String-based and may not be a valid UUID. Supports UUID, String, and Map (value object serialization)
     * types.
     *
     * @param eventData Event data map
     * @return String aggregate ID
     * @throws IllegalArgumentException if aggregateId is missing or invalid
     */
    private String extractAggregateIdAsString(Map<String, Object> eventData) {
        Object aggregateIdObj = eventData.get("aggregateId");
        if (aggregateIdObj != null) {
            // Handle UUID
            if (aggregateIdObj instanceof UUID) {
                return aggregateIdObj.toString();
            }

            // Handle String
            if (aggregateIdObj instanceof String) {
                return (String) aggregateIdObj;
            }

            // Handle value object serialization (Map with "value" field)
            if (aggregateIdObj instanceof Map) {
                @SuppressWarnings("unchecked") Map<String, Object> idMap = (Map<String, Object>) aggregateIdObj;
                Object valueObj = idMap.get("value");
                if (valueObj != null) {
                    return valueObj.toString();
                }
            }

            // Fallback: convert to string
            return aggregateIdObj.toString();
        }

        // Fallback: try to extract from nested structure
        @SuppressWarnings("unchecked") Map<String, Object> data = (Map<String, Object>) eventData.get("data");
        if (data != null) {
            Object aggregateId = data.get("aggregateId");
            if (aggregateId != null) {
                if (aggregateId instanceof UUID) {
                    return aggregateId.toString();
                }
                if (aggregateId instanceof String) {
                    return (String) aggregateId;
                }
                if (aggregateId instanceof Map) {
                    @SuppressWarnings("unchecked") Map<String, Object> idMap = (Map<String, Object>) aggregateId;
                    Object valueObj = idMap.get("value");
                    if (valueObj != null) {
                        return valueObj.toString();
                    }
                }
                return aggregateId.toString();
            }
        }
        throw new IllegalArgumentException("aggregateId not found in event data");
    }

    private String extractSchemaName(Map<String, Object> eventData) {
        Object schemaNameObj = eventData.get("schemaName");
        if (schemaNameObj != null) {
            return schemaNameObj.toString();
        }
        @SuppressWarnings("unchecked") Map<String, Object> data = (Map<String, Object>) eventData.get("data");
        if (data != null) {
            Object schemaName = data.get("schemaName");
            if (schemaName != null) {
                return schemaName.toString();
            }
        }
        return null;
    }

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

    private void createTenantSchema(String schemaName) {
        log.info("Creating tenant schema: schemaName={}", schemaName);
        try {
            jdbcTemplate.execute(String.format("CREATE SCHEMA IF NOT EXISTS %s", schemaName));
            log.info("Tenant schema created successfully: schemaName={}", schemaName);
        } catch (Exception e) {
            log.error("Failed to create tenant schema: schemaName={}, error={}", schemaName, e.getMessage(), e);
            throw new RuntimeException(String.format("Failed to create tenant schema: %s", schemaName), e);
        }
    }

    private void runFlywayMigrations(String schemaName) {
        log.info("Running Flyway migrations in tenant schema: schemaName={}", schemaName);
        try {
            Flyway flyway = Flyway.configure().dataSource(dataSource).schemas(schemaName).locations("classpath:db/migration").baselineOnMigrate(true).load();

            var migrateResult = flyway.migrate();
            int migrationsApplied = migrateResult.migrationsExecuted;
            log.info("Flyway migrations completed in tenant schema: schemaName={}, migrationsApplied={}", schemaName, migrationsApplied);
        } catch (Exception e) {
            log.error("Failed to run Flyway migrations in tenant schema: schemaName={}, error={}", schemaName, e.getMessage(), e);
            throw new RuntimeException(String.format("Failed to run Flyway migrations in schema: %s", schemaName), e);
        }
    }
}

