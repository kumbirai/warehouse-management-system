-- Flyway Migration: V2__Add_indexes.sql
-- Creates indexes for optimized query performance
-- 
-- Note: All indexes are created with IF NOT EXISTS to support idempotent migrations.

-- Index on tenant_id for filtering users by tenant
CREATE INDEX IF NOT EXISTS idx_users_tenant_id ON users(tenant_id);

-- Composite unique index on tenant_id and username for tenant-scoped username uniqueness
-- Usernames must be unique within a tenant, but can be reused across tenants
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_tenant_username_unique ON users(tenant_id, username);

-- Composite index on tenant_id and user_id for tenant-scoped user lookups
CREATE INDEX IF NOT EXISTS idx_users_tenant_user_id ON users(tenant_id, user_id);

-- Index on status for filtering users by status
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

-- Composite index on tenant_id and status for tenant-scoped status filtering
CREATE INDEX IF NOT EXISTS idx_users_tenant_status ON users(tenant_id, status);

-- Index on keycloak_user_id for Keycloak integration lookups (partial index for non-null values)
CREATE INDEX IF NOT EXISTS idx_users_keycloak_user_id ON users(keycloak_user_id)
    WHERE keycloak_user_id IS NOT NULL;

-- Index on email_address for email-based lookups
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email_address);
