# Schema-Per-Tenant Service Implementation Guide

## Overview

This guide provides step-by-step instructions for implementing the schema-per-tenant pattern in backend microservices. All tenant-aware services must consume
`TenantSchemaCreatedEvent` to programmatically create tenant schemas and run Flyway migrations.

## Prerequisites

- Service follows Clean Hexagonal Architecture
- Service has `{service}-messaging` module
- Service uses Flyway for database migrations
- Service uses PostgreSQL database

## Implementation Steps

### Step 1: Add Required Dependencies

**File:** `{service}-messaging/pom.xml`

Add Flyway dependencies:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>

<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>

<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

### Step 2: Create TenantSchemaCreatedEventListener

**File:** `{service}-messaging/src/main/java/com/ccbsa/wms/{service}/messaging/listener/TenantSchemaCreatedEventListener.java`

**Template:**

```java
package com.ccbsa.wms.{service}.messaging.listener;

import java.util.Map;
import java.util.UUID;

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
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.security.TenantContext;

/**
 * Event Listener: TenantSchemaCreatedEventListener
 * <p>
 * Listens to TenantSchemaCreatedEvent and creates tenant schema with tables in {service} database.
 * <p>
 * This listener implements schema-per-tenant pattern by:
 * 1. Creating the tenant schema in {service} database
 * 2. Running Flyway migrations in the tenant schema to create tables and indexes
 * <p>
 * Uses Flyway programmatically to ensure migrations are properly tracked in Flyway's history table.
 * Uses local DTO to avoid tight coupling with tenant-service domain classes.
 */
@Component
public class TenantSchemaCreatedEventListener {
    private static final Logger logger = LoggerFactory.getLogger(TenantSchemaCreatedEventListener.class);

    private static final String TENANT_SCHEMA_CREATED_EVENT = "TenantSchemaCreatedEvent";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public TenantSchemaCreatedEventListener(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @KafkaListener(
            topics = "tenant-events",
            groupId = "{service-name}",
            containerFactory = "externalEventKafkaListenerContainerFactory"
    )
    @Transactional
    public void handle(@Payload Map<String, Object> eventData,
                      @Header(value = "__TypeId__", required = false) String eventType,
                      @Header(value = KafkaHeaders.RECEIVED_TOPIC) String topic,
                      Acknowledgment acknowledgment) {
        logger.info("Received event on topic {}: eventData keys={}, headerType={}, @class={}",
                topic, eventData.keySet(), eventType, eventData.get("@class"));
        try {
            extractAndSetCorrelationId(eventData);

            String detectedEventType = detectEventType(eventData, eventType);
            logger.info("Event type detection: detectedType={}, headerType={}, eventDataKeys={}, aggregateType={}",
                    detectedEventType, eventType, eventData.keySet(), eventData.get("aggregateType"));
            if (!isTenantSchemaCreatedEvent(detectedEventType, eventData)) {
                logger.debug("Skipping event - not TenantSchemaCreatedEvent: detectedType={}, headerType={}",
                        detectedEventType, eventType);
                acknowledgment.acknowledge();
                return;
            }

            // For tenant events, extract as String since TenantId is String-based and may not be a valid UUID
            String aggregateIdString = extractAggregateIdAsString(eventData);
            TenantId tenantId = TenantId.of(aggregateIdString);
            String schemaName = extractSchemaName(eventData);

            if (schemaName == null || schemaName.trim().isEmpty()) {
                logger.error("Schema name is missing in TenantSchemaCreatedEvent: tenantId={}, eventId={}",
                        tenantId.getValue(), extractEventId(eventData));
                acknowledgment.acknowledge();
                return;
            }

            logger.info("Received TenantSchemaCreatedEvent: tenantId={}, schemaName={}, eventId={}",
                    tenantId.getValue(), schemaName, extractEventId(eventData));

            TenantContext.setTenantId(tenantId);
            try {
                createTenantSchema(schemaName);
                runFlywayMigrations(schemaName);

                logger.info("Successfully created tenant schema and ran migrations: tenantId={}, schemaName={}",
                        tenantId.getValue(), schemaName);
            } finally {
                TenantContext.clear();
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Error processing TenantSchemaCreatedEvent: eventId={}, error={}",
                    extractEventId(eventData), e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }

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

    private String extractSchemaName(Map<String, Object> eventData) {
        Object schemaNameObj = eventData.get("schemaName");
        if (schemaNameObj != null) {
            return schemaNameObj.toString();
        }
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
     * Extracts and validates aggregateId from event data as String.
     * <p>
     * For tenant events, aggregateId is extracted as String since TenantId is String-based
     * and may not be a valid UUID. Supports UUID, String, and Map (value object serialization) types.
     * <p>
     * Note: Aggregate IDs can be UUID or String depending on aggregate type:
     * - Tenant events use String-based TenantId (not UUID)
     * - Other events (User, Notification) may use UUID-based IDs
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
                @SuppressWarnings("unchecked")
                Map<String, Object> idMap = (Map<String, Object>) aggregateIdObj;
                Object valueObj = idMap.get("value");
                if (valueObj != null) {
                    return valueObj.toString();
                }
            }

            // Fallback: convert to string
            return aggregateIdObj.toString();
        }

        // Fallback: try to extract from nested structure
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) eventData.get("data");
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
                    @SuppressWarnings("unchecked")
                    Map<String, Object> idMap = (Map<String, Object>) aggregateId;
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

    private String detectEventType(Map<String, Object> eventData, String headerType) {
        if (headerType != null && !headerType.isEmpty()) {
            return headerType;
        }
        Object eventTypeObj = eventData.get("eventType");
        if (eventTypeObj != null) {
            return eventTypeObj.toString();
        }
        Object aggregateTypeObj = eventData.get("aggregateType");
        if (aggregateTypeObj != null) {
            return aggregateTypeObj.toString();
        }
        Object classObj = eventData.get("@class");
        if (classObj != null) {
            String className = classObj.toString();
            int lastDot = className.lastIndexOf('.');
            if (lastDot >= 0) {
                return className.substring(lastDot + 1);
            }
            return className;
        }
        return "Unknown";
    }

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
}
```

**Replace:**

- `{service}` with actual service name (e.g., `user`, `stock-management`)
- `{service-name}` with service name for Kafka group ID (e.g., `user-service`)

### Step 3: Ensure Kafka Listener Container Factory

**File:** `{service}-container/src/main/java/com/ccbsa/wms/{service}/config/{Service}Configuration.java`

Ensure `externalEventKafkaListenerContainerFactory` bean exists. If not, add:

```java
@Bean("externalEventKafkaListenerContainerFactory")
public ConcurrentKafkaListenerContainerFactory<String, Object> externalEventKafkaListenerContainerFactory(
        ConsumerFactory<String, Object> externalEventConsumerFactory) {
    // Implementation similar to notification-service
}
```

### Step 4: Update Flyway Migrations

**Files:** `{service}-dataaccess/src/main/resources/db/migration/`

Ensure migrations are schema-agnostic:

- No schema prefix in SQL statements
- Tables created in current schema (tenant schema when run programmatically)
- Also create validation tables in `public` schema for Hibernate startup validation

**Example Migration:**

```sql
-- V1__Create_initial_schema.sql
-- Creates tables in current schema (tenant schema when run programmatically)

CREATE TABLE IF NOT EXISTS {table_name}
(
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    -- ... other columns
);

-- Also create validation table in public schema for Hibernate validation
CREATE TABLE IF NOT EXISTS public.{table_name}
(
    -- Same structure as above
);
```

### Step 5: Verify Configuration

**File:** `{service}-container/src/main/resources/application.yml`

Ensure Kafka configuration includes:

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

## Testing

### Unit Test

**File:** `{service}-messaging/src/test/java/com/ccbsa/wms/{service}/messaging/listener/TenantSchemaCreatedEventListenerTest.java`

```java
@ExtendWith(MockitoExtension.class)
class TenantSchemaCreatedEventListenerTest {
    
    @Mock
    private DataSource dataSource;
    
    @Mock
    private JdbcTemplate jdbcTemplate;
    
    private TenantSchemaCreatedEventListener listener;
    
    @BeforeEach
    void setUp() {
        listener = new TenantSchemaCreatedEventListener(dataSource);
    }
    
    @Test
    void shouldCreateSchemaAndRunMigrations() {
        // Test implementation
    }
}
```

### Integration Test

1. Create tenant via `tenant-service` API
2. Verify `TenantSchemaCreatedEvent` is published
3. Verify service receives event
4. Verify schema is created
5. Verify Flyway migrations run
6. Verify tables exist in tenant schema

## Verification Checklist

- [ ] Dependencies added to `{service}-messaging/pom.xml`
- [ ] `TenantSchemaCreatedEventListener` created
- [ ] Listener registered with correct Kafka topic and group ID
- [ ] `externalEventKafkaListenerContainerFactory` bean exists
- [ ] Flyway migrations are schema-agnostic
- [ ] Validation tables created in `public` schema
- [ ] Unit tests written
- [ ] Integration tests written
- [ ] Documentation updated

## Reference Implementation

See `notification-service` for complete reference implementation:

- `services/notification-service/notification-messaging/src/main/java/com/ccbsa/wms/notification/messaging/listener/TenantSchemaCreatedEventListener.java`

## Related Documentation

- [Schema-Per-Tenant Implementation Pattern](../../01-architecture/Schema_Per_Tenant_Implementation_Pattern.md)
- [Service Architecture Document](../../01-architecture/Service_Architecture_Document.md)
- [Multi-Tenancy Enforcement Guide](../../03-security/Multi_Tenancy_Enforcement_Guide.md)

