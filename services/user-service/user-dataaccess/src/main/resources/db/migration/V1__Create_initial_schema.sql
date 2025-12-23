-- Flyway Migration: V1__Create_initial_schema.sql
-- Creates the initial database schema for the user service
-- 
-- Note: User service is tenant-aware. Uses schema-per-tenant strategy via TenantSchemaResolver.
-- Each tenant has its own isolated PostgreSQL schema for multi-tenant isolation.

-- Create users table
CREATE TABLE IF NOT EXISTS users
(
    user_id
    VARCHAR
(
    50
) PRIMARY KEY,
    tenant_id VARCHAR
(
    50
) NOT NULL,
    username VARCHAR
(
    100
) NOT NULL,
    email_address VARCHAR
(
    255
) NOT NULL,
    first_name VARCHAR
(
    100
),
    last_name VARCHAR
(
    100
),
    keycloak_user_id VARCHAR
(
    100
),
    status VARCHAR
(
    20
) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
    );

-- Add status check constraint (idempotent approach)
-- Drop constraint if it exists, then add it
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_status_check;

ALTER TABLE users
    ADD CONSTRAINT users_status_check
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'));

-- Add table comment
COMMENT
ON TABLE users IS 'User accounts and profiles. Tenant-aware table using schema-per-tenant isolation.';

-- Add column comments
COMMENT
ON COLUMN users.user_id IS 'Unique user identifier';
COMMENT
ON COLUMN users.tenant_id IS 'Tenant identifier (LDP identifier)';
COMMENT
ON COLUMN users.username IS 'Unique username for authentication within tenant';
COMMENT
ON COLUMN users.email_address IS 'User email address';
COMMENT
ON COLUMN users.first_name IS 'User first name';
COMMENT
ON COLUMN users.last_name IS 'User last name';
COMMENT
ON COLUMN users.keycloak_user_id IS 'Keycloak user identifier for IAM integration';
COMMENT
ON COLUMN users.status IS 'User status: ACTIVE, INACTIVE, or SUSPENDED';
COMMENT
ON COLUMN users.created_at IS 'Timestamp when user was created';
COMMENT
ON COLUMN users.last_modified_at IS 'Timestamp when user was last modified';
COMMENT
ON COLUMN users.version IS 'Optimistic locking version for concurrency control';
