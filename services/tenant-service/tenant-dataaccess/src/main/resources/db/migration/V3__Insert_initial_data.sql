-- Flyway Migration: V3__Insert_initial_data.sql
-- Inserts initial reference data if needed
-- 
-- Currently no initial data is required for the tenant service.
-- This file is kept for consistency and future use.

-- Example (commented out):
-- INSERT INTO tenants (tenant_id, name, status, use_per_tenant_realm, created_at, version)
-- VALUES ('default', 'Default Tenant', 'ACTIVE', false, CURRENT_TIMESTAMP, 0)
-- ON CONFLICT (tenant_id) DO NOTHING;
