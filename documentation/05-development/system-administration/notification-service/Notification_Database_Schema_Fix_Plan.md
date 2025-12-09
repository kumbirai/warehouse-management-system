# Notification Database Schema Fix Plan

## Issues Identified

### 1. Schema Name Mismatch

- **Tenant.java** creates schema name: `tenant_{tenant_id}` (e.g., `tenant_qui_ea_eum`)
- **TenantSchemaResolver** expects: `tenant_{sanitized_tenant_id}_schema` (e.g., `tenant_qui_ea_eum_schema`)
- **Mismatch:** Missing `_schema` suffix and different sanitization logic

### 2. Tables in Wrong Schema

- Current: Tables created in `public` schema by Flyway
- Expected: Tables in tenant-specific schemas (e.g., `tenant_qui_ea_eum_schema`)
- Problem: Flyway runs at startup (no tenant context) → creates in public schema

### 3. No Schema Creation Mechanism

- Tenant-service publishes `TenantSchemaCreatedEvent` but doesn't create schemas in other service databases
- Each service must create its own schemas and tables when tenant is activated

### 4. Data Isolation Violation

- All tenant data in `public` schema
- Tenant IDs in notifications don't match actual tenant IDs
- No proper multi-tenant isolation

## Solution

### Step 1: Fix Schema Naming Convention

**Fix Tenant.java** to match TenantSchemaResolver:

- Change `resolveSchemaName()` to return `tenant_{sanitized_tenant_id}_schema`
- Use same sanitization logic as TenantSchemaResolver

### Step 2: Modify Flyway Migrations

**Option A: Empty Migrations (Recommended for MVP)**

- Migrations should NOT create tables in public schema
- Tables created programmatically in tenant schemas when TenantSchemaCreatedEvent is received

**Option B: Schema-Aware Migrations**

- Migrations create tables conditionally (only if schema exists)
- Requires Flyway schema parameter configuration

**Recommendation:** Option A - simpler and cleaner for MVP

### Step 3: Create TenantSchemaCreatedEventListener

**Responsibilities:**

1. Listen to `TenantSchemaCreatedEvent` from tenant-service
2. Extract tenant ID and schema name from event
3. Create schema in notification database: `CREATE SCHEMA IF NOT EXISTS {schema_name}`
4. Create tables in tenant schema using DDL from migrations
5. Create indexes in tenant schema
6. Record in Flyway history table (in tenant schema)

### Step 4: Database Reset

1. Drop `wms_notification_db`
2. Recreate empty database
3. Start notification-service (Flyway will run but won't create tables in public)
4. Activate tenant (creates schema and publishes event)
5. Notification-service creates tables in tenant schema

## Implementation Files

1. **Fix Tenant.java:** `services/tenant-service/tenant-domain/tenant-domain-core/src/main/java/com/ccbsa/wms/tenant/domain/core/entity/Tenant.java`
    - Update `resolveSchemaName()` to match TenantSchemaResolver format

2. **Modify Migrations:** `services/notification-service/notification-dataaccess/src/main/resources/db/migration/`
    - V1: Remove table creation (or make conditional)
    - V2: Remove index creation (or make conditional)

3. **Create Listener:** `services/notification-service/notification-messaging/src/main/java/com/ccbsa/wms/notification/messaging/listener/TenantSchemaCreatedEventListener.java`
    - Listen to TenantSchemaCreatedEvent
    - Create schema and tables programmatically

4. **Database Reset Script:** `scripts/reset-notification-db.sh` (already created)

## Testing Plan

1. Run `./scripts/reset-notification-db.sh`
2. Start notification-service
3. Verify public schema is empty (no notifications table)
4. Create and activate tenant via tenant-service
5. Verify tenant schema created in notification database
6. Verify tables created in tenant schema
7. Create notification for tenant
8. Verify notification in correct tenant schema
9. Verify tenant isolation (tenant A cannot see tenant B's data)

