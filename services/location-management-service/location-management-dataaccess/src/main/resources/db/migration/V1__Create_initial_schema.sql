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
    id
    UUID
    PRIMARY
    KEY,
    tenant_id
    VARCHAR
(
    255
) NOT NULL,
    barcode VARCHAR
(
    255
) NOT NULL,
    code VARCHAR
(
    100
),
    name VARCHAR
(
    255
),
    type VARCHAR
(
    50
),
    zone VARCHAR
(
    100
) NOT NULL,
    aisle VARCHAR
(
    100
) NOT NULL,
    rack VARCHAR
(
    100
) NOT NULL,
    level VARCHAR
(
    100
) NOT NULL,
    status VARCHAR
(
    50
) NOT NULL,
    current_quantity DECIMAL
(
    18,
    2
) DEFAULT 0,
    maximum_quantity DECIMAL
(
    18,
    2
),
    description VARCHAR
(
    500
),
    parent_location_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_location_status CHECK
(
    status
    IN
(
    'AVAILABLE',
    'OCCUPIED',
    'RESERVED',
    'BLOCKED'
)),
    CONSTRAINT uk_locations_tenant_barcode UNIQUE
(
    tenant_id,
    barcode
)
    );

-- Add unique constraint for tenant_id and code combination (code must be unique per tenant when provided)
CREATE UNIQUE INDEX IF NOT EXISTS uk_locations_tenant_code ON locations (tenant_id, code) WHERE code IS NOT NULL;

-- Add table comment
COMMENT
ON TABLE locations IS 'Validation table for Hibernate schema validation. NOT used at runtime - all operations use tenant-specific schemas.';

-- Add column comments
COMMENT
ON COLUMN locations.id IS 'Unique location identifier (UUID)';
COMMENT
ON COLUMN locations.tenant_id IS 'Tenant identifier (LDP identifier). Validated at application layer.';
COMMENT
ON COLUMN locations.barcode IS 'Location barcode identifier (unique per tenant)';
COMMENT
ON COLUMN locations.code IS 'Original location code (e.g., "WH-53") - unique per tenant when provided';
COMMENT
ON COLUMN locations.name IS 'Location name/display name';
COMMENT
ON COLUMN locations.type IS 'Location type: WAREHOUSE, ZONE, AISLE, RACK, BIN';
COMMENT
ON COLUMN locations.zone IS 'Warehouse zone identifier';
COMMENT
ON COLUMN locations.aisle IS 'Aisle identifier within the zone';
COMMENT
ON COLUMN locations.rack IS 'Rack identifier within the aisle';
COMMENT
ON COLUMN locations.level IS 'Level identifier within the rack';
COMMENT
ON COLUMN locations.status IS 'Location status: AVAILABLE, OCCUPIED, RESERVED, BLOCKED';
COMMENT
ON COLUMN locations.current_quantity IS 'Current quantity stored in the location';
COMMENT
ON COLUMN locations.maximum_quantity IS 'Maximum quantity capacity (null if unlimited)';
COMMENT
ON COLUMN locations.description IS 'Optional location description';
COMMENT
ON COLUMN locations.parent_location_id IS 'Parent location identifier for hierarchical relationships. NULL for warehouse locations (root of hierarchy).';
COMMENT
ON COLUMN locations.created_at IS 'Timestamp when location was created';
COMMENT
ON COLUMN locations.last_modified_at IS 'Timestamp when location was last modified';
COMMENT
ON COLUMN locations.version IS 'Optimistic locking version for concurrency control';

-- Create stock_movements table
CREATE TABLE IF NOT EXISTS stock_movements
(
    id
    UUID
    PRIMARY
    KEY,
    tenant_id
    VARCHAR
(
    255
) NOT NULL,
    stock_item_id VARCHAR
(
    255
) NOT NULL,
    product_id UUID NOT NULL,
    source_location_id UUID NOT NULL,
    destination_location_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK
(
    quantity >
    0
),
    movement_type VARCHAR
(
    50
) NOT NULL,
    reason VARCHAR
(
    50
) NOT NULL,
    status VARCHAR
(
    20
) NOT NULL,
    initiated_by UUID NOT NULL,
    initiated_at TIMESTAMP NOT NULL,
    completed_by UUID,
    completed_at TIMESTAMP,
    cancelled_by UUID,
    cancelled_at TIMESTAMP,
    cancellation_reason VARCHAR
(
    500
),
    created_at TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_different_locations CHECK
(
    source_location_id
    !=
    destination_location_id
),
    CONSTRAINT chk_completed_state CHECK
(
(
    status =
    'COMPLETED'
    AND
    completed_by
    IS
    NOT
    NULL
    AND
    completed_at
    IS
    NOT
    NULL
) OR
(
    status
    !=
    'COMPLETED'
    AND
    completed_by
    IS
    NULL
    AND
    completed_at
    IS
    NULL
)
    ),
    CONSTRAINT chk_cancelled_state CHECK
(
(
    status =
    'CANCELLED'
    AND
    cancelled_by
    IS
    NOT
    NULL
    AND
    cancelled_at
    IS
    NOT
    NULL
    AND
    cancellation_reason
    IS
    NOT
    NULL
) OR
(
    status
    !=
    'CANCELLED'
    AND
    cancelled_by
    IS
    NULL
    AND
    cancelled_at
    IS
    NULL
    AND
    cancellation_reason
    IS
    NULL
)
    )
    );

-- Add table comment for stock_movements table
COMMENT
ON TABLE stock_movements IS 'Validation table for stock movements. NOT used at runtime - all operations use tenant-specific schemas.';

-- Add column comments for stock_movements table
COMMENT
ON COLUMN stock_movements.id IS 'Unique movement identifier (UUID)';
COMMENT
ON COLUMN stock_movements.tenant_id IS 'Tenant identifier (LDP identifier). Validated at application layer.';
COMMENT
ON COLUMN stock_movements.stock_item_id IS 'Stock item identifier';
COMMENT
ON COLUMN stock_movements.product_id IS 'Product identifier';
COMMENT
ON COLUMN stock_movements.source_location_id IS 'Source location identifier';
COMMENT
ON COLUMN stock_movements.destination_location_id IS 'Destination location identifier';
COMMENT
ON COLUMN stock_movements.quantity IS 'Movement quantity (must be positive)';
COMMENT
ON COLUMN stock_movements.movement_type IS 'Movement type';
COMMENT
ON COLUMN stock_movements.reason IS 'Movement reason';
COMMENT
ON COLUMN stock_movements.status IS 'Movement status';
COMMENT
ON COLUMN stock_movements.initiated_by IS 'User identifier who initiated the movement';
COMMENT
ON COLUMN stock_movements.initiated_at IS 'Timestamp when movement was initiated';
COMMENT
ON COLUMN stock_movements.completed_by IS 'User identifier who completed the movement (nullable)';
COMMENT
ON COLUMN stock_movements.completed_at IS 'Timestamp when movement was completed (nullable)';
COMMENT
ON COLUMN stock_movements.cancelled_by IS 'User identifier who cancelled the movement (nullable)';
COMMENT
ON COLUMN stock_movements.cancelled_at IS 'Timestamp when movement was cancelled (nullable)';
COMMENT
ON COLUMN stock_movements.cancellation_reason IS 'Cancellation reason (nullable)';
COMMENT
ON COLUMN stock_movements.created_at IS 'Timestamp when movement was created';
COMMENT
ON COLUMN stock_movements.last_modified_at IS 'Timestamp when movement was last modified';
COMMENT
ON COLUMN stock_movements.version IS 'Optimistic locking version for concurrency control';
