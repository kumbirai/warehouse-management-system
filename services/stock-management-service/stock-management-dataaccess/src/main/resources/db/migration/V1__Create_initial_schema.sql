-- Migration: Create initial schema
-- Version: 1
-- Description: Creates stock_consignments and consignment_line_items tables

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
    created_at TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
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
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_consignment_line_items_consignment FOREIGN KEY
(
    consignment_id
) REFERENCES stock_consignments
(
    id
) ON DELETE CASCADE
    );

