-- Flyway Migration: V3__Insert_initial_data.sql
-- Description: Inserts initial reference data for the location management service
-- Date: 2025-01
-- 
-- Note: This migration is for the validation schema (public schema).
-- At runtime, initial data is inserted into tenant-specific schemas when
-- TenantSchemaCreatedEventListener processes TenantSchemaCreatedEvent.
--
-- Currently, no initial data is required for the location management service.
-- Locations are created dynamically based on business requirements.
-- This file is kept for consistency with the migration structure and future extensibility.

-- No initial data required at this time
-- Future reference data can be inserted here if needed

