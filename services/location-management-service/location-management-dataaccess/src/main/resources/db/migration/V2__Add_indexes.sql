-- Flyway Migration: V2__Add_indexes.sql
-- Description: Creates indexes for performance optimization on the locations table
-- Date: 2025-01
-- 
-- Note: These indexes are created in the public schema for validation purposes.
-- At runtime, equivalent indexes are created in tenant-specific schemas when
-- TenantSchemaCreatedEventListener processes TenantSchemaCreatedEvent.

-- Create indexes for performance optimization
CREATE INDEX IF NOT EXISTS idx_locations_tenant_id ON locations (tenant_id);
CREATE INDEX IF NOT EXISTS idx_locations_barcode ON locations (barcode);
CREATE INDEX IF NOT EXISTS idx_locations_coordinates ON locations (zone, aisle, rack, level);
CREATE INDEX IF NOT EXISTS idx_locations_status ON locations (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_locations_type ON locations (tenant_id, type) WHERE type IS NOT NULL;
