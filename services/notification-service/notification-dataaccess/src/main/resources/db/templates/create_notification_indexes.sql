-- Template: create_notification_indexes.sql
-- Description: SQL template for creating notification indexes in tenant schemas
-- Date: 2025-01
-- 
-- This template is used programmatically by TenantSchemaCreatedEventListener
-- to create indexes in tenant-specific schemas.
-- 
-- Usage: Execute this SQL with {schema_name} replaced with actual tenant schema name

-- Single column indexes for common filter conditions
CREATE INDEX IF NOT EXISTS idx_notifications_tenant_id ON {schema_name}.notifications(tenant_id);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_user_id ON {schema_name}.notifications(recipient_user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON {schema_name}.notifications(status);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON {schema_name}.notifications(type);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON {schema_name}.notifications(created_at DESC);

-- Composite indexes for common query patterns
-- Optimizes: findByTenantIdAndRecipientUserId
CREATE INDEX IF NOT EXISTS idx_notifications_tenant_recipient ON {schema_name}.notifications(tenant_id, recipient_user_id);

-- Optimizes: findByTenantIdAndRecipientUserIdAndStatus and countUnreadByTenantIdAndRecipientUserId
CREATE INDEX IF NOT EXISTS idx_notifications_tenant_recipient_status ON {schema_name}.notifications(tenant_id, recipient_user_id, status);

-- Optimizes: findByTenantIdAndType
CREATE INDEX IF NOT EXISTS idx_notifications_tenant_type ON {schema_name}.notifications(tenant_id, type);

