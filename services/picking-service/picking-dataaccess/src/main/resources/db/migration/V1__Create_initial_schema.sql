-- Flyway Migration: V1__Create_initial_schema.sql
-- Description: Creates the initial database schema for the picking service
-- Date: 2025-01
-- 
-- Note: Picking service is tenant-aware. Uses schema-per-tenant strategy via TenantAwarePhysicalNamingStrategy.
-- Each tenant has its own isolated PostgreSQL schema for multi-tenant isolation.
-- Tenant validation is performed at the application layer, not via database foreign keys,
-- since each service maintains its own database (microservices architecture).
--
-- IMPORTANT: This migration creates a validation table in the public schema for Hibernate
-- schema validation at startup. This table is NOT used at runtime - all runtime operations
-- use tenant-specific schemas created programmatically when TenantSchemaCreatedEvent is received.
--
-- The actual table creation SQL for tenant schemas is in: db/templates/create_picking_tables.sql
-- Tables are created in tenant schemas by TenantSchemaCreatedEventListener when tenant-service
-- publishes TenantSchemaCreatedEvent.

-- Create validation table in public schema for Hibernate schema validation
-- This table is only used during application startup for schema validation.
-- At runtime, all operations use tenant-specific schemas (tenant_{tenant_id}_schema).

-- Create picking_lists table
CREATE TABLE IF NOT EXISTS picking_lists
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
    status VARCHAR
(
    50
) NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
                               notes VARCHAR (1000),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_picking_lists_status CHECK
(
    status
    IN
(
    'RECEIVED',
    'PROCESSING',
    'PLANNED',
    'COMPLETED'
))
    );

-- Create loads table
CREATE TABLE IF NOT EXISTS loads
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
    picking_list_id UUID NOT NULL,
    load_number VARCHAR
(
    50
) NOT NULL,
    status VARCHAR
(
    50
) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    planned_at TIMESTAMP WITH TIME ZONE,
                             version BIGINT NOT NULL DEFAULT 0,
                             CONSTRAINT chk_loads_status CHECK (status IN ('CREATED', 'PLANNED', 'IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT fk_loads_picking_list FOREIGN KEY
(
    picking_list_id
) REFERENCES picking_lists
(
    id
)
                         ON DELETE CASCADE
    );

-- Create orders table
CREATE TABLE IF NOT EXISTS orders
(
    id
    UUID
    PRIMARY
    KEY,
    load_id
    UUID
    NOT
    NULL,
    order_number
    VARCHAR
(
    50
) NOT NULL,
    customer_code VARCHAR
(
    50
) NOT NULL,
    customer_name VARCHAR
(
    200
),
    priority VARCHAR
(
    20
) NOT NULL,
    status VARCHAR
(
    50
) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
                               CONSTRAINT chk_orders_priority CHECK (priority IN ('HIGH', 'NORMAL', 'LOW')),
    CONSTRAINT chk_orders_status CHECK
(
    status
    IN
(
    'PENDING',
    'IN_PROGRESS',
    'COMPLETED',
    'CANCELLED'
)),
    CONSTRAINT fk_orders_load FOREIGN KEY
(
    load_id
) REFERENCES loads
(
    id
)
                           ON DELETE CASCADE
    );

-- Create order_line_items table
CREATE TABLE IF NOT EXISTS order_line_items
(
    id
    UUID
    PRIMARY
    KEY,
    order_id
    UUID
    NOT
    NULL,
    product_code
    VARCHAR
(
    100
) NOT NULL,
    quantity INTEGER NOT NULL CHECK
(
    quantity >
    0
),
    notes VARCHAR
(
    500
),
    CONSTRAINT fk_order_line_items_order FOREIGN KEY
(
    order_id
) REFERENCES orders
(
    id
) ON DELETE CASCADE
    );

-- Create picking_tasks table
CREATE TABLE IF NOT EXISTS picking_tasks
(
    id
    UUID
    PRIMARY
    KEY,
    load_id
    UUID
    NOT
    NULL,
    order_id
    UUID
    NOT
    NULL,
    product_code
    VARCHAR
(
    100
) NOT NULL,
    location_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK
(
    quantity >
    0
),
    status VARCHAR
(
    50
) NOT NULL,
    sequence INTEGER NOT NULL CHECK
(
    sequence
    >=
    0
),
    CONSTRAINT chk_picking_tasks_status CHECK
(
    status
    IN
(
    'PENDING',
    'IN_PROGRESS',
    'COMPLETED'
))
    );

-- Add table comments
COMMENT
ON TABLE picking_lists IS 'Validation table for Hibernate schema validation. NOT used at runtime - all operations use tenant-specific schemas.';
COMMENT
ON TABLE loads IS 'Validation table for loads. NOT used at runtime - all operations use tenant-specific schemas.';
COMMENT
ON TABLE orders IS 'Validation table for orders. NOT used at runtime - all operations use tenant-specific schemas.';
COMMENT
ON TABLE order_line_items IS 'Validation table for order line items. NOT used at runtime - all operations use tenant-specific schemas.';
COMMENT
ON TABLE picking_tasks IS 'Validation table for picking tasks. NOT used at runtime - all operations use tenant-specific schemas.';

-- Add column comments for picking_lists table
COMMENT
ON COLUMN picking_lists.id IS 'Unique picking list identifier (UUID)';
COMMENT
ON COLUMN picking_lists.tenant_id IS 'Tenant identifier (LDP identifier). Validated at application layer.';
COMMENT
ON COLUMN picking_lists.status IS 'Picking list status: RECEIVED, PROCESSING, PLANNED, COMPLETED';
COMMENT
ON COLUMN picking_lists.received_at IS 'Timestamp when picking list was received';
COMMENT
ON COLUMN picking_lists.processed_at IS 'Timestamp when picking list was processed (nullable)';
COMMENT
ON COLUMN picking_lists.notes IS 'Optional notes';
COMMENT
ON COLUMN picking_lists.version IS 'Optimistic locking version for concurrency control';

-- Add column comments for loads table
COMMENT
ON COLUMN loads.id IS 'Unique load identifier (UUID)';
COMMENT
ON COLUMN loads.tenant_id IS 'Tenant identifier (LDP identifier). Validated at application layer.';
COMMENT
ON COLUMN loads.picking_list_id IS 'Foreign key to picking_lists table';
COMMENT
ON COLUMN loads.load_number IS 'Unique load number per tenant';
COMMENT
ON COLUMN loads.status IS 'Load status: CREATED, PLANNED, IN_PROGRESS, COMPLETED';
COMMENT
ON COLUMN loads.created_at IS 'Timestamp when load was created';
COMMENT
ON COLUMN loads.planned_at IS 'Timestamp when load was planned (nullable)';
COMMENT
ON COLUMN loads.version IS 'Optimistic locking version for concurrency control';

-- Add column comments for orders table
COMMENT
ON COLUMN orders.id IS 'Unique order identifier (UUID)';
COMMENT
ON COLUMN orders.load_id IS 'Foreign key to loads table';
COMMENT
ON COLUMN orders.order_number IS 'Unique order number per load';
COMMENT
ON COLUMN orders.customer_code IS 'Customer code';
COMMENT
ON COLUMN orders.customer_name IS 'Customer name (nullable)';
COMMENT
ON COLUMN orders.priority IS 'Order priority: HIGH, NORMAL, LOW';
COMMENT
ON COLUMN orders.status IS 'Order status: PENDING, IN_PROGRESS, COMPLETED, CANCELLED';
COMMENT
ON COLUMN orders.created_at IS 'Timestamp when order was created';
COMMENT
ON COLUMN orders.completed_at IS 'Timestamp when order was completed (nullable)';

-- Add column comments for order_line_items table
COMMENT
ON COLUMN order_line_items.id IS 'Unique line item identifier (UUID)';
COMMENT
ON COLUMN order_line_items.order_id IS 'Foreign key to orders table';
COMMENT
ON COLUMN order_line_items.product_code IS 'Product code';
COMMENT
ON COLUMN order_line_items.quantity IS 'Quantity (must be positive)';
COMMENT
ON COLUMN order_line_items.notes IS 'Optional notes';

-- Add column comments for picking_tasks table
COMMENT
ON COLUMN picking_tasks.id IS 'Unique picking task identifier (UUID)';
COMMENT
ON COLUMN picking_tasks.load_id IS 'Foreign key to loads table';
COMMENT
ON COLUMN picking_tasks.order_id IS 'Foreign key to orders table';
COMMENT
ON COLUMN picking_tasks.product_code IS 'Product code';
COMMENT
ON COLUMN picking_tasks.location_id IS 'Location identifier';
COMMENT
ON COLUMN picking_tasks.quantity IS 'Quantity to pick (must be positive)';
COMMENT
ON COLUMN picking_tasks.status IS 'Picking task status: PENDING, IN_PROGRESS, COMPLETED';
COMMENT
ON COLUMN picking_tasks.sequence IS 'Sequence number for picking order (must be >= 0)';
