-- Migration: Add indexes
-- Version: 2
-- Description: Creates indexes for optimized queries on stock consignment tables

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

