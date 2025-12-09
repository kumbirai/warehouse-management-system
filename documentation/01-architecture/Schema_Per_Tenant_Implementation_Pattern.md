# Schema-Per-Tenant Implementation Pattern

## Overview

All backend microservices (except `tenant-service`) implement the schema-per-tenant pattern for multi-tenant data isolation. This pattern ensures complete data isolation between
tenants at the database level while maintaining operational simplicity.

## Architecture Pattern

### Schema-Per-Tenant Strategy

**Principle:**

- Each tenant (LDP) has its own isolated PostgreSQL schema within the service's database
- Schema naming convention: `tenant_{sanitized_tenant_id}_schema`
- All service tables are created in tenant-specific schemas
- Flyway migrations run programmatically in each tenant schema

**Services Using This Pattern:**

- `notification-service`
- `user-service`
- `stock-management-service`
- `product-service`
- `location-management-service`
- `picking-service`
- `returns-service`
- `reconciliation-service`
- `integration-service`

**Exception:**

- `tenant-service` uses the `public` schema (manages tenants themselves, not tenant-aware)

### Schema Creation Flow

**Event-Driven Schema Creation:**

1. **Tenant Creation:**
    - `tenant-service` creates a new tenant
    - `Tenant.build()` publishes `TenantSchemaCreatedEvent` with schema name
    - Event published to Kafka topic `tenant-events`

2. **Service Schema Setup:**
    - Each backend service listens to `TenantSchemaCreatedEvent`
    - Service creates tenant schema in its database
    - Service runs Flyway migrations programmatically in tenant schema
    - All tables and indexes created in tenant schema

3. **Runtime Operation:**
    - `TenantContext` set from JWT token
    - `TenantSchemaResolver` resolves schema name from tenant context
    - `TenantAwarePhysicalNamingStrategy` replaces `tenant_schema` placeholder with actual schema
    - Hibernate queries use tenant-specific schema

## Implementation Requirements

### Mandatory Components

All tenant-aware services must implement:

1. **TenantSchemaCreatedEventListener**
    - Listens to `TenantSchemaCreatedEvent` from `tenant-service`
    - Creates tenant schema in service database
    - Runs Flyway migrations programmatically in tenant schema

2. **Flyway Dependencies**
    - `org.flywaydb:flyway-core`
    - `org.flywaydb:flyway-database-postgresql`

3. **DataSource and JdbcTemplate**
    - Required for programmatic schema creation and Flyway execution

### Event Listener Implementation

**Location:** `{service}-messaging/src/main/java/com/ccbsa/wms/{service}/messaging/listener/TenantSchemaCreatedEventListener.java`

**Responsibilities:**

- Consume `TenantSchemaCreatedEvent` from Kafka topic `tenant-events`
- Extract tenant ID and schema name from event
- Create tenant schema: `CREATE SCHEMA IF NOT EXISTS {schema_name}`
- Run Flyway migrations in tenant schema context
- Handle errors gracefully (acknowledge to prevent infinite retries)

**Implementation Pattern:**

```java
@Component
public class TenantSchemaCreatedEventListener {
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

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
        // Extract tenant ID and schema name
        TenantId tenantId = extractTenantId(eventData);
        String schemaName = extractSchemaName(eventData);
        
        // Set tenant context
        TenantContext.setTenantId(tenantId);
        try {
            // Create schema
            createTenantSchema(schemaName);
            
            // Run Flyway migrations
            runFlywayMigrations(schemaName);
        } finally {
            TenantContext.clear();
        }
        
        acknowledgment.acknowledge();
    }

    private void createTenantSchema(String schemaName) {
        jdbcTemplate.execute(String.format("CREATE SCHEMA IF NOT EXISTS %s", schemaName));
    }

    private void runFlywayMigrations(String schemaName) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
    }
}
```

### Flyway Migration Strategy

**Migration Files:**

- Migrations must be schema-agnostic (no schema prefix in SQL)
- Migrations create tables in the current schema (tenant schema when run programmatically)
- Validation tables in `public` schema for Hibernate startup validation

**Migration Structure:**

- `V1__Create_initial_schema.sql` - Creates all tables
- `V2__Add_indexes.sql` - Creates indexes
- `V3__Insert_initial_data.sql` - Inserts reference data (if any)

**Example Migration (schema-agnostic):**

```sql
-- V1__Create_initial_schema.sql
-- Creates tables in current schema (tenant schema when run programmatically)

CREATE TABLE IF NOT EXISTS notifications
(
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    -- ... other columns
);
```

**Public Schema Validation Table:**

- Also create a minimal validation table in `public` schema for Hibernate schema validation at startup
- This table is NOT used at runtime

### Dependencies Configuration

**Required Dependencies in `{service}-messaging/pom.xml`:**

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

## Schema Naming Convention

**Schema Name Format:**

- Pattern: `tenant_{sanitized_tenant_id}_schema`
- Sanitization rules:
    - Convert to lowercase
    - Replace special characters (`-`, `.`, spaces) with underscores
    - Remove invalid characters (keep only alphanumeric and underscores)
    - Ensure starts with letter or underscore
    - Truncate to fit PostgreSQL identifier limit (63 characters)

**Example:**

- Tenant ID: `qui-ea-eum` → Schema: `tenant_qui_ea_eum_schema`
- Tenant ID: `natus.qui.laboriosam` → Schema: `tenant_natus_qui_laboriosam_schema`

**Implementation:**

- Schema name is resolved by `Tenant.resolveSchemaName()` in `tenant-service`
- Published in `TenantSchemaCreatedEvent`
- Services use the schema name from the event (no recalculation)

## Runtime Schema Resolution

### Tenant Context Propagation

1. **JWT Token:**
    - Contains `tenant_id` claim
    - Extracted by gateway and propagated via `X-Tenant-Id` header

2. **TenantContext:**
    - Thread-local storage for tenant ID
    - Set by security filter/interceptor
    - Available throughout request processing

3. **TenantSchemaResolver:**
    - Resolves schema name from `TenantContext`
    - Uses same sanitization logic as `Tenant.resolveSchemaName()`

4. **TenantAwarePhysicalNamingStrategy:**
    - Hibernate naming strategy
    - Replaces `tenant_schema` placeholder with actual schema name
    - Applied at query execution time

### Entity Configuration

**JPA Entity Example:**

```java
@Entity
@Table(name = "notifications", schema = "tenant_schema")
public class NotificationEntity {
    // Entity fields
}
```

**Note:** The `schema = "tenant_schema"` is a placeholder that is replaced at runtime by `TenantAwarePhysicalNamingStrategy` with the actual tenant schema name.

## Error Handling

### Schema Creation Failures

**Handling Strategy:**

- Log error with full context (tenant ID, schema name, error details)
- Acknowledge Kafka message to prevent infinite retries
- Alert monitoring system for manual intervention
- Schema creation failures are critical and require immediate attention

**Recovery:**

- Manual schema creation via database admin tools
- Re-publish `TenantSchemaCreatedEvent` (if event replay is supported)
- Service restart will not retry (event already acknowledged)

### Migration Failures

**Handling Strategy:**

- Flyway tracks migration state in `flyway_schema_history` table
- Failed migrations are logged and tracked
- Manual intervention required to fix migration issues
- Once fixed, Flyway will retry on next service restart or manual migration trigger

## Testing

### Unit Testing

**Test Schema Creation:**

- Mock `DataSource` and `JdbcTemplate`
- Verify schema creation SQL execution
- Verify Flyway configuration and migration execution

### Integration Testing

**Test End-to-End Flow:**

1. Create tenant via `tenant-service` API
2. Verify `TenantSchemaCreatedEvent` is published
3. Verify service receives event and creates schema
4. Verify Flyway migrations run successfully
5. Verify tables exist in tenant schema
6. Verify Flyway history table is created

**Database Verification:**

```sql
-- Check schema exists
SELECT schema_name FROM information_schema.schemata 
WHERE schema_name LIKE 'tenant_%_schema';

-- Check tables in tenant schema
SELECT tablename FROM pg_tables 
WHERE schemaname = 'tenant_{tenant_id}_schema';

-- Check Flyway history
SET search_path TO tenant_{tenant_id}_schema;
SELECT * FROM flyway_schema_history;
```

## Operational Considerations

### Schema Management

**Schema Lifecycle:**

- Created: When tenant is created (via `TenantSchemaCreatedEvent`)
- Updated: Via Flyway migrations (when new migrations are added)
- Deleted: When tenant is deleted (manual operation, not automated)

**Migration Management:**

- New migrations are automatically applied to all existing tenant schemas
- Flyway tracks migration state per schema
- Migrations are idempotent (safe to run multiple times)

### Monitoring

**Key Metrics:**

- Schema creation success/failure rate
- Migration execution time per schema
- Number of tenant schemas per service
- Schema size and growth

**Alerts:**

- Schema creation failures
- Migration failures
- Schema size thresholds

### Backup and Recovery

**Backup Strategy:**

- Backup entire database (includes all tenant schemas)
- Per-tenant backup possible (schema-level backup)
- Point-in-time recovery at database level

**Recovery:**

- Restore entire database from backup
- Restore specific tenant schema from backup
- Re-run migrations if needed

## Best Practices

1. **Always Use Flyway:**
    - Never create tables manually
    - All schema changes via migrations
    - Tracked in Flyway history

2. **Schema-Agnostic Migrations:**
    - Never hardcode schema names in migrations
    - Use current schema context
    - Works in both public (validation) and tenant schemas

3. **Error Handling:**
    - Acknowledge events even on failure (prevent infinite retries)
    - Log errors with full context
    - Alert monitoring system

4. **Testing:**
    - Test schema creation in integration tests
    - Verify migrations run successfully
    - Test tenant isolation

5. **Documentation:**
    - Document migration changes
    - Document schema structure
    - Document operational procedures

## Related Documentation

- [Service Architecture Document](./Service_Architecture_Document.md) - Overall service architecture
- [Multi-Tenancy Enforcement Guide](../03-security/Multi_Tenancy_Enforcement_Guide.md) - Tenant isolation enforcement
- [Development Environment Setup](../05-development/Development_Environment_Setup.md) - Development setup
- [Mandated Implementation Template Guide](../guide/mandated-Implementation-template-guide.md) - Implementation templates

