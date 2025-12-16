-- Flyway Migration: V2__Add_indexes.sql
-- Description: Creates indexes for performance optimization on products and product_barcodes tables
-- Date: 2025-01
-- 
-- Note: These indexes are created in the public schema for Hibernate validation.
-- At runtime, equivalent indexes are created in tenant-specific schemas.

-- Indexes for products table
CREATE INDEX IF NOT EXISTS idx_products_tenant_id ON products (tenant_id);
CREATE INDEX IF NOT EXISTS idx_products_product_code ON products (product_code);
CREATE INDEX IF NOT EXISTS idx_products_primary_barcode ON products (primary_barcode);
CREATE INDEX IF NOT EXISTS idx_products_category ON products (tenant_id, category);

-- Indexes for product_barcodes table
CREATE INDEX IF NOT EXISTS idx_product_barcodes_product_id ON product_barcodes (product_id);
CREATE INDEX IF NOT EXISTS idx_product_barcodes_barcode ON product_barcodes (barcode);

