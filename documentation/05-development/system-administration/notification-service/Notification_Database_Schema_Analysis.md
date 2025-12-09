# Notification Database Schema Analysis

## Current State Analysis

### Database: `wms_notification_db`

**Current Schema Structure:**

- **Schemas:** Only `public` schema exists
- **Tables:** `notifications` table exists in `public` schema (❌ WRONG)
- **Flyway History:** `flyway_schema_history` in `public` schema

**Current Data:**

- Notifications table contains tenant_id values that are UUIDs (e.g., `495e3da3-2e80-3353-ab56-efb8db105a33`)
- These tenant_id values do NOT match actual tenants in tenant-service
- Actual tenants have string IDs like `qui-ea-eum`, `natus-qui-laboriosam`, etc.

### Problem

1. **Schema-Per-Tenant Pattern Not Implemented:**
    - Tables are created in `public` schema instead of tenant-specific schemas
    - No tenant schemas exist (e.g., `tenant_qui_ea_eum_schema`)
    - Flyway migrations run at startup in `public` schema (no tenant context)

2. **Data Isolation Violation:**
    - All tenant data is in the same `public` schema
    - Tenant IDs in notifications don't match actual tenant IDs
    - No proper multi-tenant isolation

3. **Architecture Mismatch:**
    - Code expects schema-per-tenant (uses `TenantAwarePhysicalNamingStrategy`)
    - Database structure doesn't match (tables in public schema)

## Expected State (Schema-Per-Tenant Pattern)

### Architecture Pattern

**For MVP (except tenant-service):**

- Each tenant has its own isolated PostgreSQL schema
- Schema naming: `tenant_{sanitized_tenant_id}_schema`
- Example: `tenant_qui_ea_eum_schema`, `tenant_natus_qui_laboriosam_schema`

**Tenant-Service Exception:**

- Tenant-service uses `public` schema (manages tenants themselves)
- Not tenant-aware (no schema-per-tenant)

### Schema Creation Flow

1. **Tenant Activation:**
    - Tenant-service activates tenant
    - Tenant-service creates tenant schema: `tenant_{sanitized_tenant_id}_schema`
    - Tenant-service publishes `TenantSchemaCreatedEvent` with schema name

2. **Service Schema Setup:**
    - Each service (notification, user, etc.) listens to `TenantSchemaCreatedEvent`
    - Service creates its tables in the tenant's schema
    - Tables are isolated per tenant

3. **Runtime Operation:**
    - `TenantContext` is set from JWT token
    - `TenantSchemaResolver` resolves schema name from tenant context
    - `TenantAwarePhysicalNamingStrategy` replaces `tenant_schema` placeholder with actual schema
    - Hibernate queries use tenant-specific schema

## Solution: Database Reset and Schema-Per-Tenant Implementation

### Step 1: Drop Current Database

```bash
./scripts/setup-postgres-databases.sh drop wms_notification_db
```

### Step 2: Recreate Database

```bash
./scripts/setup-postgres-databases.sh create wms_notification_db
```

### Step 3: Modify Flyway Migrations

**Current Problem:**

- Migrations create tables in `public` schema
- Should NOT create tables in public schema for tenant-aware services

**Solution:**

- Migrations should be schema-aware
- Create tables in tenant schemas when `TenantSchemaCreatedEvent` is received
- OR: Migrations should be empty (only create structure when tenant schema is created)

### Step 4: Create Tenant Schema Listener

**Required:**

- Listener for `TenantSchemaCreatedEvent`
- Creates tables in tenant schema programmatically
- Runs migrations in tenant schema context

## Implementation Plan

1. ✅ Drop and recreate `wms_notification_db`
2. ✅ Modify Flyway migrations to be schema-aware
3. ✅ Create `TenantSchemaCreatedEventListener` to create tables in tenant schemas
4. ✅ Test with actual tenant activation

## Migration Strategy

### Option A: Programmatic Schema Creation (Recommended)

**When `TenantSchemaCreatedEvent` is received:**

1. Extract tenant ID and schema name from event
2. Set tenant context
3. Execute DDL statements in tenant schema:
    - `CREATE SCHEMA IF NOT EXISTS {schema_name}`
    - `CREATE TABLE {schema_name}.notifications (...)`
    - Create indexes
4. Record migration in Flyway history table (in tenant schema)

### Option B: Flyway Schema-Aware Migrations

**Modify Flyway configuration:**

- Use Flyway's schema parameter
- Run migrations per tenant schema
- Requires custom Flyway runner per tenant

**Recommendation:** Option A (Programmatic) is simpler and more flexible for MVP.

## Files to Modify

1. **Drop/Recreate Script:** `scripts/reset-notification-db.sh` (new)
2. **Tenant Schema Listener:**
   `services/notification-service/notification-messaging/src/main/java/com/ccbsa/wms/notification/messaging/listener/TenantSchemaCreatedEventListener.java` (new)
3. **Flyway Migrations:** Modify to be schema-aware or remove table creation
4. **Documentation:** Update setup guides

## Testing

After implementation:

1. Create a tenant via tenant-service
2. Activate tenant (should create schema and publish event)
3. Verify notification-service creates tables in tenant schema
4. Create notification for tenant
5. Verify notification is in correct tenant schema
6. Verify tenant isolation (tenant A cannot see tenant B's notifications)

