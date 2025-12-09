-- Flyway Migration: V2__Add_indexes.sql
-- Creates indexes for optimized query performance

-- Index on status for filtering tenants by status
CREATE INDEX IF NOT EXISTS idx_tenants_status ON tenants(status);

-- Index on keycloak_realm_name for realm-based lookups (partial index for non-null values)
CREATE INDEX IF NOT EXISTS idx_tenants_keycloak_realm ON tenants(keycloak_realm_name)
    WHERE keycloak_realm_name IS NOT NULL;

-- Index on name for search operations
CREATE INDEX IF NOT EXISTS idx_tenants_name ON tenants(name);

-- Composite index for common query patterns (status and created_at)
CREATE INDEX IF NOT EXISTS idx_tenants_status_created_at ON tenants(status, created_at DESC);
