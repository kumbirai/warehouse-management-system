-- Template: create_notification_tables.sql
-- Description: SQL template for creating notification tables in tenant schemas
-- Date: 2025-01
-- 
-- This template is used programmatically by TenantSchemaCreatedEventListener
-- to create tables in tenant-specific schemas.
-- 
-- Usage: Execute this SQL with {schema_name} replaced with actual tenant schema name
-- Example: Execute in schema "tenant_qui_ea_eum_schema"

-- Create notifications table
CREATE TABLE IF NOT EXISTS {schema_name}.notifications
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
ON TABLE {schema_name}.notifications IS 'Notification records. Tenant-aware table using schema-per-tenant isolation.';

-- Add column comments
COMMENT
ON COLUMN {schema_name}.notifications.id IS 'Unique notification identifier (UUID)';
COMMENT
ON COLUMN {schema_name}.notifications.tenant_id IS 'Tenant identifier (LDP identifier). Validated at application layer.';
COMMENT
ON COLUMN {schema_name}.notifications.recipient_user_id IS 'Recipient user identifier';
COMMENT
ON COLUMN {schema_name}.notifications.recipient_email IS 'Recipient email address from event payload. Stored for efficient delivery without service calls.';
COMMENT
ON COLUMN {schema_name}.notifications.title IS 'Notification title';
COMMENT
ON COLUMN {schema_name}.notifications.message IS 'Notification message content';
COMMENT
ON COLUMN {schema_name}.notifications.type IS 'Notification type: USER_CREATED, USER_UPDATED, USER_DEACTIVATED, USER_ACTIVATED, USER_SUSPENDED, USER_ROLE_ASSIGNED, USER_ROLE_REMOVED, TENANT_CREATED, TENANT_ACTIVATED, TENANT_DEACTIVATED, TENANT_SUSPENDED, WELCOME, SYSTEM_ALERT';
COMMENT
ON COLUMN {schema_name}.notifications.status IS 'Notification status: PENDING, SENT, DELIVERED, FAILED, READ';
COMMENT
ON COLUMN {schema_name}.notifications.created_at IS 'Timestamp when notification was created';
COMMENT
ON COLUMN {schema_name}.notifications.last_modified_at IS 'Timestamp when notification was last modified';
COMMENT
ON COLUMN {schema_name}.notifications.sent_at IS 'Timestamp when notification was sent';
COMMENT
ON COLUMN {schema_name}.notifications.read_at IS 'Timestamp when notification was read by recipient';
COMMENT
ON COLUMN {schema_name}.notifications.version IS 'Optimistic locking version for concurrency control';

