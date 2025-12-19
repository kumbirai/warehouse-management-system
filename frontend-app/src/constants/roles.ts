/**
 * Role definitions aligned with Roles_and_Permissions_Definition.md
 *
 * All roles are defined as realm roles in Keycloak and included in JWT tokens.
 * Roles are organized into three categories:
 * 1. System-Level Roles - Cross-tenant administrative access
 * 2. Tenant-Level Roles - Administrative and management access within a tenant
 * 3. Operational Roles - Day-to-day warehouse operations
 */

/**
 * System-Level Roles
 */
export const SYSTEM_ADMIN = 'SYSTEM_ADMIN';

/**
 * Tenant-Level Roles
 */
export const TENANT_ADMIN = 'TENANT_ADMIN';
export const WAREHOUSE_MANAGER = 'WAREHOUSE_MANAGER';
export const STOCK_MANAGER = 'STOCK_MANAGER';
export const LOCATION_MANAGER = 'LOCATION_MANAGER';
export const RECONCILIATION_MANAGER = 'RECONCILIATION_MANAGER';
export const RETURNS_MANAGER = 'RETURNS_MANAGER';

/**
 * Operational Roles
 */
export const OPERATOR = 'OPERATOR';
export const PICKER = 'PICKER';
export const STOCK_CLERK = 'STOCK_CLERK';
export const RECONCILIATION_CLERK = 'RECONCILIATION_CLERK';
export const RETURNS_CLERK = 'RETURNS_CLERK';
export const VIEWER = 'VIEWER';
export const USER = 'USER';

/**
 * Service Role (for service-to-service communication, typically not used in frontend)
 */
export const SERVICE = 'SERVICE';

/**
 * All available roles for user assignment
 * Excludes SERVICE role as it's for service-to-service communication
 */
export const ALL_ROLES = [
  // System-Level
  SYSTEM_ADMIN,
  // Tenant-Level
  TENANT_ADMIN,
  WAREHOUSE_MANAGER,
  STOCK_MANAGER,
  LOCATION_MANAGER,
  RECONCILIATION_MANAGER,
  RETURNS_MANAGER,
  // Operational
  OPERATOR,
  PICKER,
  STOCK_CLERK,
  RECONCILIATION_CLERK,
  RETURNS_CLERK,
  VIEWER,
  USER,
] as const;

/**
 * System-level roles (cross-tenant access)
 */
export const SYSTEM_LEVEL_ROLES = [SYSTEM_ADMIN] as const;

/**
 * Tenant-level roles (single tenant scope)
 */
export const TENANT_LEVEL_ROLES = [
  TENANT_ADMIN,
  WAREHOUSE_MANAGER,
  STOCK_MANAGER,
  LOCATION_MANAGER,
  RECONCILIATION_MANAGER,
  RETURNS_MANAGER,
] as const;

/**
 * Operational roles (day-to-day warehouse operations)
 */
export const OPERATIONAL_ROLES = [
  OPERATOR,
  PICKER,
  STOCK_CLERK,
  RECONCILIATION_CLERK,
  RETURNS_CLERK,
  VIEWER,
  USER,
] as const;

/**
 * Manager roles (warehouse and specialized managers)
 */
export const MANAGER_ROLES = [
  WAREHOUSE_MANAGER,
  STOCK_MANAGER,
  LOCATION_MANAGER,
  RECONCILIATION_MANAGER,
  RETURNS_MANAGER,
] as const;

/**
 * Clerk roles (specialized operational roles)
 */
export const CLERK_ROLES = [STOCK_CLERK, RECONCILIATION_CLERK, RETURNS_CLERK] as const;

/**
 * Admin roles (system and tenant administrators)
 */
export const ADMIN_ROLES = [SYSTEM_ADMIN, TENANT_ADMIN] as const;

/**
 * Roles that can manage stock consignments
 * Based on permissions: stock:consignment:*
 */
export const STOCK_CONSIGNMENT_ROLES = [
  SYSTEM_ADMIN,
  TENANT_ADMIN,
  WAREHOUSE_MANAGER,
  STOCK_MANAGER,
  OPERATOR,
  STOCK_CLERK,
] as const;

/**
 * Roles that can execute operations (picking, stock movements, etc.)
 */
export const EXECUTE_OPERATION_ROLES = [
  SYSTEM_ADMIN,
  TENANT_ADMIN,
  WAREHOUSE_MANAGER,
  OPERATOR,
  PICKER,
  STOCK_CLERK,
  RECONCILIATION_CLERK,
  RETURNS_CLERK,
] as const;

/**
 * Type for role values
 */
export type Role = (typeof ALL_ROLES)[number];

/**
 * Type guard to check if a string is a valid role
 */
export function isRole(value: string): value is Role {
  return ALL_ROLES.includes(value as Role);
}
