# Tenant Schema Creation and Notification Delivery Fixes

## Overview

Fixed three critical issues:

1. **Schema creation on tenant creation** - TenantSchemaCreatedEvent now published when tenant is created (not just activated)
2. **Flyway migrations on tenant schemas** - Using Flyway programmatically to run migrations in tenant schemas
3. **Notification delivery adapter** - Email adapter now properly registered

## Issues Fixed

### Issue 1: Schema Creation Timing

**Problem:**

- `TenantSchemaCreatedEvent` was only published when tenant was ACTIVATED
- User requirement: Schema should be created when tenant is CREATED

**Solution:**

- Modified `Tenant.build()` to publish `TenantSchemaCreatedEvent` on tenant creation
- Schema is now created immediately when tenant is created, before activation
- Activation still publishes the event (for services that might have missed the creation event)

**Files Modified:**

- `services/tenant-service/tenant-domain/tenant-domain-core/src/main/java/com/ccbsa/wms/tenant/domain/core/entity/Tenant.java`
    - Added `TenantSchemaCreatedEvent` publication in `Builder.build()` method
    - Made `resolveSchemaName()` package-private (was private) for Builder access

### Issue 2: Flyway Migrations on Tenant Schemas

**Problem:**

- Current implementation uses SQL templates and JdbcTemplate
- User requirement: Use Flyway programmatically to run migrations on tenant schemas

**Solution:**

- Updated `TenantSchemaCreatedEventListener` to use Flyway programmatically
- Flyway runs migrations in tenant schema context
- Migrations are tracked in Flyway's history table within each tenant schema

**Files Modified:**

- `services/notification-service/notification-messaging/src/main/java/com/ccbsa/wms/notification/messaging/listener/TenantSchemaCreatedEventListener.java`
    - Replaced SQL template execution with Flyway programmatic execution
    - Uses `Flyway.configure().schemas(schemaName).locations("classpath:db/migration").migrate()`
    - Added Flyway dependencies to `notification-messaging/pom.xml`

**Dependencies Added:**

- `org.flywaydb:flyway-core`
- `org.flywaydb:flyway-database-postgresql`

### Issue 3: Notification Delivery Adapter Not Registered

**Problem:**

- Error: "No delivery adapter available for channel: EMAIL"
- `SmtpEmailDeliveryAdapter` was not being registered as a Spring bean

**Root Cause:**

- `notification-email` module was not included as a dependency in `notification-container`
- Spring Boot was not scanning the `com.ccbsa.wms.notification.email` package

**Solution:**

- Added `notification-email` dependency to `notification-container/pom.xml`
- Updated `NotificationServiceApplication` to scan `com.ccbsa.wms.notification.email` package

**Files Modified:**

- `services/notification-service/notification-container/pom.xml`
    - Added `notification-email` dependency
- `services/notification-service/notification-container/src/main/java/com/ccbsa/wms/notification/NotificationServiceApplication.java`
    - Added `com.ccbsa.wms.notification.email` to `scanBasePackages`

## How It Works Now

### Tenant Creation Flow

1. **Tenant Created:**
    - `CreateTenantCommandHandler` creates tenant via `Tenant.builder().build()`
    - `Tenant.build()` publishes `TenantCreatedEvent` and `TenantSchemaCreatedEvent`
    - Events are published after transaction commit

2. **Schema Creation (Notification Service):**
    - `TenantSchemaCreatedEventListener` receives `TenantSchemaCreatedEvent`
    - Creates tenant schema: `CREATE SCHEMA IF NOT EXISTS tenant_{tenant_id}_schema`
    - Runs Flyway migrations in tenant schema:
        - V1__Create_initial_schema.sql (creates notifications table)
        - V2__Add_indexes.sql (creates indexes)
        - V3__Insert_initial_data.sql (no-op)
    - Flyway tracks migrations in `flyway_schema_history` table within tenant schema

3. **Tenant Activation:**
    - `ActivateTenantCommandHandler` calls `tenant.activate()`
    - `Tenant.activate()` publishes `TenantSchemaCreatedEvent` (again, for services that might have missed it) and `TenantActivatedEvent`
    - `TenantActivatedEventListener` creates welcome notification

4. **Notification Delivery:**
    - `NotificationCreatedEventListener` receives `NotificationCreatedEvent`
    - Finds `SmtpEmailDeliveryAdapter` via Spring's dependency injection
    - Sends email via SMTP (MailHog in development)

## Migration Strategy

### Flyway Programmatic Execution

When `TenantSchemaCreatedEvent` is received:

1. **Create Schema:**
   ```java
   jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
   ```

2. **Run Flyway Migrations:**
   ```java
   Flyway flyway = Flyway.configure()
       .dataSource(dataSource)
       .schemas(schemaName)  // Sets default schema
       .locations("classpath:db/migration")
       .baselineOnMigrate(true)
       .load();
   flyway.migrate();
   ```

3. **Result:**
    - All migration scripts (V1, V2, V3) run in tenant schema
    - Tables created in tenant schema (not public)
    - Indexes created in tenant schema
    - Flyway history tracked in tenant schema's `flyway_schema_history` table

### Migration Files

**V1__Create_initial_schema.sql:**

- Creates `notifications` table in current schema (tenant schema when run programmatically)
- Also creates validation table in public schema for Hibernate startup validation

**V2__Add_indexes.sql:**

- Creates indexes in current schema (tenant schema when run programmatically)
- Also creates validation indexes in public schema for Hibernate startup validation

**V3__Insert_initial_data.sql:**

- Empty (no initial data required)

## Testing

### Verify Schema Creation

1. Create a tenant via tenant-service API
2. Check logs for `TenantSchemaCreatedEvent` publication
3. Verify notification-service receives event and creates schema:
   ```sql
   SELECT schemaname FROM pg_tables WHERE tablename = 'notifications' AND schemaname LIKE 'tenant_%';
   ```

### Verify Flyway Migrations

1. Check Flyway history in tenant schema:
   ```sql
   SET search_path TO tenant_{tenant_id}_schema;
   SELECT * FROM flyway_schema_history;
   ```

2. Verify tables exist:
   ```sql
   SELECT tablename FROM pg_tables WHERE schemaname = 'tenant_{tenant_id}_schema';
   ```

3. Verify indexes exist:
   ```sql
   SELECT indexname FROM pg_indexes WHERE schemaname = 'tenant_{tenant_id}_schema';
   ```

### Verify Notification Delivery

1. Activate tenant
2. Check logs for notification creation and email sending
3. Verify email adapter is registered:
    - Check startup logs for `SmtpEmailDeliveryAdapter` bean creation
    - Check that `SendNotificationCommandHandler` finds EMAIL adapter

## Files Modified

1. **services/tenant-service/tenant-domain/tenant-domain-core/src/main/java/com/ccbsa/wms/tenant/domain/core/entity/Tenant.java**
    - Publish `TenantSchemaCreatedEvent` on creation
    - Made `resolveSchemaName()` package-private

2. **services/notification-service/notification-messaging/src/main/java/com/ccbsa/wms/notification/messaging/listener/TenantSchemaCreatedEventListener.java**
    - Replaced SQL template execution with Flyway programmatic execution
    - Uses Flyway to run migrations in tenant schema

3. **services/notification-service/notification-messaging/pom.xml**
    - Added Flyway dependencies

4. **services/notification-service/notification-container/pom.xml**
    - Added `notification-email` dependency

5. **services/notification-service/notification-container/src/main/java/com/ccbsa/wms/notification/NotificationServiceApplication.java**
    - Added `com.ccbsa.wms.notification.email` to scanBasePackages

## Next Steps

1. **Rebuild Services:**
   ```bash
   mvn clean install -DskipTests -pl services/tenant-service,services/notification-service -am
   ```

2. **Restart Services:**
    - Restart tenant-service
    - Restart notification-service

3. **Test:**
    - Create a new tenant
    - Verify schema is created
    - Verify Flyway migrations run
    - Activate tenant
    - Verify welcome email is sent

## Notes

- **Schema Creation:** Happens on tenant creation (not just activation)
- **Flyway Migrations:** Run programmatically in tenant schema context
- **Email Adapter:** Now properly registered and available
- **Backward Compatibility:** Existing tenants will have schemas created when they activate (if schema wasn't created on creation)

