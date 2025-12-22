-- Flyway Migration: V2__Add_indexes.sql
-- Description: Creates indexes for optimized queries on stock management tables
-- Date: 2025-01

-- Indexes for stock_consignments table
CREATE INDEX IF NOT EXISTS idx_stock_consignments_tenant_id ON stock_consignments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_stock_consignments_reference ON stock_consignments(consignment_reference);
CREATE INDEX IF NOT EXISTS idx_stock_consignments_status ON stock_consignments(status);
CREATE INDEX IF NOT EXISTS idx_stock_consignments_warehouse_id ON stock_consignments(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_stock_consignments_received_at ON stock_consignments(received_at);

-- Indexes for consignment_line_items table
CREATE INDEX IF NOT EXISTS idx_consignment_line_items_consignment_id ON consignment_line_items(consignment_id);
CREATE INDEX IF NOT EXISTS idx_consignment_line_items_product_code ON consignment_line_items(product_code);
CREATE INDEX IF NOT EXISTS idx_consignment_line_items_expiration_date ON consignment_line_items(expiration_date);

-- Indexes for stock_items table
CREATE INDEX IF NOT EXISTS idx_stock_items_tenant_id ON stock_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_stock_items_product_id ON stock_items(product_id);
CREATE INDEX IF NOT EXISTS idx_stock_items_location_id ON stock_items(location_id);
CREATE INDEX IF NOT EXISTS idx_stock_items_consignment_id ON stock_items(consignment_id);
CREATE INDEX IF NOT EXISTS idx_stock_items_classification ON stock_items(tenant_id, classification);
CREATE INDEX IF NOT EXISTS idx_stock_items_expiration_date ON stock_items(expiration_date) WHERE expiration_date IS NOT NULL;

