-- Flyway Migration: V1__Create_initial_schema.sql
-- Creates the initial database schema for the tenant service
-- 
-- Note: Tenant service manages tenants, so this table is NOT tenant-aware (uses public schema)

CREATE TABLE IF NOT EXISTS tenants
(
    tenant_id
    VARCHAR
(
    50
) PRIMARY KEY,
    name VARCHAR
(
    200
) NOT NULL,
    status VARCHAR
(
    20
) NOT NULL,
    email_address VARCHAR
(
    255
),
    phone VARCHAR
(
    50
),
    address VARCHAR
(
    500
),
    keycloak_realm_name VARCHAR
(
    100
),
    use_per_tenant_realm BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMP,
    deactivated_at TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT tenants_status_check CHECK
(
    status
    IN
(
    'PENDING',
    'ACTIVE',
    'INACTIVE',
    'SUSPENDED'
))
    );
