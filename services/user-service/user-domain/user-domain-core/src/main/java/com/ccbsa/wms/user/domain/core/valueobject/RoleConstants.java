package com.ccbsa.wms.user.domain.core.valueobject;

import java.util.Set;

/**
 * Role Constants
 * <p>
 * Defines all valid roles in the system, aligned with Roles_and_Permissions_Definition.md.
 * All roles are defined as realm roles in Keycloak and included in JWT tokens.
 * <p>
 * Roles are organized into three categories:
 * 1. System-Level Roles - Cross-tenant administrative access
 * 2. Tenant-Level Roles - Administrative and management access within a tenant
 * 3. Operational Roles - Day-to-day warehouse operations
 */
public final class RoleConstants {

    /**
     * System-Level Roles
     */
    public static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";
    /**
     * Tenant-Level Roles
     */
    public static final String TENANT_ADMIN = "TENANT_ADMIN";
    public static final String WAREHOUSE_MANAGER = "WAREHOUSE_MANAGER";
    public static final String STOCK_MANAGER = "STOCK_MANAGER";
    public static final String LOCATION_MANAGER = "LOCATION_MANAGER";
    public static final String RECONCILIATION_MANAGER = "RECONCILIATION_MANAGER";
    public static final String RETURNS_MANAGER = "RETURNS_MANAGER";
    /**
     * Operational Roles
     */
    public static final String OPERATOR = "OPERATOR";
    public static final String PICKER = "PICKER";
    public static final String STOCK_CLERK = "STOCK_CLERK";
    public static final String RECONCILIATION_CLERK = "RECONCILIATION_CLERK";
    public static final String RETURNS_CLERK = "RETURNS_CLERK";
    public static final String VIEWER = "VIEWER";
    public static final String USER = "USER";
    /**
     * Service Role (for service-to-service communication)
     */
    public static final String SERVICE = "SERVICE";
    /**
     * All valid roles for user assignment.
     * Excludes SERVICE role as it's for service-to-service communication.
     */
    public static final Set<String> VALID_ROLES = Set.of(
            SYSTEM_ADMIN,
            TENANT_ADMIN,
            WAREHOUSE_MANAGER,
            STOCK_MANAGER,
            LOCATION_MANAGER,
            RECONCILIATION_MANAGER,
            RETURNS_MANAGER,
            OPERATOR,
            PICKER,
            STOCK_CLERK,
            RECONCILIATION_CLERK,
            RETURNS_CLERK,
            VIEWER,
            USER
    );
    /**
     * System-level roles (cross-tenant access)
     */
    public static final Set<String> SYSTEM_LEVEL_ROLES = Set.of(SYSTEM_ADMIN);
    /**
     * Tenant-level roles (single tenant scope)
     */
    public static final Set<String> TENANT_LEVEL_ROLES = Set.of(
            TENANT_ADMIN,
            WAREHOUSE_MANAGER,
            STOCK_MANAGER,
            LOCATION_MANAGER,
            RECONCILIATION_MANAGER,
            RETURNS_MANAGER
    );
    /**
     * Operational roles (day-to-day warehouse operations)
     */
    public static final Set<String> OPERATIONAL_ROLES = Set.of(
            OPERATOR,
            PICKER,
            STOCK_CLERK,
            RECONCILIATION_CLERK,
            RETURNS_CLERK,
            VIEWER,
            USER
    );
    /**
     * Manager roles (warehouse and specialized managers)
     */
    public static final Set<String> MANAGER_ROLES = Set.of(
            WAREHOUSE_MANAGER,
            STOCK_MANAGER,
            LOCATION_MANAGER,
            RECONCILIATION_MANAGER,
            RETURNS_MANAGER
    );
    /**
     * Clerk roles (specialized operational roles)
     */
    public static final Set<String> CLERK_ROLES = Set.of(
            STOCK_CLERK,
            RECONCILIATION_CLERK,
            RETURNS_CLERK
    );
    /**
     * Admin roles (system and tenant administrators)
     */
    public static final Set<String> ADMIN_ROLES = Set.of(SYSTEM_ADMIN, TENANT_ADMIN);
    /**
     * Base role that all users have by default.
     */
    public static final String BASE_ROLE = USER;

    private RoleConstants() {
        // Utility class - prevent instantiation
    }

    /**
     * Checks if a role name is valid.
     *
     * @param roleName Role name to validate
     * @return true if role is valid
     */
    public static boolean isValidRole(String roleName) {
        return roleName != null && VALID_ROLES.contains(roleName);
    }

    /**
     * Checks if a role is a system-level role.
     *
     * @param roleName Role name to check
     * @return true if role is system-level
     */
    public static boolean isSystemLevelRole(String roleName) {
        return SYSTEM_LEVEL_ROLES.contains(roleName);
    }

    /**
     * Checks if a role is a tenant-level role.
     *
     * @param roleName Role name to check
     * @return true if role is tenant-level
     */
    public static boolean isTenantLevelRole(String roleName) {
        return TENANT_LEVEL_ROLES.contains(roleName);
    }

    /**
     * Checks if a role is an operational role.
     *
     * @param roleName Role name to check
     * @return true if role is operational
     */
    public static boolean isOperationalRole(String roleName) {
        return OPERATIONAL_ROLES.contains(roleName);
    }

    /**
     * Checks if a role is a manager role.
     *
     * @param roleName Role name to check
     * @return true if role is a manager role
     */
    public static boolean isManagerRole(String roleName) {
        return MANAGER_ROLES.contains(roleName);
    }

    /**
     * Checks if a role is a clerk role.
     *
     * @param roleName Role name to check
     * @return true if role is a clerk role
     */
    public static boolean isClerkRole(String roleName) {
        return CLERK_ROLES.contains(roleName);
    }

    /**
     * Checks if a role is an admin role.
     *
     * @param roleName Role name to check
     * @return true if role is an admin role
     */
    public static boolean isAdminRole(String roleName) {
        return ADMIN_ROLES.contains(roleName);
    }
}

