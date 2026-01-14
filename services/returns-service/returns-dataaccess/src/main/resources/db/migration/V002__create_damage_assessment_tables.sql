-- Flyway Migration: V002__create_damage_assessment_tables.sql
-- Description: Creates the damage_assessments and damaged_product_items tables for the returns service
-- Date: 2025-01
-- 
-- Note: Returns service is tenant-aware. Uses schema-per-tenant strategy via TenantAwarePhysicalNamingStrategy.
-- Each tenant has its own isolated PostgreSQL schema for multi-tenant isolation.
--
-- IMPORTANT: This migration creates validation tables in the public schema for Hibernate
-- schema validation at startup. This table is NOT used at runtime - all runtime operations
-- use tenant-specific schemas created programmatically when TenantSchemaCreatedEvent is received.

-- Create damage_assessments table
CREATE TABLE IF NOT EXISTS damage_assessments
(
    assessment_id
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
    damage_type VARCHAR
(
    50
) NOT NULL,
    damage_severity VARCHAR
(
    50
) NOT NULL,
    damage_source VARCHAR
(
    50
) NOT NULL,
    assessment_status VARCHAR
(
    50
) NOT NULL,
    insurance_claim_number VARCHAR
(
    100
),
    insurance_company VARCHAR
(
    200
),
    insurance_claim_status VARCHAR
(
    50
),
    insurance_claim_amount DECIMAL
(
    19,
    2
),
    damage_notes VARCHAR
(
    2000
),
    recorded_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_damage_assessments_tenant_order UNIQUE
(
    tenant_id,
    order_number,
    assessment_id
)
    );

-- Create damaged_product_items table
CREATE TABLE IF NOT EXISTS damaged_product_items
(
    item_id
    UUID
    PRIMARY
    KEY,
    assessment_id
    UUID
    NOT
    NULL,
    product_id
    UUID
    NOT
    NULL,
    damaged_quantity
    INTEGER
    NOT
    NULL,
    damage_type
    VARCHAR
(
    50
) NOT NULL,
    damage_severity VARCHAR
(
    50
) NOT NULL,
    damage_source VARCHAR
(
    50
) NOT NULL,
    photo_url VARCHAR
(
    500
),
    notes VARCHAR
(
    1000
),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_damaged_product_items_assessment FOREIGN KEY
(
    assessment_id
) REFERENCES damage_assessments
(
    assessment_id
) ON DELETE CASCADE
    );

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_damage_assessments_tenant_status ON damage_assessments (tenant_id, assessment_status);
CREATE INDEX IF NOT EXISTS idx_damage_assessments_tenant_order ON damage_assessments (tenant_id, order_number);
CREATE INDEX IF NOT EXISTS idx_damaged_product_items_assessment ON damaged_product_items (assessment_id);
CREATE INDEX IF NOT EXISTS idx_damaged_product_items_product ON damaged_product_items (product_id);
