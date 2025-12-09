# Notification Database Schema-Per-Tenant Implementation Summary

## Overview

Fixed the `wms_notification_db` setup to properly implement schema-per-tenant pattern for MVP. The database was incorrectly creating tables in the `public` schema instead of
tenant-specific schemas.

## Changes Made

### 1. Fixed Schema Naming Convention

**File:** `services/tenant-service/tenant-domain/tenant-domain-core/src/main/java/com/ccbsa/wms/tenant/domain/core/entity/Tenant.java`

- Updated `resolveSchemaName()` method to match `TenantSchemaResolver` convention
- Now returns: `tenant_{sanitized_tenant_id}_schema` (with `_schema` suffix)
- Uses same sanitization logic as `TenantSchemaResolver` for consistency

### 2. Modified Flyway Migrations

**Files:**

- `services/notification-service/notification-dataaccess/src/main/resources/db/migration/V1__Create_initial_schema.sql`
- `services/notification-service/notification-dataaccess/src/main/resources/db/migration/V2__Add_indexes.sql`

- Migrations now intentionally do NOT create tables in public schema
- Tables are created programmatically in tenant schemas when `TenantSchemaCreatedEvent` is received
- Migrations are kept as no-op to satisfy Flyway baseline requirements

### 3. Created SQL Templates

**Files:**

- `services/notification-service/notification-dataaccess/src/main/resources/db/templates/create_notification_tables.sql`
- `services/notification-service/notification-dataaccess/src/main/resources/db/templates/create_notification_indexes.sql`

- SQL templates with `{schema_name}` placeholder for programmatic execution
- Templates contain the actual DDL statements for tables and indexes
- Used by `TenantSchemaCreatedEventListener` to create schema structures

### 4. Created TenantSchemaCreatedEventListener

**File:** `services/notification-service/notification-messaging/src/main/java/com/ccbsa/wms/notification/messaging/listener/TenantSchemaCreatedEventListener.java`

**Responsibilities:**

- Listens to `TenantSchemaCreatedEvent` from tenant-service
- Extracts tenant ID and schema name from event
- Creates tenant schema: `CREATE SCHEMA IF NOT EXISTS {schema_name}`
- Creates tables in tenant schema using SQL template
- Creates indexes in tenant schema using SQL template
- Handles errors gracefully with proper logging

**Dependencies Added:**

- `spring-boot-starter-jdbc` for `JdbcTemplate` support

### 5. Created Database Reset Script

**File:** `scripts/reset-notification-db.sh`

- Script to drop and recreate `wms_notification_db`
- Ensures clean database state for schema-per-tenant setup
- Includes verification steps

## How It Works

### Schema Creation Flow

1. **Tenant Activation:**
    - Tenant-service activates tenant
    - `Tenant.activate()` publishes `TenantSchemaCreatedEvent` with schema name: `tenant_{sanitized_tenant_id}_schema`

2. **Schema Setup in Notification Service:**
    - `TenantSchemaCreatedEventListener` receives event
    - Creates schema in notification database: `CREATE SCHEMA IF NOT EXISTS tenant_xxx_schema`
    - Loads SQL templates and replaces `{schema_name}` placeholder
    - Executes table creation SQL in tenant schema
    - Executes index creation SQL in tenant schema

3. **Runtime Operation:**
    - `TenantContext` is set from JWT token
    - `TenantSchemaResolver` resolves schema name from tenant context
    - `TenantAwarePhysicalNamingStrategy` replaces `tenant_schema` placeholder with actual schema
    - Hibernate queries use tenant-specific schema

## Testing Steps

1. **Reset Database:**
   ```bash
   ./scripts/reset-notification-db.sh
   ```

2. **Start Services:**
    - Start tenant-service
    - Start notification-service (Flyway will run but won't create tables in public)

3. **Verify Public Schema is Empty:**
   ```sql
   PGPASSWORD=secret psql -h localhost -p 5432 -U postgres -d wms_notification_db -c "SELECT schemaname, tablename FROM pg_tables WHERE schemaname = 'public';"
   ```
   Should show only `flyway_schema_history` table.

4. **Create and Activate Tenant:**
    - Create tenant via tenant-service API
    - Activate tenant (this publishes `TenantSchemaCreatedEvent`)

5. **Verify Tenant Schema Created:**
   ```sql
   PGPASSWORD=secret psql -h localhost -p 5432 -U postgres -d wms_notification_db -c "\dn"
   ```
   Should show tenant schema: `tenant_{tenant_id}_schema`

6. **Verify Tables in Tenant Schema:**
   ```sql
   PGPASSWORD=secret psql -h localhost -p 5432 -U postgres -d wms_notification_db -c "SELECT schemaname, tablename FROM pg_tables WHERE schemaname LIKE 'tenant_%';"
   ```
   Should show `notifications` table in tenant schema.

7. **Test Tenant Isolation:**
    - Create notification for tenant A
    - Verify notification is in tenant A's schema
    - Verify tenant B cannot see tenant A's notifications

## Files Modified

1. `services/tenant-service/tenant-domain/tenant-domain-core/src/main/java/com/ccbsa/wms/tenant/domain/core/entity/Tenant.java`
2. `services/notification-service/notification-dataaccess/src/main/resources/db/migration/V1__Create_initial_schema.sql`
3. `services/notification-service/notification-dataaccess/src/main/resources/db/migration/V2__Add_indexes.sql`
4. `services/notification-service/notification-messaging/pom.xml` (added JdbcTemplate dependency)

## Files Created

1. `services/notification-service/notification-dataaccess/src/main/resources/db/templates/create_notification_tables.sql`
2. `services/notification-service/notification-dataaccess/src/main/resources/db/templates/create_notification_indexes.sql`
3. `services/notification-service/notification-messaging/src/main/java/com/ccbsa/wms/notification/messaging/listener/TenantSchemaCreatedEventListener.java`
4. `scripts/reset-notification-db.sh`
5. `documentation/05-development/system-administration/notification-service/Notification_Database_Schema_Analysis.md`
6. `documentation/05-development/system-administration/notification-service/Notification_Database_Schema_Fix_Plan.md`
7. `documentation/05-development/system-administration/notification-service/Notification_Database_Schema_Implementation_Summary.md`

## Next Steps

1. Run database reset script
2. Test tenant activation and schema creation
3. Verify tenant isolation
4. Monitor logs for any schema creation errors
5. Apply same pattern to other tenant-aware services (user-service, etc.)

