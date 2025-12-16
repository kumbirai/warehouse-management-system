-- Flyway Migration: V1__Create_initial_schema.sql
-- Description: Creates the initial schema for products and product_barcodes tables
-- Date: 2025-01
-- 
-- Note: Product service is tenant-aware. Uses schema-per-tenant strategy via TenantAwarePhysicalNamingStrategy.
-- Each tenant has its own isolated PostgreSQL schema for multi-tenant isolation.
-- Tenant validation is performed at the application layer, not via database foreign keys,
-- since each service maintains its own database (microservices architecture).
--
-- IMPORTANT: This migration creates a validation table in the public schema for Hibernate
-- schema validation at startup. This table is NOT used at runtime - all runtime operations
-- use tenant-specific schemas created programmatically when TenantSchemaCreatedEvent is received.
--
-- The actual table creation SQL for tenant schemas is in: db/templates/create_product_tables.sql
-- Tables are created in tenant schemas by TenantSchemaCreatedEventListener when tenant-service
-- publishes TenantSchemaCreatedEvent.

-- Create validation table in public schema for Hibernate schema validation
-- This table is only used during application startup for schema validation.
-- At runtime, all operations use tenant-specific schemas (tenant_{tenant_id}_schema).

-- Create products table
CREATE TABLE IF NOT EXISTS products
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
    product_code VARCHAR
(
    100
) NOT NULL,
    description VARCHAR
(
    500
) NOT NULL,
    primary_barcode VARCHAR
(
    255
) NOT NULL,
    primary_barcode_type VARCHAR
(
    50
) NOT NULL,
    unit_of_measure VARCHAR
(
    50
) NOT NULL,
    category VARCHAR
(
    100
),
    brand VARCHAR
(
    100
),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_unit_of_measure CHECK
(
    unit_of_measure
    IN
(
    'EA',
    'CS',
    'PK',
    'BOX',
    'PAL'
)),
    CONSTRAINT chk_barcode_type CHECK
(
    primary_barcode_type
    IN
(
    'EAN_13',
    'CODE_128',
    'UPC_A',
    'ITF_14',
    'CODE_39'
)),
    CONSTRAINT uk_products_tenant_product_code UNIQUE
(
    tenant_id,
    product_code
),
    CONSTRAINT uk_products_tenant_primary_barcode UNIQUE
(
    tenant_id,
    primary_barcode
)
    );

-- Create product_barcodes table
CREATE TABLE IF NOT EXISTS product_barcodes
(
    id
    UUID
    PRIMARY
    KEY,
    product_id
    UUID
    NOT
    NULL,
    barcode
    VARCHAR
(
    255
) NOT NULL,
    barcode_type VARCHAR
(
    50
) NOT NULL,
    CONSTRAINT fk_product_barcodes_product FOREIGN KEY
(
    product_id
) REFERENCES products
(
    id
) ON DELETE CASCADE,
    CONSTRAINT chk_barcode_type_secondary CHECK
(
    barcode_type
    IN
(
    'EAN_13',
    'CODE_128',
    'UPC_A',
    'ITF_14',
    'CODE_39'
)),
    CONSTRAINT uk_product_barcodes_barcode UNIQUE
(
    barcode
)
    );

-- Add table comments
COMMENT
ON TABLE products IS 'Validation table for Hibernate schema validation. NOT used at runtime - all operations use tenant-specific schemas.';
COMMENT
ON TABLE product_barcodes IS 'Validation table for secondary product barcodes. NOT used at runtime - all operations use tenant-specific schemas.';

-- Add column comments for products table
COMMENT
ON COLUMN products.id IS 'Unique product identifier (UUID)';
COMMENT
ON COLUMN products.tenant_id IS 'Tenant identifier (LDP identifier). Validated at application layer.';
COMMENT
ON COLUMN products.product_code IS 'Unique product code per tenant (alphanumeric with hyphens/underscores)';
COMMENT
ON COLUMN products.description IS 'Product description (max 500 characters)';
COMMENT
ON COLUMN products.primary_barcode IS 'Primary barcode identifier (unique per tenant)';
COMMENT
ON COLUMN products.primary_barcode_type IS 'Barcode type: EAN_13, CODE_128, UPC_A, ITF_14, CODE_39';
COMMENT
ON COLUMN products.unit_of_measure IS 'Unit of measure: EA, CS, PK, BOX, PAL';
COMMENT
ON COLUMN products.category IS 'Product category (optional)';
COMMENT
ON COLUMN products.brand IS 'Product brand (optional)';
COMMENT
ON COLUMN products.created_at IS 'Timestamp when product was created';
COMMENT
ON COLUMN products.last_modified_at IS 'Timestamp when product was last modified';
COMMENT
ON COLUMN products.version IS 'Optimistic locking version for concurrency control';

-- Add column comments for product_barcodes table
COMMENT
ON COLUMN product_barcodes.id IS 'Unique barcode identifier (UUID)';
COMMENT
ON COLUMN product_barcodes.product_id IS 'Foreign key to products table';
COMMENT
ON COLUMN product_barcodes.barcode IS 'Secondary barcode value (unique across all products)';
COMMENT
ON COLUMN product_barcodes.barcode_type IS 'Barcode type: EAN_13, CODE_128, UPC_A, ITF_14, CODE_39';

