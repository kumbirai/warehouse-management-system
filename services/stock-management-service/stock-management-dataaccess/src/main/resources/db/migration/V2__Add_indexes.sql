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

-- Indexes for stock_allocations table
CREATE INDEX IF NOT EXISTS idx_stock_allocations_tenant ON stock_allocations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_stock_allocations_product ON stock_allocations(product_id);
CREATE INDEX IF NOT EXISTS idx_stock_allocations_location ON stock_allocations(location_id) WHERE location_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_stock_allocations_stock_item ON stock_allocations(stock_item_id);
CREATE INDEX IF NOT EXISTS idx_stock_allocations_status ON stock_allocations(status);
CREATE INDEX IF NOT EXISTS idx_stock_allocations_reference ON stock_allocations(tenant_id, reference_id) WHERE reference_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_stock_allocations_allocated_at ON stock_allocations(allocated_at DESC);
CREATE INDEX IF NOT EXISTS idx_stock_allocations_tenant_product ON stock_allocations(tenant_id, product_id);
CREATE INDEX IF NOT EXISTS idx_stock_allocations_tenant_product_location ON stock_allocations(tenant_id, product_id, location_id) WHERE location_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_stock_allocations_stock_item_status ON stock_allocations(stock_item_id, status);

-- Indexes for stock_adjustments table
CREATE INDEX IF NOT EXISTS idx_stock_adjustments_tenant ON stock_adjustments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_stock_adjustments_product ON stock_adjustments(product_id);
CREATE INDEX IF NOT EXISTS idx_stock_adjustments_location ON stock_adjustments(location_id) WHERE location_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_stock_adjustments_stock_item ON stock_adjustments(stock_item_id) WHERE stock_item_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_stock_adjustments_adjusted_at ON stock_adjustments(adjusted_at DESC);
CREATE INDEX IF NOT EXISTS idx_stock_adjustments_tenant_product ON stock_adjustments(tenant_id, product_id);
CREATE INDEX IF NOT EXISTS idx_stock_adjustments_tenant_product_location ON stock_adjustments(tenant_id, product_id, location_id) WHERE location_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_stock_adjustments_tenant_stock_item ON stock_adjustments(tenant_id, stock_item_id) WHERE stock_item_id IS NOT NULL;

-- Indexes for stock_level_thresholds table
CREATE INDEX IF NOT EXISTS idx_stock_level_thresholds_tenant ON stock_level_thresholds(tenant_id);
CREATE INDEX IF NOT EXISTS idx_stock_level_thresholds_product ON stock_level_thresholds(product_id);
CREATE INDEX IF NOT EXISTS idx_stock_level_thresholds_location ON stock_level_thresholds(location_id) WHERE location_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_stock_level_thresholds_tenant_product ON stock_level_thresholds(tenant_id, product_id);
CREATE INDEX IF NOT EXISTS idx_stock_level_thresholds_tenant_product_location ON stock_level_thresholds(tenant_id, product_id, location_id) WHERE location_id IS NOT NULL;

