# TenantSchemaCreatedEvent Implementation Status

## Overview

This document tracks the implementation status of `TenantSchemaCreatedEventListener` across all backend microservices. All tenant-aware services must consume
`TenantSchemaCreatedEvent` to programmatically create tenant schemas and run Flyway migrations.

## Implementation Status

### Completed Services

1. **notification-service** ✅
    - Listener: `services/notification-service/notification-messaging/src/main/java/com/ccbsa/wms/notification/messaging/listener/TenantSchemaCreatedEventListener.java`
    - Configuration: `services/notification-service/notification-container/src/main/java/com/ccbsa/wms/notification/config/NotificationServiceConfiguration.java`
    - Dependencies: Flyway dependencies added to `notification-messaging/pom.xml`
    - Status: Fully implemented and tested

2. **user-service** ✅
    - Listener: `services/user-service/user-messaging/src/main/java/com/ccbsa/wms/user/messaging/listener/TenantSchemaCreatedEventListener.java`
    - Configuration: `services/user-service/user-container/src/main/java/com/ccbsa/wms/user/config/UserServiceConfiguration.java`
    - Dependencies: Flyway dependencies added to `user-messaging/pom.xml`
    - Status: Fully implemented

### Pending Services

3. **stock-management-service** ⏳
    - Listener: Not implemented
    - Configuration: Needs `externalEventKafkaListenerContainerFactory` bean
    - Dependencies: Need Flyway dependencies in `stock-management-messaging/pom.xml`
    - Status: Pending implementation

4. **product-service** ⏳
    - Listener: Not implemented
    - Configuration: Needs `externalEventKafkaListenerContainerFactory` bean
    - Dependencies: Need Flyway dependencies in `product-messaging/pom.xml`
    - Status: Pending implementation

5. **location-management-service** ⏳
    - Listener: Not implemented
    - Configuration: Needs `externalEventKafkaListenerContainerFactory` bean
    - Dependencies: Need Flyway dependencies in `location-management-messaging/pom.xml`
    - Status: Pending implementation

6. **picking-service** ⏳
    - Listener: Not implemented
    - Configuration: Needs `externalEventKafkaListenerContainerFactory` bean
    - Dependencies: Need Flyway dependencies in `picking-messaging/pom.xml`
    - Status: Pending implementation

7. **returns-service** ⏳
    - Listener: Not implemented
    - Configuration: Needs `externalEventKafkaListenerContainerFactory` bean
    - Dependencies: Need Flyway dependencies in `returns-messaging/pom.xml`
    - Status: Pending implementation

8. **reconciliation-service** ⏳
    - Listener: Not implemented
    - Configuration: Needs `externalEventKafkaListenerContainerFactory` bean
    - Dependencies: Need Flyway dependencies in `reconciliation-messaging/pom.xml`
    - Status: Pending implementation

9. **integration-service** ⏳
    - Listener: Not implemented
    - Configuration: Needs `externalEventKafkaListenerContainerFactory` bean
    - Dependencies: Need Flyway dependencies in `integration-messaging/pom.xml`
    - Status: Pending implementation

## Implementation Template

### Step 1: Add Dependencies

**File:** `{service}-messaging/pom.xml`

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

### Step 2: Create Listener

**File:** `{service}-messaging/src/main/java/com/ccbsa/wms/{service}/messaging/listener/TenantSchemaCreatedEventListener.java`

Use the implementation from `user-service` as a template, replacing:

- Package name: `com.ccbsa.wms.{service}.messaging.listener`
- Service name in comments: `{service} database`
- Kafka group ID: `{service}-service`

### Step 3: Add Kafka Configuration

**File:** `{service}-container/src/main/java/com/ccbsa/wms/{service}/config/{Service}Configuration.java`

Add `externalEventKafkaListenerContainerFactory` bean (see `user-service` implementation).

## Idempotency Guarantees

The implementation ensures idempotency to prevent race conditions:

1. **Schema Creation:**
    - Uses `CREATE SCHEMA IF NOT EXISTS` - safe for concurrent execution
    - PostgreSQL ensures atomicity

2. **Flyway Migrations:**
    - Flyway tracks migrations in `flyway_schema_history` table
    - Migrations are idempotent - run only once per schema
    - Concurrent executions are safe (Flyway uses database locks)

3. **Event Processing:**
    - Kafka consumer group ensures each service processes events once
    - Manual acknowledgment prevents duplicate processing
    - Error handling acknowledges even on failure (prevents infinite retries)

## Testing

### Unit Testing

Test schema creation and migration execution with mocked dependencies.

### Integration Testing

1. Create tenant via `tenant-service` API
2. Verify `TenantSchemaCreatedEvent` is published
3. Verify service receives event and creates schema
4. Verify Flyway migrations run successfully
5. Verify tables exist in tenant schema

### Database Verification

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

## Next Steps

1. Implement listeners for remaining services (stock-management, product, location-management, picking, returns, reconciliation, integration)
2. Add Kafka configuration beans to each service
3. Add Flyway dependencies to each messaging module
4. Test schema creation end-to-end
5. Verify idempotency with concurrent tenant creation

## Related Documentation

- [Schema-Per-Tenant Implementation Pattern](../01-architecture/Schema_Per_Tenant_Implementation_Pattern.md)
- [Schema-Per-Tenant Service Implementation Guide](./Schema_Per_Tenant_Service_Implementation_Guide.md)
- [Service Architecture Document](../01-architecture/Service_Architecture_Document.md)

