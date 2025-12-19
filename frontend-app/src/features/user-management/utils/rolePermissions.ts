import {
    LOCATION_MANAGER,
    OPERATOR,
    PICKER,
    RECONCILIATION_CLERK,
    RECONCILIATION_MANAGER,
    RETURNS_CLERK,
    RETURNS_MANAGER,
    STOCK_CLERK,
    STOCK_MANAGER,
    SYSTEM_ADMIN,
    TENANT_ADMIN,
    USER,
    VIEWER,
    WAREHOUSE_MANAGER,
} from '../../../constants/roles';

/**
 * Roles that WAREHOUSE_MANAGER can assign
 */
const WAREHOUSE_MANAGER_ASSIGNABLE_ROLES = [
  OPERATOR,
  PICKER,
  STOCK_CLERK,
  RECONCILIATION_CLERK,
  RETURNS_CLERK,
  VIEWER,
  USER,
] as const;

/**
 * Mapping of manager roles to their assignable clerk roles
 */
const MANAGER_TO_CLERK_ROLES: Record<string, readonly string[]> = {
  [STOCK_MANAGER]: [STOCK_CLERK, VIEWER, USER],
  [LOCATION_MANAGER]: [VIEWER, USER],
  [RECONCILIATION_MANAGER]: [RECONCILIATION_CLERK, VIEWER, USER],
  [RETURNS_MANAGER]: [RETURNS_CLERK, VIEWER, USER],
};

/**
 * Checks if the current user can assign a specific role to a target user.
 *
 * Rules from Roles_and_Permissions_Definition.md:
 * 1. SYSTEM_ADMIN can assign any role to any user
 * 2. TENANT_ADMIN can assign any role to users within own tenant only
 * 3. WAREHOUSE_MANAGER can assign operational roles to users within own tenant
 * 4. Specialized managers can assign corresponding clerk roles within own tenant
 * 5. SYSTEM_ADMIN can only be assigned by existing SYSTEM_ADMIN
 *
 * @param currentUserRoles Current user's roles
 * @param roleToAssign Role to assign
 * @param targetUserTenantId Target user's tenant ID (optional, for tenant validation)
 * @param currentUserTenantId Current user's tenant ID (optional, for tenant validation)
 * @returns true if the role can be assigned
 */
export function canAssignRole(
  currentUserRoles: string[],
  roleToAssign: string,
  targetUserTenantId?: string,
  currentUserTenantId?: string
): boolean {
  // Rule 1: SYSTEM_ADMIN can assign any role to any user
  if (currentUserRoles.includes(SYSTEM_ADMIN)) {
    // Special check: SYSTEM_ADMIN can only be assigned by existing SYSTEM_ADMIN
    if (roleToAssign === SYSTEM_ADMIN && !currentUserRoles.includes(SYSTEM_ADMIN)) {
      return false;
    }
    return true;
  }

  // Rule 2: TENANT_ADMIN can assign any role to users within own tenant only
  if (currentUserRoles.includes(TENANT_ADMIN)) {
    // Validate tenant match if tenant IDs are provided
    if (targetUserTenantId && currentUserTenantId && targetUserTenantId !== currentUserTenantId) {
      return false;
    }
    // TENANT_ADMIN cannot assign SYSTEM_ADMIN
    if (roleToAssign === SYSTEM_ADMIN) {
      return false;
    }
    return true;
  }

  // Rule 3: WAREHOUSE_MANAGER can assign operational roles to users within own tenant
  if (currentUserRoles.includes(WAREHOUSE_MANAGER)) {
    if (targetUserTenantId && currentUserTenantId && targetUserTenantId !== currentUserTenantId) {
      return false;
    }
    return (WAREHOUSE_MANAGER_ASSIGNABLE_ROLES as readonly string[]).includes(roleToAssign);
  }

  // Rule 4: Specialized managers can assign corresponding clerk roles
  for (const [managerRole, assignableRoles] of Object.entries(MANAGER_TO_CLERK_ROLES)) {
    if (currentUserRoles.includes(managerRole)) {
      if (targetUserTenantId && currentUserTenantId && targetUserTenantId !== currentUserTenantId) {
        return false;
      }
      return assignableRoles.includes(roleToAssign);
    }
  }

  // Rule 5: Other roles cannot manage roles
  return false;
}

/**
 * Checks if the current user can remove a specific role from a target user.
 * Uses the same rules as assignment, with additional constraint:
 * - Cannot remove USER role (base role)
 *
 * @param currentUserRoles Current user's roles
 * @param roleToRemove Role to remove
 * @param targetUserTenantId Target user's tenant ID (optional)
 * @param currentUserTenantId Current user's tenant ID (optional)
 * @returns true if the role can be removed
 */
export function canRemoveRole(
  currentUserRoles: string[],
  roleToRemove: string,
  targetUserTenantId?: string,
  currentUserTenantId?: string
): boolean {
  // Cannot remove base role
  if (roleToRemove === USER) {
    return false;
  }

  // Use same validation as assignment
  return canAssignRole(currentUserRoles, roleToRemove, targetUserTenantId, currentUserTenantId);
}

/**
 * Gets the reason why a role cannot be assigned (for tooltips/error messages)
 */
export function getRoleAssignmentReason(
  currentUserRoles: string[],
  roleToAssign: string,
  targetUserTenantId?: string,
  currentUserTenantId?: string
): string | null {
  if (canAssignRole(currentUserRoles, roleToAssign, targetUserTenantId, currentUserTenantId)) {
    return null;
  }

  if (currentUserRoles.includes(SYSTEM_ADMIN)) {
    if (roleToAssign === SYSTEM_ADMIN && !currentUserRoles.includes(SYSTEM_ADMIN)) {
      return 'SYSTEM_ADMIN role can only be assigned by existing SYSTEM_ADMIN';
    }
    return null; // Should not happen
  }

  if (currentUserRoles.includes(TENANT_ADMIN)) {
    if (targetUserTenantId && currentUserTenantId && targetUserTenantId !== currentUserTenantId) {
      return 'TENANT_ADMIN can only assign roles to users in own tenant';
    }
    if (roleToAssign === SYSTEM_ADMIN) {
      return 'TENANT_ADMIN cannot assign SYSTEM_ADMIN role';
    }
    return 'Insufficient privileges to assign this role';
  }

  if (currentUserRoles.includes(WAREHOUSE_MANAGER)) {
    if (targetUserTenantId && currentUserTenantId && targetUserTenantId !== currentUserTenantId) {
      return 'WAREHOUSE_MANAGER can only assign roles to users in own tenant';
    }
    return 'WAREHOUSE_MANAGER can only assign operational roles';
  }

  for (const [managerRole] of Object.entries(MANAGER_TO_CLERK_ROLES)) {
    if (currentUserRoles.includes(managerRole)) {
      if (targetUserTenantId && currentUserTenantId && targetUserTenantId !== currentUserTenantId) {
        return `${managerRole} can only assign roles to users in own tenant`;
      }
      return `${managerRole} can only assign specific clerk roles`;
    }
  }

  return 'You do not have permission to assign roles';
}
