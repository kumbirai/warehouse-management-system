-- Flyway Migration: V2__Add_indexes.sql
-- Description: Creates indexes for performance optimization on the notifications table
-- Date: 2025-01
--
-- IMPORTANT: This migration creates indexes in the public schema for Hibernate schema validation.
-- These indexes are NOT used at runtime - all runtime operations use tenant-specific schemas
-- created programmatically when TenantSchemaCreatedEvent is received.
--
-- The actual index creation SQL for tenant schemas is in: db/templates/create_notification_indexes.sql
-- Indexes are created in tenant schemas by TenantSchemaCreatedEventListener when tenant-service
-- publishes TenantSchemaCreatedEvent.

-- Create indexes in public schema for Hibernate schema validation
-- These indexes are only used during application startup for schema validation.
-- At runtime, all operations use tenant-specific schemas (tenant_{tenant_id}_schema).

-- Single column indexes for common filter conditions
CREATE INDEX IF NOT EXISTS idx_notifications_tenant_id ON notifications(tenant_id);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_user_id ON notifications(recipient_user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(type);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC);

-- Composite indexes for common query patterns
-- Optimizes: findByTenantIdAndRecipientUserId
CREATE INDEX IF NOT EXISTS idx_notifications_tenant_recipient ON notifications(tenant_id, recipient_user_id);

-- Optimizes: findByTenantIdAndRecipientUserIdAndStatus and countUnreadByTenantIdAndRecipientUserId
CREATE INDEX IF NOT EXISTS idx_notifications_tenant_recipient_status ON notifications(tenant_id, recipient_user_id, status);

-- Optimizes: findByTenantIdAndType
CREATE INDEX IF NOT EXISTS idx_notifications_tenant_type ON notifications(tenant_id, type);
