-- Flyway Migration: V1__Create_initial_schema.sql
-- Description: Creates the initial database schema for the location management service
-- Date: 2025-01
-- 
-- Note: Location service is tenant-aware. Uses schema-per-tenant strategy via TenantAwarePhysicalNamingStrategy.
-- Each tenant has its own isolated PostgreSQL schema for multi-tenant isolation.
-- Tenant validation is performed at the application layer, not via database foreign keys,
-- since each service maintains its own database (microservices architecture).
--
-- IMPORTANT: This migration creates a validation table in the public schema for Hibernate
-- schema validation at startup. This table is NOT used at runtime - all runtime operations
-- use tenant-specific schemas created programmatically when TenantSchemaCreatedEvent is received.
--
-- The actual table creation SQL for tenant schemas is in: db/templates/create_location_tables.sql
-- Tables are created in tenant schemas by TenantSchemaCreatedEventListener when tenant-service
-- publishes TenantSchemaCreatedEvent.

-- Create validation table in public schema for Hibernate schema validation
-- This table is only used during application startup for schema validation.
-- At runtime, all operations use tenant-specific schemas (tenant_{tenant_id}_schema).
CREATE TABLE IF NOT EXISTS locations
(
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    barcode VARCHAR(255) NOT NULL,
    zone VARCHAR(100) NOT NULL,
    aisle VARCHAR(100) NOT NULL,
    rack VARCHAR(100) NOT NULL,
    level VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    current_quantity DECIMAL(18, 2) DEFAULT 0,
    maximum_quantity DECIMAL(18, 2),
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_location_status CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'BLOCKED')),
    CONSTRAINT uk_locations_tenant_barcode UNIQUE (tenant_id, barcode)
);

-- Add table comment
COMMENT ON TABLE locations IS 'Validation table for Hibernate schema validation. NOT used at runtime - all operations use tenant-specific schemas.';

-- Add column comments
COMMENT ON COLUMN locations.id IS 'Unique location identifier (UUID)';
COMMENT ON COLUMN locations.tenant_id IS 'Tenant identifier (LDP identifier). Validated at application layer.';
COMMENT ON COLUMN locations.barcode IS 'Location barcode identifier (unique per tenant)';
COMMENT ON COLUMN locations.zone IS 'Warehouse zone identifier';
COMMENT ON COLUMN locations.aisle IS 'Aisle identifier within the zone';
COMMENT ON COLUMN locations.rack IS 'Rack identifier within the aisle';
COMMENT ON COLUMN locations.level IS 'Level identifier within the rack';
COMMENT ON COLUMN locations.status IS 'Location status: AVAILABLE, OCCUPIED, RESERVED, BLOCKED';
COMMENT ON COLUMN locations.current_quantity IS 'Current quantity stored in the location';
COMMENT ON COLUMN locations.maximum_quantity IS 'Maximum quantity capacity (null if unlimited)';
COMMENT ON COLUMN locations.description IS 'Optional location description';
COMMENT ON COLUMN locations.created_at IS 'Timestamp when location was created';
COMMENT ON COLUMN locations.last_modified_at IS 'Timestamp when location was last modified';
COMMENT ON COLUMN locations.version IS 'Optimistic locking version for concurrency control';
