# User Role Management Implementation Plan

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 2.0
**Date:** 2025-12
**Status:** Draft
**Related Documents:**

- **[Roles and Permissions Definition](../../../03-security/Roles_and_Permissions_Definition.md)** - **AUTHORITATIVE REFERENCE** (MUST READ FIRST)
- [User Management Implementation Plan](01-User_Management_Implementation_Plan.md)
- [Security Architecture Document](../../../01-architecture/Security_Architecture_Document.md)
- [Multi-Tenancy Enforcement Guide](../../../03-security/Multi_Tenancy_Enforcement_Guide.md)

---

## Overview

### Purpose

Detailed implementation plan for managing user roles and permissions. Ensures proper role assignment and synchronization with Keycloak. **This plan MUST align with the authoritative [Roles and Permissions Definition](../../../03-security/Roles_and_Permissions_Definition.md) document.**

### Authoritative Reference

**⚠️ CRITICAL:** All role definitions, permissions, assignment rules, and validation logic MUST reference and comply with:
- **[Roles and Permissions Definition](../../../03-security/Roles_and_Permissions_Definition.md)**

This document defines:
- Complete role hierarchy
- All 15 system roles
- Role assignment rules and constraints
- Permission model
- Tenant-scoped role enforcement
- Role-to-service mapping

### Available Roles

**All roles defined in [Roles and Permissions Definition](../../../03-security/Roles_and_Permissions_Definition.md):**

#### System-Level Roles
- **SYSTEM_ADMIN** - Full system access across all tenants

#### Tenant-Level Roles
- **TENANT_ADMIN** - Full administrative access within a tenant
- **WAREHOUSE_MANAGER** - Warehouse operations management
- **STOCK_MANAGER** - Stock management operations
- **LOCATION_MANAGER** - Location management operations
- **RECONCILIATION_MANAGER** - Reconciliation management operations
- **RETURNS_MANAGER** - Returns management operations

#### Operational Roles
- **OPERATOR** - General operational access for warehouse staff
- **PICKER** - Specialized picking operations
- **STOCK_CLERK** - Stock receipt and management operations
- **RECONCILIATION_CLERK** - Stock counting operations
- **RETURNS_CLERK** - Returns processing operations
- **VIEWER** - Read-only access
- **USER** - Basic user access (default role)

#### Service Roles
- **SERVICE** - Service-to-service communication (system role)

**Total: 15 roles** (see [Roles and Permissions Definition](../../../03-security/Roles_and_Permissions_Definition.md) for complete details)

---

## UI/UX Design

### Role Management Interface

The role management UI should display all 15 roles organized by category, with appropriate permissions based on the current user's role.

```
┌─────────────────────────────────────────────────┐
│  User Roles: john.doe                           │
│  Tenant: Local Distribution Partner (ldp-001)    │
├─────────────────────────────────────────────────┤
│  Current Roles:                                 │
│  • USER (Base Role)                             │
│  • PICKER                                       │
│                                                 │
│  Available Roles:                               │
│                                                 │
│  System-Level Roles:                            │
│  ┌─────────────────────────────────────────┐   │
│  │ ☐ SYSTEM_ADMIN                          │   │
│  │   (Full system access across tenants)   │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  Tenant-Level Administrative Roles:             │
│  ┌─────────────────────────────────────────┐   │
│  │ ☐ TENANT_ADMIN                          │   │
│  │ ☐ WAREHOUSE_MANAGER                     │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  Specialized Manager Roles:                     │
│  ┌─────────────────────────────────────────┐   │
│  │ ☐ STOCK_MANAGER                         │   │
│  │ ☐ LOCATION_MANAGER                      │   │
│  │ ☐ RECONCILIATION_MANAGER                 │   │
│  │ ☐ RETURNS_MANAGER                       │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  Operational Roles:                             │
│  ┌─────────────────────────────────────────┐   │
│  │ ☐ OPERATOR                             │   │
│  │ ☑ PICKER                               │   │
│  │ ☐ STOCK_CLERK                          │   │
│  │ ☐ RECONCILIATION_CLERK                 │   │
│  │ ☐ RETURNS_CLERK                        │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  Access Roles:                                  │
│  ┌─────────────────────────────────────────┐   │
│  │ ☐ VIEWER                               │   │
│  │ ☑ USER (Base Role - Cannot Remove)     │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  Service Roles:                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ ☐ SERVICE                               │   │
│  │   (Service-to-service communication)     │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ℹ️  Note: Role availability depends on your   │
│     permissions. Some roles may be disabled.    │
│                                                 │
│  [Cancel]                    [Save Changes]     │
└─────────────────────────────────────────────────┘
```

### Role Selection UI Considerations

1. **Role Grouping** - Group roles by category (System, Tenant Admin, Managers, Operational, Access)
2. **Permission-Based Display** - Show/hide roles based on current user's permissions
3. **Disabled States** - Disable roles that current user cannot assign
4. **Tooltips** - Show role descriptions on hover
5. **Validation Feedback** - Show inline errors for invalid role assignments
6. **Tenant Context** - Display tenant information for tenant-scoped roles

---

## Backend Implementation

### Current Implementation Status

**⚠️ MISSING IMPLEMENTATION:**

1. **Incomplete Role List** - Current implementation only supports 5 roles instead of 15
2. **Missing Role Assignment Rules** - No enforcement of assignment hierarchy and constraints
3. **Missing Permission Validation** - No validation that current user can assign/remove specific roles
4. **Missing Tenant-Scoped Validation** - TENANT_ADMIN can assign roles to users outside own tenant
5. **Missing Role Hierarchy Logic** - No role hierarchy enforcement
6. **Missing Assignment Constraints** - SYSTEM_ADMIN assignment not restricted

### API Endpoints

- `POST /api/v1/users/{id}/roles` - Assign role to user
- `DELETE /api/v1/users/{id}/roles/{roleId}` - Remove role from user
- `GET /api/v1/users/{id}/roles` - Get user roles

### Required Command Handler Implementation

```java
// AssignUserRoleCommandHandler.java
@Component
@Transactional
public class AssignUserRoleCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(AssignUserRoleCommandHandler.class);
    
    // ALL 15 ROLES from Roles_and_Permissions_Definition.md
    private static final Set<String> VALID_ROLES = Set.of(
        "SYSTEM_ADMIN",
        "TENANT_ADMIN",
        "WAREHOUSE_MANAGER",
        "STOCK_MANAGER",
        "LOCATION_MANAGER",
        "RECONCILIATION_MANAGER",
        "RETURNS_MANAGER",
        "OPERATOR",
        "PICKER",
        "STOCK_CLERK",
        "RECONCILIATION_CLERK",
        "RETURNS_CLERK",
        "VIEWER",
        "USER",
        "SERVICE"
    );

    private final UserRepository userRepository;
    private final UserEventPublisher eventPublisher;
    private final AuthenticationServicePort authenticationService;
    private final RoleAssignmentValidator roleAssignmentValidator;
    private final SecurityContextService securityContextService;

    public void handle(AssignUserRoleCommand command) {
        logger.debug("Assigning role to user: userId={}, role={}", 
            command.getUserId().getValue(), command.getRoleName());

        // 1. Validate role exists
        if (!VALID_ROLES.contains(command.getRoleName())) {
            throw new IllegalArgumentException(
                String.format("Invalid role: %s. Valid roles: %s", 
                    command.getRoleName(), VALID_ROLES));
        }

        // 2. Load target user
        User targetUser = userRepository.findById(command.getUserId())
            .orElseThrow(() -> new UserNotFoundException(
                String.format("User not found: %s", command.getUserId().getValue())));

        // 3. Get current user (assigner) from security context
        UserId currentUserId = securityContextService.getCurrentUserId();
        List<String> currentUserRoles = securityContextService.getCurrentUserRoles();
        TenantId currentUserTenantId = TenantContext.getTenantId();

        // 4. Validate assignment permissions
        roleAssignmentValidator.validateRoleAssignment(
            currentUserId,
            currentUserRoles,
            currentUserTenantId,
            targetUser,
            command.getRoleName()
        );

        // 5. Check if user already has this role
        List<String> existingRoles = authenticationService.getUserRoles(
            targetUser.getKeycloakUserId().orElseThrow(
                () -> new IllegalStateException("User has no Keycloak ID")));
        
        if (existingRoles.contains(command.getRoleName())) {
            logger.warn("User already has role: userId={}, role={}", 
                targetUser.getId().getValue(), command.getRoleName());
            return; // Idempotent: already assigned
        }

        // 6. Assign role in Keycloak
        if (targetUser.getKeycloakUserId().isPresent()) {
            try {
                authenticationService.assignRole(
                    targetUser.getKeycloakUserId().get(), 
                    command.getRoleName());
            } catch (Exception e) {
                logger.error("Failed to assign role in Keycloak: {}", e.getMessage(), e);
                throw new RoleAssignmentException(
                    String.format("Failed to assign role: %s", e.getMessage()), e);
            }
        } else {
            throw new IllegalStateException(
                "Cannot assign role: user has no Keycloak ID");
        }

        // 7. Update last modified timestamp
        // Note: Role information is stored in Keycloak, not in domain model
        userRepository.save(targetUser);

        // 8. Publish event
        UserRoleAssignedEvent event = new UserRoleAssignedEvent(
            targetUser.getId(),
            targetUser.getTenantId(),
            command.getRoleName(),
            currentUserId
        );
        eventPublisher.publish(Collections.singletonList(event));

        logger.info("Role assigned successfully: userId={}, role={}, assignedBy={}", 
            targetUser.getId().getValue(), command.getRoleName(), currentUserId.getValue());
    }
}
```

### Role Assignment Validator

```java
// RoleAssignmentValidator.java
@Component
public class RoleAssignmentValidator {
    private static final Logger logger = LoggerFactory.getLogger(RoleAssignmentValidator.class);

    /**
     * Validates that the current user can assign the specified role to the target user.
     * 
     * Rules from Roles_and_Permissions_Definition.md:
     * 1. SYSTEM_ADMIN can assign any role to any user
     * 2. TENANT_ADMIN can assign any role to users within own tenant only
     * 3. WAREHOUSE_MANAGER can assign operational roles to users within own tenant
     * 4. Specialized managers can assign corresponding clerk roles within own tenant
     * 5. SYSTEM_ADMIN can only be assigned by existing SYSTEM_ADMIN
     * 6. Users cannot assign roles with higher privileges than their own
     * 7. Tenant-scoped roles can only be assigned to users within the same tenant
     */
    public void validateRoleAssignment(
            UserId currentUserId,
            List<String> currentUserRoles,
            TenantId currentUserTenantId,
            User targetUser,
            String roleToAssign) {
        
        // Rule 1: SYSTEM_ADMIN can assign any role to any user
        if (currentUserRoles.contains("SYSTEM_ADMIN")) {
            // Special check: SYSTEM_ADMIN can only be assigned by existing SYSTEM_ADMIN
            if ("SYSTEM_ADMIN".equals(roleToAssign) && !currentUserRoles.contains("SYSTEM_ADMIN")) {
                throw new InsufficientPrivilegesException(
                    "SYSTEM_ADMIN role can only be assigned by existing SYSTEM_ADMIN");
            }
            return; // SYSTEM_ADMIN can assign any role
        }

        // Rule 2: TENANT_ADMIN can assign any role to users within own tenant only
        if (currentUserRoles.contains("TENANT_ADMIN")) {
            // Validate tenant match
            if (!targetUser.getTenantId().equals(currentUserTenantId)) {
                throw new TenantMismatchException(
                    String.format("TENANT_ADMIN can only assign roles to users in own tenant. " +
                        "Target user tenant: %s, Current user tenant: %s",
                        targetUser.getTenantId().getValue(), currentUserTenantId.getValue()));
            }
            
            // TENANT_ADMIN cannot assign SYSTEM_ADMIN
            if ("SYSTEM_ADMIN".equals(roleToAssign)) {
                throw new InsufficientPrivilegesException(
                    "TENANT_ADMIN cannot assign SYSTEM_ADMIN role");
            }
            
            return; // TENANT_ADMIN can assign any other role within own tenant
        }

        // Rule 3: WAREHOUSE_MANAGER can assign operational roles to users within own tenant
        if (currentUserRoles.contains("WAREHOUSE_MANAGER")) {
            if (!targetUser.getTenantId().equals(currentUserTenantId)) {
                throw new TenantMismatchException(
                    "WAREHOUSE_MANAGER can only assign roles to users in own tenant");
            }
            
            // WAREHOUSE_MANAGER can assign operational roles only
            Set<String> allowedRoles = Set.of(
                "OPERATOR", "PICKER", "STOCK_CLERK", 
                "RECONCILIATION_CLERK", "RETURNS_CLERK", "VIEWER", "USER"
            );
            
            if (!allowedRoles.contains(roleToAssign)) {
                throw new InsufficientPrivilegesException(
                    String.format("WAREHOUSE_MANAGER cannot assign role: %s", roleToAssign));
            }
            
            return;
        }

        // Rule 4: Specialized managers can assign corresponding clerk roles
        Map<String, Set<String>> managerToClerkRoles = Map.of(
            "STOCK_MANAGER", Set.of("STOCK_CLERK", "VIEWER", "USER"),
            "LOCATION_MANAGER", Set.of("VIEWER", "USER"),
            "RECONCILIATION_MANAGER", Set.of("RECONCILIATION_CLERK", "VIEWER", "USER"),
            "RETURNS_MANAGER", Set.of("RETURNS_CLERK", "VIEWER", "USER")
        );

        for (Map.Entry<String, Set<String>> entry : managerToClerkRoles.entrySet()) {
            String managerRole = entry.getKey();
            Set<String> allowedClerkRoles = entry.getValue();
            
            if (currentUserRoles.contains(managerRole)) {
                if (!targetUser.getTenantId().equals(currentUserTenantId)) {
                    throw new TenantMismatchException(
                        String.format("%s can only assign roles to users in own tenant", managerRole));
                }
                
                if (!allowedClerkRoles.contains(roleToAssign)) {
                    throw new InsufficientPrivilegesException(
                        String.format("%s cannot assign role: %s", managerRole, roleToAssign));
                }
                
                return;
            }
        }

        // Rule 5: Other roles cannot manage roles
        throw new InsufficientPrivilegesException(
            String.format("User with roles %s cannot assign roles", currentUserRoles));
    }
}
```

### Remove Role Command Handler

```java
// RemoveUserRoleCommandHandler.java - Similar structure with validation
// Must enforce same rules as assignment, plus:
// - Cannot remove USER role (base role)
// - Cannot remove last role from user
// - SYSTEM_ADMIN removal requires existing SYSTEM_ADMIN
```

---

## Keycloak Integration

```java
// AuthenticationServiceAdapter.java
@Override
public void assignRole(KeycloakUserId keycloakUserId, String roleName) {
    RealmResource realm = keycloak.realm(keycloakProperties.getRealm());
    UserResource userResource = realm.users().get(keycloakUserId.getValue());

    // Get realm role
    RoleRepresentation role = realm.roles().get(roleName).toRepresentation();

    // Assign role to user
    userResource.roles().realmLevel().add(Collections.singletonList(role));
}

@Override
public void removeRole(KeycloakUserId keycloakUserId, String roleName) {
    RealmResource realm = keycloak.realm(keycloakProperties.getRealm());
    UserResource userResource = realm.users().get(keycloakUserId.getValue());

    // Get realm role
    RoleRepresentation role = realm.roles().get(roleName).toRepresentation();

    // Remove role from user
    userResource.roles().realmLevel().remove(Collections.singletonList(role));
}

@Override
public List<String> getUserRoles(KeycloakUserId keycloakUserId) {
    RealmResource realm = keycloak.realm(keycloakProperties.getRealm());
    UserResource userResource = realm.users().get(keycloakUserId.getValue());

    return userResource.roles().realmLevel().listEffective().stream()
        .map(RoleRepresentation::getName)
        .collect(Collectors.toList());
}
```

---

## Domain Events

```java
// UserRoleAssignedEvent.java
public class UserRoleAssignedEvent extends UserEvent<UserId> {
    private final TenantId tenantId;
    private final String roleName;

    public UserRoleAssignedEvent(UserId userId, TenantId tenantId, String roleName) {
        super(userId);
        this.tenantId = tenantId;
        this.roleName = roleName;
    }

    // Getters...
}

// UserRoleRemovedEvent.java - Similar structure
```

---

## Security and Permissions

### Role Assignment Rules (from Roles_and_Permissions_Definition.md)

#### Assignment Hierarchy

1. **SYSTEM_ADMIN**
    - Can assign any role to any user across all tenants
    - Can assign SYSTEM_ADMIN role (but only if current user is SYSTEM_ADMIN)
    - Can remove any role from any user

2. **TENANT_ADMIN**
    - Can assign any role to users within own tenant only
    - Cannot assign SYSTEM_ADMIN role
    - Cannot assign roles to users outside own tenant
    - Can remove roles from users within own tenant

3. **WAREHOUSE_MANAGER**
    - Can assign operational roles to users within own tenant:
      - OPERATOR, PICKER, STOCK_CLERK, RECONCILIATION_CLERK, RETURNS_CLERK, VIEWER, USER
    - Cannot assign manager roles or SYSTEM_ADMIN/TENANT_ADMIN
    - Can remove operational roles from users within own tenant

4. **Specialized Managers** (STOCK_MANAGER, LOCATION_MANAGER, RECONCILIATION_MANAGER, RETURNS_MANAGER)
    - Can assign corresponding clerk roles within own tenant:
      - STOCK_MANAGER → STOCK_CLERK, VIEWER, USER
      - LOCATION_MANAGER → VIEWER, USER
      - RECONCILIATION_MANAGER → RECONCILIATION_CLERK, VIEWER, USER
      - RETURNS_MANAGER → RETURNS_CLERK, VIEWER, USER
    - Cannot assign manager roles or SYSTEM_ADMIN/TENANT_ADMIN

5. **Other Roles** (OPERATOR, PICKER, CLERK roles, VIEWER, USER)
    - Cannot manage roles

### Assignment Constraints

1. **SYSTEM_ADMIN Assignment**
    - Can only be assigned by existing SYSTEM_ADMIN
    - Requires explicit approval workflow (future enhancement)

2. **Tenant Isolation**
    - Tenant-scoped roles can only be assigned to users within the same tenant
    - Cross-tenant role assignment is only allowed for SYSTEM_ADMIN

3. **Role Hierarchy**
    - Users cannot assign roles with higher privileges than their own
    - Role hierarchy: SYSTEM_ADMIN > TENANT_ADMIN > WAREHOUSE_MANAGER > OPERATOR > USER

4. **Base Role Protection**
    - USER role is always present (base role)
    - Cannot remove USER role (unless user is being deleted)

5. **Multi-Role Assignment**
    - Users can have multiple roles assigned
    - USER role is always present
    - Operational roles can be combined (e.g., PICKER + STOCK_CLERK)
    - Manager roles are typically exclusive

### Role Removal Rules

1. **SYSTEM_ADMIN Removal**
    - Can only be removed by another SYSTEM_ADMIN
    - Cannot remove last SYSTEM_ADMIN from system

2. **TENANT_ADMIN Removal**
    - Can be removed by SYSTEM_ADMIN or another TENANT_ADMIN
    - Cannot remove last TENANT_ADMIN from tenant (recommended, not enforced)

3. **USER Role**
    - Cannot be removed (base role)
    - Automatically assigned on user creation

4. **Self-Removal**
    - Users cannot remove their own roles

---

## Implementation Gaps

### Current Implementation Issues

1. **❌ Incomplete Role List**
   - Current: Only 5 roles (SYSTEM_ADMIN, TENANT_ADMIN, WAREHOUSE_MANAGER, PICKER, USER)
   - Required: All 15 roles from Roles_and_Permissions_Definition.md

2. **❌ Missing Role Assignment Validation**
   - No validation of who can assign which roles
   - No tenant-scoped role assignment enforcement
   - No SYSTEM_ADMIN assignment restriction

3. **❌ Missing Permission Checks**
   - No validation that current user has permission to assign/remove specific role
   - No role hierarchy enforcement

4. **❌ Missing Tenant Context Validation**
   - TENANT_ADMIN can potentially assign roles to users outside own tenant
   - No tenant matching validation

5. **❌ Missing Role Removal Constraints**
   - No protection against removing USER role
   - No validation of role removal permissions

6. **❌ Missing Audit Trail**
   - Role assignments not logged with assigner information
   - No audit events for role changes

### Required Implementation Tasks

1. **Update Role Validation**
   - [ ] Expand VALID_ROLES set to include all 15 roles
   - [ ] Create role constants class referencing Roles_and_Permissions_Definition.md

2. **Implement Role Assignment Validator**
   - [ ] Create RoleAssignmentValidator component
   - [ ] Implement assignment hierarchy rules
   - [ ] Implement tenant-scoped validation
   - [ ] Implement SYSTEM_ADMIN assignment restriction

3. **Update Command Handlers**
   - [ ] Update AssignUserRoleCommandHandler with validation
   - [ ] Update RemoveUserRoleCommandHandler with validation
   - [ ] Add permission checks
   - [ ] Add tenant context validation

4. **Add Security Context Service**
   - [ ] Create SecurityContextService to extract current user info
   - [ ] Extract current user roles from JWT
   - [ ] Extract current user tenant ID

5. **Enhance Domain Events**
   - [ ] Add assignedBy field to UserRoleAssignedEvent
   - [ ] Add assignedBy field to UserRoleRemovedEvent
   - [ ] Add audit logging

6. **Update Tests**
   - [ ] Test all 15 roles
   - [ ] Test assignment rules
   - [ ] Test tenant isolation
   - [ ] Test permission enforcement

---

## References

- **[Roles and Permissions Definition](../../../03-security/Roles_and_Permissions_Definition.md)** - **AUTHORITATIVE REFERENCE**
- [User Management Implementation Plan](01-User_Management_Implementation_Plan.md)
- [Security Architecture Document](../../../01-architecture/Security_Architecture_Document.md)
- [Multi-Tenancy Enforcement Guide](../../../03-security/Multi_Tenancy_Enforcement_Guide.md)
- [IAM Integration Guide](../../../03-security/IAM_Integration_Guide.md)

---

**Document Status:** Draft
**Last Updated:** 2025-12
**Next Review:** 2026-01
