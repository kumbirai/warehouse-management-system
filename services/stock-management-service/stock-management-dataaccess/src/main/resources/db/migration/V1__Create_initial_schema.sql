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
    allocated_quantity INTEGER NOT NULL DEFAULT 0,
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
),
    CONSTRAINT chk_stock_items_allocated_quantity_non_negative CHECK
(
    allocated_quantity
    >=
    0
),
    CONSTRAINT chk_stock_items_allocated_quantity_not_exceed_total CHECK
(
    allocated_quantity
    <=
    quantity
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
ON COLUMN stock_items.allocated_quantity IS 'Quantity allocated for picking orders (must be <= quantity)';
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

-- Create stock_allocations table
CREATE TABLE IF NOT EXISTS stock_allocations
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
    stock_item_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK
(
    quantity >
    0
),
    allocation_type VARCHAR
(
    50
) NOT NULL,
    reference_id VARCHAR
(
    255
),
    status VARCHAR
(
    20
) NOT NULL,
    allocated_by UUID NOT NULL,
    allocated_at TIMESTAMP NOT NULL,
    released_at TIMESTAMP,
    notes VARCHAR
(
    1000
),
    created_at TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
    );

-- Create stock_adjustments table
CREATE TABLE IF NOT EXISTS stock_adjustments
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
    stock_item_id UUID,
    adjustment_type VARCHAR
(
    50
) NOT NULL,
    quantity INTEGER NOT NULL CHECK
(
    quantity >
    0
),
    reason VARCHAR
(
    50
) NOT NULL,
    notes VARCHAR
(
    1000
),
    adjusted_by UUID NOT NULL,
    authorization_code VARCHAR
(
    255
),
    adjusted_at TIMESTAMP NOT NULL,
    quantity_before INTEGER NOT NULL,
    quantity_after INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
    );

-- Create stock_level_thresholds table
CREATE TABLE IF NOT EXISTS stock_level_thresholds
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
    minimum_quantity NUMERIC
(
    18,
    2
),
    maximum_quantity NUMERIC
(
    18,
    2
),
    enable_auto_restock BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_min_max_threshold CHECK
(
(
    minimum_quantity
    IS
    NULL
    AND
    maximum_quantity
    IS
    NULL
) OR
(
    minimum_quantity
    IS
    NULL
) OR
(
    maximum_quantity
    IS
    NULL
) OR
(
    minimum_quantity <
    maximum_quantity
)
    ),
    CONSTRAINT uk_stock_level_thresholds_tenant_product_location UNIQUE
(
    tenant_id,
    product_id,
    location_id
)
    );

-- Add table comments for new tables
COMMENT
ON TABLE stock_allocations IS 'Validation table for stock allocations. NOT used at runtime - all operations use tenant-specific schemas.';
COMMENT
ON TABLE stock_adjustments IS 'Validation table for stock adjustments. NOT used at runtime - all operations use tenant-specific schemas.';
COMMENT
ON TABLE stock_level_thresholds IS 'Validation table for stock level thresholds. NOT used at runtime - all operations use tenant-specific schemas.';

-- Add column comments for stock_allocations table
COMMENT
ON COLUMN stock_allocations.id IS 'Unique allocation identifier (UUID)';
COMMENT
ON COLUMN stock_allocations.tenant_id IS 'Tenant identifier (LDP identifier). Validated at application layer.';
COMMENT
ON COLUMN stock_allocations.product_id IS 'Product identifier';
COMMENT
ON COLUMN stock_allocations.location_id IS 'Location identifier (nullable)';
COMMENT
ON COLUMN stock_allocations.stock_item_id IS 'Stock item identifier';
COMMENT
ON COLUMN stock_allocations.quantity IS 'Allocated quantity (must be positive)';
COMMENT
ON COLUMN stock_allocations.allocation_type IS 'Allocation type';
COMMENT
ON COLUMN stock_allocations.reference_id IS 'Reference identifier (nullable)';
COMMENT
ON COLUMN stock_allocations.status IS 'Allocation status';
COMMENT
ON COLUMN stock_allocations.allocated_by IS 'User identifier who allocated the stock';
COMMENT
ON COLUMN stock_allocations.allocated_at IS 'Timestamp when allocation was made';
COMMENT
ON COLUMN stock_allocations.released_at IS 'Timestamp when allocation was released (nullable)';
COMMENT
ON COLUMN stock_allocations.notes IS 'Optional notes';
COMMENT
ON COLUMN stock_allocations.created_at IS 'Timestamp when allocation was created';
COMMENT
ON COLUMN stock_allocations.last_modified_at IS 'Timestamp when allocation was last modified';
COMMENT
ON COLUMN stock_allocations.version IS 'Optimistic locking version for concurrency control';

-- Add column comments for stock_adjustments table
COMMENT
ON COLUMN stock_adjustments.id IS 'Unique adjustment identifier (UUID)';
COMMENT
ON COLUMN stock_adjustments.tenant_id IS 'Tenant identifier (LDP identifier). Validated at application layer.';
COMMENT
ON COLUMN stock_adjustments.product_id IS 'Product identifier';
COMMENT
ON COLUMN stock_adjustments.location_id IS 'Location identifier (nullable)';
COMMENT
ON COLUMN stock_adjustments.stock_item_id IS 'Stock item identifier (nullable)';
COMMENT
ON COLUMN stock_adjustments.adjustment_type IS 'Adjustment type';
COMMENT
ON COLUMN stock_adjustments.quantity IS 'Adjustment quantity (must be positive)';
COMMENT
ON COLUMN stock_adjustments.reason IS 'Adjustment reason';
COMMENT
ON COLUMN stock_adjustments.notes IS 'Optional notes';
COMMENT
ON COLUMN stock_adjustments.adjusted_by IS 'User identifier who made the adjustment';
COMMENT
ON COLUMN stock_adjustments.authorization_code IS 'Authorization code (nullable)';
COMMENT
ON COLUMN stock_adjustments.adjusted_at IS 'Timestamp when adjustment was made';
COMMENT
ON COLUMN stock_adjustments.quantity_before IS 'Quantity before adjustment';
COMMENT
ON COLUMN stock_adjustments.quantity_after IS 'Quantity after adjustment';
COMMENT
ON COLUMN stock_adjustments.created_at IS 'Timestamp when adjustment was created';
COMMENT
ON COLUMN stock_adjustments.last_modified_at IS 'Timestamp when adjustment was last modified';
COMMENT
ON COLUMN stock_adjustments.version IS 'Optimistic locking version for concurrency control';

-- Add column comments for stock_level_thresholds table
COMMENT
ON COLUMN stock_level_thresholds.id IS 'Unique threshold identifier (UUID)';
COMMENT
ON COLUMN stock_level_thresholds.tenant_id IS 'Tenant identifier (LDP identifier). Validated at application layer.';
COMMENT
ON COLUMN stock_level_thresholds.product_id IS 'Product identifier';
COMMENT
ON COLUMN stock_level_thresholds.location_id IS 'Location identifier (nullable)';
COMMENT
ON COLUMN stock_level_thresholds.minimum_quantity IS 'Minimum quantity threshold (nullable)';
COMMENT
ON COLUMN stock_level_thresholds.maximum_quantity IS 'Maximum quantity threshold (nullable)';
COMMENT
ON COLUMN stock_level_thresholds.enable_auto_restock IS 'Enable automatic restock when below minimum';
COMMENT
ON COLUMN stock_level_thresholds.created_at IS 'Timestamp when threshold was created';
COMMENT
ON COLUMN stock_level_thresholds.last_modified_at IS 'Timestamp when threshold was last modified';
COMMENT
ON COLUMN stock_level_thresholds.version IS 'Optimistic locking version for concurrency control';

-- Create restock_requests table
CREATE TABLE IF NOT EXISTS restock_requests
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
    current_quantity DECIMAL
(
    18,
    2
) NOT NULL,
    minimum_quantity DECIMAL
(
    18,
    2
) NOT NULL,
    maximum_quantity DECIMAL
(
    18,
    2
),
    requested_quantity DECIMAL
(
    18,
    2
) NOT NULL,
    priority VARCHAR
(
    50
) NOT NULL,
    status VARCHAR
(
    50
) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    sent_to_d365_at TIMESTAMP,
    d365_order_reference VARCHAR
(
    255
),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_restock_requests_priority CHECK
(
    priority
    IN
(
    'HIGH',
    'MEDIUM',
    'LOW'
)),
    CONSTRAINT chk_restock_requests_status CHECK
(
    status
    IN
(
    'PENDING',
    'SENT_TO_D365',
    'FULFILLED',
    'CANCELLED'
))
    );

-- Add table comment for restock_requests
COMMENT
ON TABLE restock_requests IS 'Validation table for restock requests. NOT used at runtime - all operations use tenant-specific schemas.';

-- Add column comments for restock_requests table
COMMENT
ON COLUMN restock_requests.id IS 'Unique restock request identifier (UUID)';
COMMENT
ON COLUMN restock_requests.tenant_id IS 'Tenant identifier (LDP identifier). Validated at application layer.';
COMMENT
ON COLUMN restock_requests.product_id IS 'Product identifier';
COMMENT
ON COLUMN restock_requests.location_id IS 'Location identifier (optional - null for product-level restock)';
COMMENT
ON COLUMN restock_requests.current_quantity IS 'Current stock quantity when request was generated';
COMMENT
ON COLUMN restock_requests.minimum_quantity IS 'Minimum threshold quantity';
COMMENT
ON COLUMN restock_requests.maximum_quantity IS 'Maximum threshold quantity (optional)';
COMMENT
ON COLUMN restock_requests.requested_quantity IS 'Requested restock quantity';
COMMENT
ON COLUMN restock_requests.priority IS 'Restock priority: HIGH, MEDIUM, LOW';
COMMENT
ON COLUMN restock_requests.status IS 'Request status: PENDING, SENT_TO_D365, FULFILLED, CANCELLED';
COMMENT
ON COLUMN restock_requests.created_at IS 'Timestamp when restock request was created';
COMMENT
ON COLUMN restock_requests.sent_to_d365_at IS 'Timestamp when request was sent to Microsoft Dynamics 365';
COMMENT
ON COLUMN restock_requests.d365_order_reference IS 'D365 order reference (if sent to D365)';
COMMENT
ON COLUMN restock_requests.version IS 'Optimistic locking version for concurrency control';

