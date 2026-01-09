-- Flyway Migration: V2__Add_indexes.sql
-- Description: Creates indexes for optimized queries on picking service tables
-- Date: 2025-01

-- Indexes for picking_lists table
CREATE INDEX IF NOT EXISTS idx_picking_lists_tenant_id ON picking_lists(tenant_id);
CREATE INDEX IF NOT EXISTS idx_picking_lists_status ON picking_lists(status);
CREATE INDEX IF NOT EXISTS idx_picking_lists_received_at ON picking_lists(received_at DESC);

-- Indexes for loads table
CREATE INDEX IF NOT EXISTS idx_loads_tenant_id ON loads(tenant_id);
CREATE INDEX IF NOT EXISTS idx_loads_picking_list_id ON loads(picking_list_id);
CREATE INDEX IF NOT EXISTS idx_loads_load_number ON loads(load_number);
CREATE INDEX IF NOT EXISTS idx_loads_status ON loads(status);
CREATE INDEX IF NOT EXISTS idx_loads_created_at ON loads(created_at DESC);

-- Unique constraint for loads: tenant_id and load_number combination
CREATE UNIQUE INDEX IF NOT EXISTS uk_loads_tenant_load_number ON loads(tenant_id, load_number);

-- Indexes for orders table
CREATE INDEX IF NOT EXISTS idx_orders_load_id ON orders(load_id);
CREATE INDEX IF NOT EXISTS idx_orders_order_number ON orders(order_number);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_priority ON orders(priority);
CREATE INDEX IF NOT EXISTS idx_orders_customer_code ON orders(customer_code);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at DESC);

-- Unique constraint for orders: load_id and order_number combination
CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_load_order_number ON orders(load_id, order_number);

-- Indexes for order_line_items table
CREATE INDEX IF NOT EXISTS idx_order_line_items_order_id ON order_line_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_line_items_product_code ON order_line_items(product_code);

-- Indexes for picking_tasks table
CREATE INDEX IF NOT EXISTS idx_picking_tasks_load_id ON picking_tasks(load_id);
CREATE INDEX IF NOT EXISTS idx_picking_tasks_order_id ON picking_tasks(order_id);
CREATE INDEX IF NOT EXISTS idx_picking_tasks_location_id ON picking_tasks(location_id);
CREATE INDEX IF NOT EXISTS idx_picking_tasks_product_code ON picking_tasks(product_code);
CREATE INDEX IF NOT EXISTS idx_picking_tasks_status ON picking_tasks(status);
CREATE INDEX IF NOT EXISTS idx_picking_tasks_sequence ON picking_tasks(sequence);
CREATE INDEX IF NOT EXISTS idx_picking_tasks_load_sequence ON picking_tasks(load_id, sequence);
