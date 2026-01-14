-- Flyway Migration: V001__create_returns_tables.sql
-- Description: Creates the returns and return_line_items tables for the returns service
-- Date: 2025-01
-- 
-- Note: Returns service is tenant-aware. Uses schema-per-tenant strategy via TenantAwarePhysicalNamingStrategy.
-- Each tenant has its own isolated PostgreSQL schema for multi-tenant isolation.
--
-- IMPORTANT: This migration creates validation tables in the public schema for Hibernate
-- schema validation at startup. This table is NOT used at runtime - all runtime operations
-- use tenant-specific schemas created programmatically when TenantSchemaCreatedEvent is received.

-- Create returns table
CREATE TABLE IF NOT EXISTS returns
(
    return_id
    UUID
    PRIMARY
    KEY,
    tenant_id
    VARCHAR
(
    255
) NOT NULL,
    order_number VARCHAR
(
    50
) NOT NULL,
    return_type VARCHAR
(
    50
) NOT NULL,
    return_status VARCHAR
(
    50
) NOT NULL,
    customer_signature TEXT,
    signature_timestamp TIMESTAMP,
    primary_return_reason VARCHAR
(
    50
),
    return_notes VARCHAR
(
    2000
),
    returned_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_returns_tenant_order UNIQUE
(
    tenant_id,
    order_number,
    return_id
)
    );

-- Create return_line_items table
CREATE TABLE IF NOT EXISTS return_line_items
(
    line_item_id
    UUID
    PRIMARY
    KEY,
    return_id
    UUID
    NOT
    NULL,
    product_id
    UUID
    NOT
    NULL,
    ordered_quantity
    INTEGER
    NOT
    NULL,
    picked_quantity
    INTEGER
    NOT
    NULL,
    accepted_quantity
    INTEGER
    NOT
    NULL,
    returned_quantity
    INTEGER
    NOT
    NULL,
    product_condition
    VARCHAR
(
    50
),
    return_reason VARCHAR
(
    50
),
    line_notes VARCHAR
(
    1000
),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_return_line_items_return FOREIGN KEY
(
    return_id
) REFERENCES returns
(
    return_id
) ON DELETE CASCADE
    );

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_returns_tenant_status ON returns (tenant_id, return_status);
CREATE INDEX IF NOT EXISTS idx_returns_tenant_order ON returns (tenant_id, order_number);
CREATE INDEX IF NOT EXISTS idx_return_line_items_return ON return_line_items (return_id);
CREATE INDEX IF NOT EXISTS idx_return_line_items_product ON return_line_items (product_id);
