-- Flyway Migration: V4__Add_picking_list_reference.sql
-- Description: Adds picking_list_reference column to picking_lists table for human-readable IDs
-- Date: 2025-01
--
-- This migration adds a human-readable reference field to picking lists.
-- Format: PICK-{YYYYMMDD}-{sequence} (e.g., PICK-20250107-001)
-- The reference is unique per tenant and provides a user-friendly identifier.

-- Add picking_list_reference column to picking_lists table
ALTER TABLE picking_lists
    ADD COLUMN IF NOT EXISTS picking_list_reference VARCHAR (50);

-- Create unique index for tenant_id and picking_list_reference combination
-- This ensures uniqueness per tenant
CREATE UNIQUE INDEX IF NOT EXISTS uk_picking_lists_tenant_reference ON picking_lists(tenant_id, picking_list_reference);

-- Create index for efficient lookups by reference
CREATE INDEX IF NOT EXISTS idx_picking_lists_reference ON picking_lists(picking_list_reference);

-- Add column comment
COMMENT
ON COLUMN picking_lists.picking_list_reference IS 'Human-readable picking list reference (format: PICK-{YYYYMMDD}-{sequence}). Unique per tenant.';

-- Create counter table for reference sequence generation
CREATE TABLE IF NOT EXISTS picking_list_reference_counters
(
    key
    VARCHAR
(
    255
) PRIMARY KEY,
    sequence INTEGER NOT NULL DEFAULT 0,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
                               );

COMMENT
ON TABLE picking_list_reference_counters IS 'Counter table for generating unique picking list references per tenant per day. Key format: {tenantId}_{YYYYMMDD}';
COMMENT
ON COLUMN picking_list_reference_counters.key IS 'Counter key: {tenantId}_{YYYYMMDD}';
COMMENT
ON COLUMN picking_list_reference_counters.sequence IS 'Current sequence number for the key';
COMMENT
ON COLUMN picking_list_reference_counters.last_updated IS 'Timestamp when counter was last updated';
