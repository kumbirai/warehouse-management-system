package com.ccbsa.wms.user.application.service.validation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.user.application.service.exception.InsufficientPrivilegesException;
import com.ccbsa.wms.user.application.service.exception.TenantMismatchException;
import com.ccbsa.wms.user.domain.core.entity.User;
import com.ccbsa.wms.user.domain.core.valueobject.RoleConstants;

/**
 * Validator for role assignment operations.
 * <p>
 * Enforces role assignment rules from Roles_and_Permissions_Definition.md:
 * <ul>
 *   <li>SYSTEM_ADMIN can assign any role to any user</li>
 *   <li>TENANT_ADMIN can assign any role to users within own tenant only</li>
 *   <li>WAREHOUSE_MANAGER can assign operational roles to users within own tenant</li>
 *   <li>Specialized managers can assign corresponding clerk roles within own tenant</li>
 *   <li>SYSTEM_ADMIN can only be assigned by existing SYSTEM_ADMIN</li>
 *   <li>Users cannot assign roles with higher privileges than their own</li>
 *   <li>Tenant-scoped roles can only be assigned to users within the same tenant</li>
 * </ul>
 */
@Component
public class RoleAssignmentValidator {
    private static final Logger logger = LoggerFactory.getLogger(RoleAssignmentValidator.class);

    // Roles that WAREHOUSE_MANAGER can assign
    private static final Set<String> WAREHOUSE_MANAGER_ASSIGNABLE_ROLES =
            Set.of(RoleConstants.OPERATOR, RoleConstants.PICKER, RoleConstants.STOCK_CLERK, RoleConstants.RECONCILIATION_CLERK, RoleConstants.RETURNS_CLERK, RoleConstants.VIEWER,
                    RoleConstants.USER);

    // Mapping of manager roles to their assignable clerk roles
    private static final Map<String, Set<String>> MANAGER_TO_CLERK_ROLES =
            Map.of(RoleConstants.STOCK_MANAGER, Set.of(RoleConstants.STOCK_CLERK, RoleConstants.VIEWER, RoleConstants.USER), RoleConstants.LOCATION_MANAGER,
                    Set.of(RoleConstants.VIEWER, RoleConstants.USER), RoleConstants.RECONCILIATION_MANAGER,
                    Set.of(RoleConstants.RECONCILIATION_CLERK, RoleConstants.VIEWER, RoleConstants.USER), RoleConstants.RETURNS_MANAGER,
                    Set.of(RoleConstants.RETURNS_CLERK, RoleConstants.VIEWER, RoleConstants.USER));

    /**
     * Validates that the current user can remove the specified role from the target user.
     * <p>
     * Uses the same rules as assignment, with additional constraints:
     * <ul>
     *   <li>Cannot remove USER role (base role)</li>
     *   <li>SYSTEM_ADMIN removal requires existing SYSTEM_ADMIN</li>
     * </ul>
     *
     * @param currentUserId       Current user ID (remover)
     * @param currentUserRoles    Current user's roles
     * @param currentUserTenantId Current user's tenant ID
     * @param targetUser          Target user
     * @param roleToRemove        Role to remove
     * @throws InsufficientPrivilegesException if user cannot remove the role
     * @throws TenantMismatchException         if tenant mismatch
     */
    public void validateRoleRemoval(UserId currentUserId, List<String> currentUserRoles, TenantId currentUserTenantId, User targetUser, String roleToRemove) {

        // Cannot remove base role
        if (RoleConstants.BASE_ROLE.equals(roleToRemove)) {
            throw new InsufficientPrivilegesException(String.format("Cannot remove base role: %s", roleToRemove));
        }

        // Use same validation as assignment
        validateRoleAssignment(currentUserId, currentUserRoles, currentUserTenantId, targetUser, roleToRemove);
    }

    /**
     * Validates that the current user can assign the specified role to the target user.
     *
     * @param currentUserId       Current user ID (assigner)
     * @param currentUserRoles    Current user's roles
     * @param currentUserTenantId Current user's tenant ID
     * @param targetUser          Target user (assignee)
     * @param roleToAssign        Role to assign
     * @throws InsufficientPrivilegesException if user cannot assign the role
     * @throws TenantMismatchException         if tenant mismatch
     */
    public void validateRoleAssignment(UserId currentUserId, List<String> currentUserRoles, TenantId currentUserTenantId, User targetUser, String roleToAssign) {

        logger.debug("Validating role assignment: currentUserId={}, currentUserRoles={}, targetUserId={}, targetTenantId={}, role={}", currentUserId.getValue(), currentUserRoles,
                targetUser.getId().getValue(), targetUser.getTenantId().getValue(), roleToAssign);

        // Rule 1: SYSTEM_ADMIN can assign any role to any user
        if (currentUserRoles.contains(RoleConstants.SYSTEM_ADMIN)) {
            // Special check: SYSTEM_ADMIN can only be assigned by existing SYSTEM_ADMIN
            if (RoleConstants.SYSTEM_ADMIN.equals(roleToAssign) && !currentUserRoles.contains(RoleConstants.SYSTEM_ADMIN)) {
                throw new InsufficientPrivilegesException("SYSTEM_ADMIN role can only be assigned by existing SYSTEM_ADMIN");
            }
            logger.debug("SYSTEM_ADMIN can assign any role");
            return; // SYSTEM_ADMIN can assign any role
        }

        // Rule 2: TENANT_ADMIN can assign any role to users within own tenant only
        if (currentUserRoles.contains(RoleConstants.TENANT_ADMIN)) {
            // Validate tenant match
            if (!targetUser.getTenantId().equals(currentUserTenantId)) {
                throw new TenantMismatchException(String.format("TENANT_ADMIN can only assign roles to users in own tenant. " + "Target user tenant: %s, Current user tenant: %s",
                        targetUser.getTenantId().getValue(), currentUserTenantId.getValue()));
            }

            // TENANT_ADMIN cannot assign SYSTEM_ADMIN
            if (RoleConstants.SYSTEM_ADMIN.equals(roleToAssign)) {
                throw new InsufficientPrivilegesException("TENANT_ADMIN cannot assign SYSTEM_ADMIN role");
            }

            logger.debug("TENANT_ADMIN can assign role within own tenant");
            return; // TENANT_ADMIN can assign any other role within own tenant
        }

        // Rule 3: WAREHOUSE_MANAGER can assign operational roles to users within own tenant
        if (currentUserRoles.contains(RoleConstants.WAREHOUSE_MANAGER)) {
            if (!targetUser.getTenantId().equals(currentUserTenantId)) {
                throw new TenantMismatchException("WAREHOUSE_MANAGER can only assign roles to users in own tenant");
            }

            // WAREHOUSE_MANAGER can assign operational roles only
            if (!WAREHOUSE_MANAGER_ASSIGNABLE_ROLES.contains(roleToAssign)) {
                throw new InsufficientPrivilegesException(String.format("WAREHOUSE_MANAGER cannot assign role: %s", roleToAssign));
            }

            logger.debug("WAREHOUSE_MANAGER can assign operational role");
            return;
        }

        // Rule 4: Specialized managers can assign corresponding clerk roles
        for (Map.Entry<String, Set<String>> entry : MANAGER_TO_CLERK_ROLES.entrySet()) {
            String managerRole = entry.getKey();
            Set<String> allowedClerkRoles = entry.getValue();

            if (currentUserRoles.contains(managerRole)) {
                if (!targetUser.getTenantId().equals(currentUserTenantId)) {
                    throw new TenantMismatchException(String.format("%s can only assign roles to users in own tenant", managerRole));
                }

                if (!allowedClerkRoles.contains(roleToAssign)) {
                    throw new InsufficientPrivilegesException(String.format("%s cannot assign role: %s", managerRole, roleToAssign));
                }

                logger.debug("{} can assign clerk role", managerRole);
                return;
            }
        }

        // Rule 5: Other roles cannot manage roles
        throw new InsufficientPrivilegesException(String.format("User with roles %s cannot assign roles", currentUserRoles));
    }
}

