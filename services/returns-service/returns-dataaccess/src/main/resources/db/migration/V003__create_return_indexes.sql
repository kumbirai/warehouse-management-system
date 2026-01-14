-- Flyway Migration: V003__create_return_indexes.sql
-- Description: Creates additional indexes for returns service queries
-- Date: 2025-01
-- 
-- Note: This migration creates indexes in the public schema for validation.
-- Actual indexes are created in tenant schemas by TenantSchemaCreatedEventListener.

-- Additional indexes for returns table
CREATE INDEX IF NOT EXISTS idx_returns_created_at ON returns (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_returns_returned_at ON returns (returned_at DESC);

-- Additional indexes for damage_assessments table
CREATE INDEX IF NOT EXISTS idx_damage_assessments_recorded_at ON damage_assessments (recorded_at DESC);
CREATE INDEX IF NOT EXISTS idx_damage_assessments_damage_type ON damage_assessments (damage_type);
CREATE INDEX IF NOT EXISTS idx_damage_assessments_damage_severity ON damage_assessments (damage_severity);
