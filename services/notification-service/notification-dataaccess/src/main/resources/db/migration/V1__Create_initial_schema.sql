-- Flyway Migration: V1__Create_initial_schema.sql
-- Description: Creates the initial database schema for the notification service
-- Date: 2025-01
-- 
-- Note: Notification service is tenant-aware. Uses schema-per-tenant strategy via TenantSchemaResolver.
-- Each tenant has its own isolated PostgreSQL schema for multi-tenant isolation.
-- Tenant validation is performed at the application layer, not via database foreign keys,
-- since each service maintains its own database (microservices architecture).
--
-- IMPORTANT: This migration creates a validation table in the public schema for Hibernate
-- schema validation at startup. This table is NOT used at runtime - all runtime operations
-- use tenant-specific schemas created programmatically when TenantSchemaCreatedEvent is received.
--
-- The actual table creation SQL for tenant schemas is in: db/templates/create_notification_tables.sql
-- Tables are created in tenant schemas by TenantSchemaCreatedEventListener when tenant-service
-- publishes TenantSchemaCreatedEvent.

-- Create validation table in public schema for Hibernate schema validation
-- This table is only used during application startup for schema validation.
-- At runtime, all operations use tenant-specific schemas (tenant_{tenant_id}_schema).
--
-- Note: When Flyway runs with .schemas(schemaName), it automatically sets the search_path
-- to that schema. Tables created without schema qualifier will be created in the current
-- schema (the one specified in Flyway configuration).
CREATE TABLE IF NOT EXISTS notifications
(
    id
    UUID
    PRIMARY
    KEY,
    tenant_id
    VARCHAR
(
    50
) NOT NULL,
    recipient_user_id VARCHAR
(
    50
) NOT NULL,
    recipient_email VARCHAR
(
    255
),
    title VARCHAR
(
    200
) NOT NULL,
    message VARCHAR
(
    1000
) NOT NULL,
    type VARCHAR
(
    50
) NOT NULL,
    status VARCHAR
(
    20
) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP,
    sent_at TIMESTAMP,
    read_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_notification_status CHECK
(
    status
    IN
(
    'PENDING',
    'SENT',
    'DELIVERED',
    'FAILED',
    'READ'
)),
    CONSTRAINT chk_notification_type CHECK
(
    type
    IN
(
    'USER_CREATED',
    'USER_UPDATED',
    'USER_DEACTIVATED',
    'USER_ACTIVATED',
    'USER_SUSPENDED',
    'USER_ROLE_ASSIGNED',
    'USER_ROLE_REMOVED',
    'TENANT_CREATED',
    'TENANT_ACTIVATED',
    'TENANT_DEACTIVATED',
    'TENANT_SUSPENDED',
    'WELCOME',
    'SYSTEM_ALERT'
))
    );

-- Add table comment
COMMENT
ON TABLE notifications IS 'Validation table for Hibernate schema validation. NOT used at runtime - all operations use tenant-specific schemas.';

-- Add column comments
COMMENT
ON COLUMN notifications.id IS 'Unique notification identifier (UUID)';
COMMENT
ON COLUMN notifications.tenant_id IS 'Tenant identifier (LDP identifier). Validated at application layer.';
COMMENT
ON COLUMN notifications.recipient_user_id IS 'Recipient user identifier';
COMMENT
ON COLUMN notifications.recipient_email IS 'Recipient email address from event payload. Stored for efficient delivery without service calls.';
COMMENT
ON COLUMN notifications.title IS 'Notification title';
COMMENT
ON COLUMN notifications.message IS 'Notification message content';
COMMENT
ON COLUMN notifications.type IS 'Notification type: USER_CREATED, USER_UPDATED, USER_DEACTIVATED, USER_ACTIVATED, USER_SUSPENDED, USER_ROLE_ASSIGNED, USER_ROLE_REMOVED, TENANT_CREATED, TENANT_ACTIVATED, TENANT_DEACTIVATED, TENANT_SUSPENDED, WELCOME, SYSTEM_ALERT';
COMMENT
ON COLUMN notifications.status IS 'Notification status: PENDING, SENT, DELIVERED, FAILED, READ';
COMMENT
ON COLUMN notifications.created_at IS 'Timestamp when notification was created';
COMMENT
ON COLUMN notifications.last_modified_at IS 'Timestamp when notification was last modified';
COMMENT
ON COLUMN notifications.sent_at IS 'Timestamp when notification was sent';
COMMENT
ON COLUMN notifications.read_at IS 'Timestamp when notification was read by recipient';
COMMENT
ON COLUMN notifications.version IS 'Optimistic locking version for concurrency control';
