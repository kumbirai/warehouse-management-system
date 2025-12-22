-- Flyway Migration: V1__Create_initial_schema.sql
-- Description: Creates the initial database schema for the stock management service
-- Date: 2025-01
-- 
-- Note: Stock management service is tenant-aware. Uses schema-per-tenant strategy via TenantAwarePhysicalNamingStrategy.
-- Each tenant has its own isolated PostgreSQL schema for multi-tenant isolation.
-- Tenant validation is performed at the application layer, not via database foreign keys,
-- since each service maintains its own database (microservices architecture).
--
-- IMPORTANT: This migration creates a validation table in the public schema for Hibernate
-- schema validation at startup. This table is NOT used at runtime - all runtime operations
-- use tenant-specific schemas created programmatically when TenantSchemaCreatedEvent is received.
--
-- The actual table creation SQL for tenant schemas is in: db/templates/create_stock_tables.sql
-- Tables are created in tenant schemas by TenantSchemaCreatedEventListener when tenant-service
-- publishes TenantSchemaCreatedEvent.

-- Create validation table in public schema for Hibernate schema validation
-- This table is only used during application startup for schema validation.
-- At runtime, all operations use tenant-specific schemas (tenant_{tenant_id}_schema).

-- Create stock_consignments table
CREATE TABLE IF NOT EXISTS stock_consignments
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
    consignment_reference VARCHAR
(
    100
) NOT NULL,
    warehouse_id VARCHAR
(
    50
) NOT NULL,
    status VARCHAR
(
    50
) NOT NULL,
    received_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP,
    received_by VARCHAR
(
    255
),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_stock_consignments_tenant_reference UNIQUE
(
    tenant_id,
    consignment_reference
)
    );

-- Create consignment_line_items table
CREATE TABLE IF NOT EXISTS consignment_line_items
(
    id
    UUID
    PRIMARY
    KEY,
    consignment_id
    UUID
    NOT
    NULL,
    product_code
    VARCHAR
(
    50
) NOT NULL,
    quantity INTEGER NOT NULL,
    expiration_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_consignment_line_items_consignment FOREIGN KEY
(
    consignment_id
)
    REFERENCES stock_consignments
(
    id
) ON DELETE CASCADE
    );

-- Create stock_items table
CREATE TABLE IF NOT EXISTS stock_items
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
    product_id UUID NOT NULL,
    location_id UUID,
    quantity INTEGER NOT NULL,
    expiration_date DATE,
    classification VARCHAR
(
    50
) NOT NULL,
    consignment_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_stock_items_consignment FOREIGN KEY
(
    consignment_id
)
    REFERENCES stock_consignments
(
    id
) ON DELETE SET NULL,
    CONSTRAINT chk_stock_items_quantity_positive CHECK
(
    quantity >
    0
)
    );

-- Add table comments
COMMENT
ON TABLE stock_consignments IS 'Validation table for Hibernate schema validation. NOT used at runtime - all operations use tenant-specific schemas.';
COMMENT
ON TABLE consignment_line_items IS 'Validation table for consignment line items. NOT used at runtime - all operations use tenant-specific schemas.';
COMMENT
ON TABLE stock_items IS 'Validation table for stock items. NOT used at runtime - all operations use tenant-specific schemas.';

-- Add column comments for stock_consignments table
COMMENT
ON COLUMN stock_consignments.id IS 'Unique consignment identifier (UUID)';
COMMENT
ON COLUMN stock_consignments.tenant_id IS 'Tenant identifier (LDP identifier). Validated at application layer.';
COMMENT
ON COLUMN stock_consignments.consignment_reference IS 'Unique consignment reference from D365 (unique per tenant)';
COMMENT
ON COLUMN stock_consignments.warehouse_id IS 'Warehouse identifier';
COMMENT
ON COLUMN stock_consignments.status IS 'Consignment status: RECEIVED, CONFIRMED, CANCELLED';
COMMENT
ON COLUMN stock_consignments.received_at IS 'Timestamp when consignment was received';
COMMENT
ON COLUMN stock_consignments.confirmed_at IS 'Timestamp when consignment was confirmed';
COMMENT
ON COLUMN stock_consignments.received_by IS 'User identifier who received the consignment';
COMMENT
ON COLUMN stock_consignments.created_at IS 'Timestamp when consignment was created';
COMMENT
ON COLUMN stock_consignments.last_modified_at IS 'Timestamp when consignment was last modified';
COMMENT
ON COLUMN stock_consignments.version IS 'Optimistic locking version for concurrency control';

-- Add column comments for consignment_line_items table
COMMENT
ON COLUMN consignment_line_items.id IS 'Unique line item identifier (UUID)';
COMMENT
ON COLUMN consignment_line_items.consignment_id IS 'Foreign key to stock_consignments table';
COMMENT
ON COLUMN consignment_line_items.product_code IS 'Product code for the line item';
COMMENT
ON COLUMN consignment_line_items.quantity IS 'Quantity in the line item';
COMMENT
ON COLUMN consignment_line_items.expiration_date IS 'Expiration date for the line item (nullable for non-perishable)';
COMMENT
ON COLUMN consignment_line_items.created_at IS 'Timestamp when line item was created';

-- Add column comments for stock_items table
COMMENT
ON COLUMN stock_items.id IS 'Unique stock item identifier (UUID)';
COMMENT
ON COLUMN stock_items.tenant_id IS 'Tenant identifier (LDP identifier). Validated at application layer.';
COMMENT
ON COLUMN stock_items.product_id IS 'Product identifier';
COMMENT
ON COLUMN stock_items.location_id IS 'Location identifier (nullable if not yet assigned)';
COMMENT
ON COLUMN stock_items.quantity IS 'Stock quantity (must be positive)';
COMMENT
ON COLUMN stock_items.expiration_date IS 'Expiration date (nullable for non-perishable)';
COMMENT
ON COLUMN stock_items.classification IS 'Stock classification: EXPIRED, CRITICAL, NEAR_EXPIRY, NORMAL, EXTENDED_SHELF_LIFE';
COMMENT
ON COLUMN stock_items.consignment_id IS 'Reference to source consignment (nullable)';
COMMENT
ON COLUMN stock_items.created_at IS 'Timestamp when stock item was created';
COMMENT
ON COLUMN stock_items.last_modified_at IS 'Timestamp when stock item was last modified';
COMMENT
ON COLUMN stock_items.version IS 'Optimistic locking version for concurrency control';

