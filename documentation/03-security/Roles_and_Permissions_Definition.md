# Roles and Permissions Definition

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-01  
**Status:** Draft  
**Related Documents:**

- [Security Architecture Document](../01-architecture/Security_Architecture_Document.md)
- [Multi-Tenancy Enforcement Guide](Multi_Tenancy_Enforcement_Guide.md)
- [IAM Integration Guide](IAM_Integration_Guide.md)
- [Business Requirements Document](../00-business-requirements/business-requirements-document.md)

---

## Table of Contents

1. [Overview](#overview)
2. [Role Hierarchy](#role-hierarchy)
3. [System-Level Roles](#system-level-roles)
4. [Tenant-Level Roles](#tenant-level-roles)
5. [Operational Roles](#operational-roles)
6. [Permission Model](#permission-model)
7. [Role-to-Service Mapping](#role-to-service-mapping)
8. [Role Assignment Rules](#role-assignment-rules)
9. [Keycloak Configuration](#keycloak-configuration)
10. [Implementation Guidelines](#implementation-guidelines)

---

## Overview

### Purpose

This document defines all roles and permissions required for the Warehouse Management System. Roles are designed to support multi-tenant operations while ensuring proper access control across all system capabilities.

### Role Categories

Roles are organized into three categories:

1. **System-Level Roles** - Cross-tenant administrative access
2. **Tenant-Level Roles** - Administrative and management access within a tenant
3. **Operational Roles** - Day-to-day warehouse operations

### Design Principles

1. **Principle of Least Privilege** - Users are granted minimum permissions necessary for their role
2. **Tenant Isolation** - Tenant-level roles are scoped to their tenant only
3. **Separation of Duties** - Critical operations require appropriate authorization
4. **Audit Trail** - All role assignments and permission changes are logged

---

## Role Hierarchy

```
SYSTEM_ADMIN (Cross-tenant)
    │
    ├── TENANT_ADMIN (Tenant-scoped)
    │       │
    │       ├── WAREHOUSE_MANAGER (Tenant-scoped)
    │       │       │
    │       │       ├── STOCK_MANAGER (Tenant-scoped)
    │       │       ├── LOCATION_MANAGER (Tenant-scoped)
    │       │       ├── RECONCILIATION_MANAGER (Tenant-scoped)
    │       │       └── RETURNS_MANAGER (Tenant-scoped)
    │       │
    │       └── OPERATOR (Tenant-scoped)
    │               │
    │               ├── PICKER (Tenant-scoped)
    │               ├── STOCK_CLERK (Tenant-scoped)
    │               ├── RECONCILIATION_CLERK (Tenant-scoped)
    │               └── RETURNS_CLERK (Tenant-scoped)
    │
    └── VIEWER (Tenant-scoped, read-only)
```

---

## System-Level Roles

### SYSTEM_ADMIN

**Description:** Full system access across all tenants. Reserved for system administrators and support staff.

**Scope:** Cross-tenant (can access all tenants)

**Key Responsibilities:**
- Manage all tenants (create, activate, deactivate, suspend)
- Manage users across all tenants
- Configure system-wide settings
- Access system monitoring and logs
- Manage integration configurations
- Override tenant-level restrictions when necessary

**Permissions:**
- `tenant:*` - Full tenant management
- `user:*` - Full user management across all tenants
- `system:*` - System configuration and monitoring
- `integration:*` - Integration service management
- `audit:*` - Access to audit logs across all tenants

**Service Access:**
- **Tenant Service:** Full access (all tenants)
- **User Service:** Full access (all tenants)
- **Integration Service:** Full access
- **All Other Services:** Read-only access (for monitoring)

**Keycloak Configuration:**
- Realm role: `SYSTEM_ADMIN`
- Can bypass tenant context validation
- Special handling in `TenantContextInterceptor`

**Assignment Rules:**
- Only assigned by existing SYSTEM_ADMIN users
- Requires explicit approval workflow
- Limited to IT/Operations staff

---

## Tenant-Level Roles

### TENANT_ADMIN

**Description:** Full administrative access within a tenant. Manages tenant configuration, users, and operations.

**Scope:** Single tenant (own tenant only)

**Key Responsibilities:**
- Manage users within own tenant
- Configure tenant-specific settings
- Assign roles to users within tenant
- View all operations and reports within tenant
- Manage tenant configuration
- Approve critical operations (if required)

**Permissions:**
- `tenant:read` - View tenant information
- `tenant:write` - Update tenant configuration (limited)
- `user:*` - Full user management within tenant
- `role:assign` - Assign roles to users within tenant
- `*:read` - Read access to all tenant data
- `*:write` - Write access to all tenant operations (delegated to managers)

**Service Access:**
- **User Service:** Full access (own tenant only)
- **Tenant Service:** Read access (own tenant only)
- **All Other Services:** Full access (own tenant only)

**Keycloak Configuration:**
- Realm role: `TENANT_ADMIN`
- Tenant ID must match user's tenant_id attribute

**Assignment Rules:**
- Assigned by SYSTEM_ADMIN or existing TENANT_ADMIN
- One TENANT_ADMIN per tenant recommended (but not enforced)

---

### WAREHOUSE_MANAGER

**Description:** Warehouse operations management within a tenant. Oversees daily warehouse operations and manages operational staff.

**Scope:** Single tenant (own tenant only)

**Key Responsibilities:**
- Oversee warehouse operations
- Manage picking operations
- Monitor stock levels and restock requests
- Manage location assignments
- Review and approve reconciliation variances
- Manage returns processing
- Generate operational reports
- Assign tasks to operational staff

**Permissions:**
- `stock:*` - Full stock management
- `location:*` - Full location management
- `picking:*` - Full picking operations management
- `returns:*` - Full returns management
- `reconciliation:*` - Full reconciliation management
- `report:*` - Generate and view reports
- `user:read` - View users within tenant (for task assignment)

**Service Access:**
- **Stock Management Service:** Full access
- **Location Management Service:** Full access
- **Picking Service:** Full access
- **Returns Service:** Full access
- **Reconciliation Service:** Full access
- **Product Service:** Read access
- **User Service:** Read access (own tenant only)

**Keycloak Configuration:**
- Realm role: `WAREHOUSE_MANAGER`
- Tenant ID must match user's tenant_id attribute

**Assignment Rules:**
- Assigned by TENANT_ADMIN or SYSTEM_ADMIN
- Multiple WAREHOUSE_MANAGER users per tenant allowed

---

### STOCK_MANAGER

**Description:** Specialized role for stock management operations. Focuses on stock consignment, classification, and level management.

**Scope:** Single tenant (own tenant only)

**Key Responsibilities:**
- Manage stock consignment receipt and confirmation
- Classify stock by expiration dates
- Monitor stock levels and thresholds
- Generate and approve restock requests
- Manage stock classification rules
- Review stock expiration alerts
- Coordinate with integration service for D365 sync

**Permissions:**
- `stock:consignment:*` - Full consignment management
- `stock:classification:*` - Stock classification management
- `stock:level:*` - Stock level management
- `stock:restock:*` - Restock request management
- `stock:expiration:*` - Expiration tracking and alerts
- `product:read` - Read product master data
- `location:read` - Read location information

**Service Access:**
- **Stock Management Service:** Full access
- **Product Service:** Read access
- **Location Management Service:** Read access
- **Integration Service:** Read access (for D365 sync status)

**Keycloak Configuration:**
- Realm role: `STOCK_MANAGER`
- Tenant ID must match user's tenant_id attribute

**Assignment Rules:**
- Assigned by TENANT_ADMIN, WAREHOUSE_MANAGER, or SYSTEM_ADMIN

---

### LOCATION_MANAGER

**Description:** Specialized role for warehouse location management. Manages location assignments, movements, and capacity.

**Scope:** Single tenant (own tenant only)

**Key Responsibilities:**
- Manage warehouse location master data
- Assign locations to stock based on FEFO principles
- Track and manage stock movements
- Manage location status (occupied, available, reserved, blocked)
- Enforce location capacity limits
- Generate location barcodes
- Optimize location assignments

**Permissions:**
- `location:*` - Full location management
- `location:movement:*` - Stock movement management
- `location:capacity:*` - Location capacity management
- `location:barcode:*` - Location barcode management
- `stock:read` - Read stock information
- `product:read` - Read product information

**Service Access:**
- **Location Management Service:** Full access
- **Stock Management Service:** Read access
- **Product Service:** Read access

**Keycloak Configuration:**
- Realm role: `LOCATION_MANAGER`
- Tenant ID must match user's tenant_id attribute

**Assignment Rules:**
- Assigned by TENANT_ADMIN, WAREHOUSE_MANAGER, or SYSTEM_ADMIN

---

### RECONCILIATION_MANAGER

**Description:** Specialized role for stock reconciliation operations. Manages daily stock counts and reconciliation with D365.

**Scope:** Single tenant (own tenant only)

**Key Responsibilities:**
- Generate electronic stock count worksheets
- Manage stock count execution
- Review and approve stock count variances
- Reconcile stock counts with D365
- Manage reconciliation workflows
- Review reconciliation reports
- Approve variance adjustments

**Permissions:**
- `reconciliation:*` - Full reconciliation management
- `reconciliation:count:*` - Stock count management
- `reconciliation:variance:*` - Variance approval
- `reconciliation:d365:*` - D365 reconciliation
- `stock:read` - Read stock information
- `location:read` - Read location information
- `report:reconciliation:*` - Reconciliation reports

**Service Access:**
- **Reconciliation Service:** Full access
- **Stock Management Service:** Read access
- **Location Management Service:** Read access
- **Integration Service:** Read access (for D365 sync)

**Keycloak Configuration:**
- Realm role: `RECONCILIATION_MANAGER`
- Tenant ID must match user's tenant_id attribute

**Assignment Rules:**
- Assigned by TENANT_ADMIN, WAREHOUSE_MANAGER, or SYSTEM_ADMIN

---

### RETURNS_MANAGER

**Description:** Specialized role for returns management. Handles all return processing and reconciliation.

**Scope:** Single tenant (own tenant only)

**Key Responsibilities:**
- Process partial order acceptances
- Process full order returns
- Handle damage-in-transit returns
- Assign return locations
- Reconcile returns with D365
- Review returns reports
- Manage return workflows

**Permissions:**
- `returns:*` - Full returns management
- `returns:partial:*` - Partial order acceptance
- `returns:full:*` - Full order returns
- `returns:damage:*` - Damage-in-transit handling
- `returns:reconciliation:*` - Returns reconciliation
- `location:read` - Read location information
- `stock:read` - Read stock information
- `picking:read` - Read picking information

**Service Access:**
- **Returns Service:** Full access
- **Stock Management Service:** Read access
- **Location Management Service:** Read access
- **Picking Service:** Read access
- **Integration Service:** Read access (for D365 sync)

**Keycloak Configuration:**
- Realm role: `RETURNS_MANAGER`
- Tenant ID must match user's tenant_id attribute

**Assignment Rules:**
- Assigned by TENANT_ADMIN, WAREHOUSE_MANAGER, or SYSTEM_ADMIN

---

## Operational Roles

### OPERATOR

**Description:** General operational access for warehouse staff. Can perform day-to-day warehouse operations.

**Scope:** Single tenant (own tenant only)

**Key Responsibilities:**
- Execute picking operations
- Perform stock movements
- Execute stock counts
- Process returns
- Scan barcodes
- Update stock information
- View operational data

**Permissions:**
- `picking:execute` - Execute picking tasks
- `stock:movement:execute` - Execute stock movements
- `reconciliation:count:execute` - Execute stock counts
- `returns:process` - Process returns
- `*:read` - Read access to operational data
- `barcode:scan` - Barcode scanning

**Service Access:**
- **Picking Service:** Execute operations
- **Stock Management Service:** Read and limited write
- **Location Management Service:** Read and limited write
- **Reconciliation Service:** Execute counts
- **Returns Service:** Process returns
- **Product Service:** Read access

**Keycloak Configuration:**
- Realm role: `OPERATOR`
- Tenant ID must match user's tenant_id attribute

**Assignment Rules:**
- Assigned by TENANT_ADMIN, WAREHOUSE_MANAGER, or SYSTEM_ADMIN
- Most common role for warehouse staff

---

### PICKER

**Description:** Specialized role for picking operations. Focuses exclusively on picking tasks.

**Scope:** Single tenant (own tenant only)

**Key Responsibilities:**
- Execute picking tasks from picking lists
- Scan product and location barcodes
- Update picking status
- Complete picking operations
- View picking instructions
- Report picking issues

**Permissions:**
- `picking:read` - Read picking lists and tasks
- `picking:execute` - Execute picking operations
- `picking:update` - Update picking status
- `location:read` - Read location information
- `product:read` - Read product information
- `barcode:scan` - Barcode scanning

**Service Access:**
- **Picking Service:** Execute operations
- **Location Management Service:** Read access
- **Product Service:** Read access
- **Stock Management Service:** Read access (for stock availability)

**Keycloak Configuration:**
- Realm role: `PICKER`
- Tenant ID must match user's tenant_id attribute

**Assignment Rules:**
- Assigned by TENANT_ADMIN, WAREHOUSE_MANAGER, or SYSTEM_ADMIN
- Can be combined with other operational roles

---

### STOCK_CLERK

**Description:** Specialized role for stock receipt and management operations.

**Scope:** Single tenant (own tenant only)

**Key Responsibilities:**
- Receive stock consignments
- Confirm stock receipt
- Classify stock by expiration dates
- Assign stock to locations
- Update stock information
- Scan product and location barcodes

**Permissions:**
- `stock:consignment:receive` - Receive stock consignments
- `stock:consignment:confirm` - Confirm stock receipt
- `stock:classification:assign` - Assign stock classification
- `stock:location:assign` - Assign stock to locations
- `stock:update` - Update stock information
- `location:read` - Read location information
- `product:read` - Read product information
- `barcode:scan` - Barcode scanning

**Service Access:**
- **Stock Management Service:** Limited write access
- **Location Management Service:** Read access
- **Product Service:** Read access

**Keycloak Configuration:**
- Realm role: `STOCK_CLERK`
- Tenant ID must match user's tenant_id attribute

**Assignment Rules:**
- Assigned by TENANT_ADMIN, WAREHOUSE_MANAGER, STOCK_MANAGER, or SYSTEM_ADMIN

---

### RECONCILIATION_CLERK

**Description:** Specialized role for stock counting operations.

**Scope:** Single tenant (own tenant only)

**Key Responsibilities:**
- Execute stock counts using electronic worksheets
- Scan location and product barcodes
- Enter count quantities
- Complete stock count worksheets
- Report count discrepancies

**Permissions:**
- `reconciliation:count:execute` - Execute stock counts
- `reconciliation:count:enter` - Enter count quantities
- `reconciliation:count:complete` - Complete count worksheets
- `location:read` - Read location information
- `product:read` - Read product information
- `barcode:scan` - Barcode scanning

**Service Access:**
- **Reconciliation Service:** Execute counts
- **Location Management Service:** Read access
- **Product Service:** Read access
- **Stock Management Service:** Read access

**Keycloak Configuration:**
- Realm role: `RECONCILIATION_CLERK`
- Tenant ID must match user's tenant_id attribute

**Assignment Rules:**
- Assigned by TENANT_ADMIN, WAREHOUSE_MANAGER, RECONCILIATION_MANAGER, or SYSTEM_ADMIN

---

### RETURNS_CLERK

**Description:** Specialized role for returns processing operations.

**Scope:** Single tenant (own tenant only)

**Key Responsibilities:**
- Process returned orders
- Scan returned products
- Record return reasons
- Assign return locations
- Update return status
- Scan barcodes

**Permissions:**
- `returns:process` - Process returns
- `returns:record` - Record return information
- `returns:location:assign` - Assign return locations
- `returns:update` - Update return status
- `location:read` - Read location information
- `product:read` - Read product information
- `barcode:scan` - Barcode scanning

**Service Access:**
- **Returns Service:** Process returns
- **Location Management Service:** Read access
- **Product Service:** Read access
- **Stock Management Service:** Read access

**Keycloak Configuration:**
- Realm role: `RETURNS_CLERK`
- Tenant ID must match user's tenant_id attribute

**Assignment Rules:**
- Assigned by TENANT_ADMIN, WAREHOUSE_MANAGER, RETURNS_MANAGER, or SYSTEM_ADMIN

---

### VIEWER

**Description:** Read-only access for viewing warehouse data and reports. No write or execute permissions.

**Scope:** Single tenant (own tenant only)

**Key Responsibilities:**
- View stock levels and information
- View location information
- View picking lists and status
- View returns information
- View reconciliation reports
- Generate and view reports
- View product master data

**Permissions:**
- `*:read` - Read access to all tenant data
- `report:view` - View reports
- No write or execute permissions

**Service Access:**
- **All Services:** Read-only access

**Keycloak Configuration:**
- Realm role: `VIEWER`
- Tenant ID must match user's tenant_id attribute

**Assignment Rules:**
- Assigned by TENANT_ADMIN, WAREHOUSE_MANAGER, or SYSTEM_ADMIN
- Suitable for auditors, analysts, or read-only users

---

### USER

**Description:** Basic user access. Minimal permissions for basic system interaction.

**Scope:** Single tenant (own tenant only)

**Key Responsibilities:**
- View own profile
- Update own profile
- View basic system information
- Access system (with limited functionality)

**Permissions:**
- `user:profile:read` - Read own profile
- `user:profile:update` - Update own profile
- `system:read` - Read basic system information

**Service Access:**
- **User Service:** Own profile only
- **All Other Services:** No access (or very limited read access)

**Keycloak Configuration:**
- Realm role: `USER`
- Tenant ID must match user's tenant_id attribute

**Assignment Rules:**
- Default role for all users
- Can be combined with other roles
- Assigned automatically on user creation

---

### SERVICE

**Description:** Service-to-service communication role. Used for inter-service API calls and system integrations.

**Scope:** System-wide (no tenant restriction)

**Key Responsibilities:**
- Service-to-service API calls
- System integration operations
- Internal service communication
- Automated system operations

**Permissions:**
- `tenant:read` - Read tenant information (for service operations)
- `integration:*` - Integration service operations
- Service-specific permissions as needed

**Service Access:**
- **All Services:** Limited access for service-to-service communication
- **Tenant Service:** Read access (for realm lookup)
- **Integration Service:** Full access

**Keycloak Configuration:**
- Realm role: `SERVICE`
- No tenant ID required (system-level role)
- Used by service accounts and integration clients

**Assignment Rules:**
- Assigned by SYSTEM_ADMIN only
- Used for service accounts, not human users
- Required for inter-service communication

---

## Permission Model

### Permission Format

Permissions follow the format: `{resource}:{action}` or `{resource}:{sub-resource}:{action}`

**Examples:**
- `stock:read` - Read stock information
- `stock:write` - Write stock information
- `stock:consignment:receive` - Receive stock consignments
- `picking:execute` - Execute picking operations
- `reconciliation:count:execute` - Execute stock counts

### Permission Categories

#### Resource Categories

1. **tenant** - Tenant management
2. **user** - User management
3. **stock** - Stock management
4. **location** - Location management
5. **picking** - Picking operations
6. **returns** - Returns management
7. **reconciliation** - Reconciliation operations
8. **product** - Product master data
9. **integration** - Integration management
10. **report** - Reporting
11. **system** - System configuration
12. **audit** - Audit logs
13. **barcode** - Barcode operations

#### Action Types

1. **read** - Read/view access
2. **write** - Create/update access
3. **delete** - Delete access
4. **execute** - Execute operations
5. **approve** - Approve operations
6. **assign** - Assign resources
7. *** (wildcard)** - All actions

### Permission Inheritance

Roles inherit permissions from parent roles in the hierarchy:

- **WAREHOUSE_MANAGER** inherits permissions from **OPERATOR**
- **OPERATOR** inherits permissions from **VIEWER**
- Specialized manager roles inherit from **WAREHOUSE_MANAGER**

### Permission Enforcement

Permissions are enforced at multiple layers:

1. **Gateway Level** - Role validation in JWT token
2. **Service Level** - `@PreAuthorize` annotations on endpoints
3. **Domain Level** - Business rule validation

---

## Role-to-Service Mapping

### Service Access Matrix

| Role | Tenant Service | User Service | Stock Management | Location Management | Product Service | Picking Service | Returns Service | Reconciliation Service | Integration Service | Notification Service |
|------|---------------|--------------|------------------|---------------------|-----------------|-----------------|----------------|------------------------|---------------------|---------------------|
| SYSTEM_ADMIN | Full (all) | Full (all) | Read (all) | Read (all) | Read (all) | Read (all) | Read (all) | Read (all) | Full | Read |
| TENANT_ADMIN | Read (own) | Full (own) | Full (own) | Full (own) | Read (own) | Full (own) | Full (own) | Full (own) | Read | Read |
| WAREHOUSE_MANAGER | - | Read (own) | Full (own) | Full (own) | Read (own) | Full (own) | Full (own) | Full (own) | Read | Read |
| STOCK_MANAGER | - | - | Full (own) | Read (own) | Read (own) | - | - | - | Read | Read |
| LOCATION_MANAGER | - | - | Read (own) | Full (own) | Read (own) | - | - | - | - | - |
| RECONCILIATION_MANAGER | - | - | Read (own) | Read (own) | Read (own) | - | - | Full (own) | Read | Read |
| RETURNS_MANAGER | - | - | Read (own) | Read (own) | Read (own) | Read (own) | Full (own) | - | Read | Read |
| OPERATOR | - | - | Limited (own) | Limited (own) | Read (own) | Execute (own) | Process (own) | Execute (own) | - | - |
| PICKER | - | - | Read (own) | Read (own) | Read (own) | Execute (own) | - | - | - | - |
| STOCK_CLERK | - | - | Limited (own) | Read (own) | Read (own) | - | - | - | - | - |
| RECONCILIATION_CLERK | - | - | Read (own) | Read (own) | Read (own) | - | - | Execute (own) | - | - |
| RETURNS_CLERK | - | - | Read (own) | Read (own) | Read (own) | - | Process (own) | - | - | - |
| VIEWER | - | - | Read (own) | Read (own) | Read (own) | Read (own) | Read (own) | Read (own) | - | - |
| USER | - | Own profile | - | - | - | - | - | - | - | - |
| SERVICE | Read (all) | - | - | - | - | - | - | - | Full | - |

**Legend:**
- **Full** - Full read/write/execute access
- **Read** - Read-only access
- **Limited** - Limited write access (specific operations only)
- **Execute** - Execute operations only
- **Process** - Process operations only
- **Own** - Own tenant only
- **All** - All tenants (SYSTEM_ADMIN only)
- **-** - No access

---

## Role Assignment Rules

### Assignment Hierarchy

1. **SYSTEM_ADMIN** can assign any role to any user
2. **TENANT_ADMIN** can assign any role to users within own tenant
3. **WAREHOUSE_MANAGER** can assign operational roles to users within own tenant
4. Specialized managers can assign corresponding clerk roles within own tenant

### Assignment Constraints

1. **SYSTEM_ADMIN** can only be assigned by existing SYSTEM_ADMIN
2. **TENANT_ADMIN** can be assigned by SYSTEM_ADMIN or existing TENANT_ADMIN
3. Tenant-scoped roles can only be assigned to users within the same tenant
4. Users cannot assign roles with higher privileges than their own
5. Role assignments are logged for audit purposes

### Multi-Role Assignment

Users can have multiple roles assigned:

- **USER** role is always present (base role)
- Operational roles can be combined (e.g., PICKER + STOCK_CLERK)
- Manager roles are typically exclusive
- SYSTEM_ADMIN and TENANT_ADMIN are typically exclusive

**Example Combinations:**
- PICKER + STOCK_CLERK (warehouse worker who picks and receives stock)
- OPERATOR + VIEWER (operator who also needs read access to reports)
- USER + VIEWER (read-only user with profile access)

### Role Removal

1. **SYSTEM_ADMIN** role can only be removed by another SYSTEM_ADMIN
2. **TENANT_ADMIN** role can be removed by SYSTEM_ADMIN or another TENANT_ADMIN
3. Users cannot remove their own roles
4. Role removals are logged for audit purposes
5. Removing a role does not affect other assigned roles

---

## Keycloak Configuration

### Realm Roles

All roles must be created as **realm roles** in Keycloak:

1. **SYSTEM_ADMIN**
2. **TENANT_ADMIN**
3. **WAREHOUSE_MANAGER**
4. **STOCK_MANAGER**
5. **LOCATION_MANAGER**
6. **RECONCILIATION_MANAGER**
7. **RETURNS_MANAGER**
8. **OPERATOR**
9. **PICKER**
10. **STOCK_CLERK**
11. **RECONCILIATION_CLERK**
12. **RETURNS_CLERK**
13. **VIEWER**
14. **USER**
15. **SERVICE** - Service-to-service communication (system role)

### Role Configuration

**Realm:** `wms-realm`

**Role Type:** Realm Role

**Role Attributes:**
- Role name must match exactly (case-sensitive)
- Roles are included in JWT token `realm_access.roles` claim
- Roles are validated by gateway and services

### Token Claims

Roles are included in JWT access tokens:

```json
{
  "sub": "user-uuid",
  "tenant_id": "ldp-123",
  "realm_access": {
    "roles": ["USER", "PICKER", "STOCK_CLERK"]
  },
  "exp": 1234567890,
  "iat": 1234567890,
  "iss": "http://keycloak:7080/realms/wms-realm"
}
```

### Role Assignment in Keycloak

Roles are assigned to users via:

1. **User Service API** - Primary method (synchronized with Keycloak)
2. **Keycloak Admin Console** - Direct assignment (for emergency cases)
3. **Keycloak Admin API** - Programmatic assignment

**Note:** User Service is the source of truth for role assignments. Direct Keycloak assignments should be avoided except for emergency cases.

---

## Implementation Guidelines

### Service-Level Authorization

Use `@PreAuthorize` annotations on controller endpoints:

```java
@PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'TENANT_ADMIN')")
@PostMapping("/api/v1/stock-management/consignments")
public ResponseEntity<StockConsignmentResponse> createConsignment(
    @RequestBody CreateStockConsignmentCommand command) {
    // Implementation
}
```

### Role Validation

Validate roles in application services:

```java
@Component
public class CreateStockConsignmentHandler {
    
    public void handle(CreateStockConsignmentCommand command) {
        // Validate user has required role
        if (!hasRequiredRole("STOCK_MANAGER", "WAREHOUSE_MANAGER", "TENANT_ADMIN")) {
            throw new InsufficientPrivilegesException("User does not have required role");
        }
        
        // Process command
    }
}
```

### Tenant Context Validation

Always validate tenant context matches user's tenant:

```java
TenantId userTenantId = TenantContext.getTenantId();
if (!userTenantId.equals(command.getTenantId())) {
    throw new TenantMismatchException("Tenant mismatch");
}
```

**Exception:** SYSTEM_ADMIN can access all tenants (tenant context validation bypassed).

### Role Hierarchy Enforcement

Implement role hierarchy in permission checks:

```java
private boolean hasRequiredRole(String... requiredRoles) {
    List<String> userRoles = getCurrentUserRoles();
    
    // Check direct role match
    for (String role : requiredRoles) {
        if (userRoles.contains(role)) {
            return true;
        }
    }
    
    // Check role hierarchy
    if (userRoles.contains("WAREHOUSE_MANAGER")) {
        return Arrays.asList(requiredRoles).contains("OPERATOR");
    }
    
    if (userRoles.contains("TENANT_ADMIN")) {
        return true; // TENANT_ADMIN has all tenant permissions
    }
    
    return false;
}
```

### Audit Logging

Log all role-related operations:

```java
@EventListener
public void handleUserRoleAssigned(UserRoleAssignedEvent event) {
    auditService.log(
        "ROLE_ASSIGNED",
        event.getUserId(),
        event.getTenantId(),
        Map.of(
            "role", event.getRoleName(),
            "assignedBy", getCurrentUserId(),
            "timestamp", LocalDateTime.now()
        )
    );
}
```

---

## Role Definitions Summary

### Quick Reference

| Role | Level | Scope | Primary Function |
|------|-------|-------|------------------|
| SYSTEM_ADMIN | System | Cross-tenant | System administration |
| TENANT_ADMIN | Tenant | Single tenant | Tenant administration |
| WAREHOUSE_MANAGER | Tenant | Single tenant | Warehouse operations management |
| STOCK_MANAGER | Tenant | Single tenant | Stock management |
| LOCATION_MANAGER | Tenant | Single tenant | Location management |
| RECONCILIATION_MANAGER | Tenant | Single tenant | Reconciliation management |
| RETURNS_MANAGER | Tenant | Single tenant | Returns management |
| OPERATOR | Tenant | Single tenant | General operations |
| PICKER | Tenant | Single tenant | Picking operations |
| STOCK_CLERK | Tenant | Single tenant | Stock receipt operations |
| RECONCILIATION_CLERK | Tenant | Single tenant | Stock counting operations |
| RETURNS_CLERK | Tenant | Single tenant | Returns processing |
| VIEWER | Tenant | Single tenant | Read-only access |
| USER | Tenant | Single tenant | Basic user access |
| SERVICE | System | System-wide | Service-to-service communication |

---

## References

- [Security Architecture Document](../01-architecture/Security_Architecture_Document.md)
- [Multi-Tenancy Enforcement Guide](Multi_Tenancy_Enforcement_Guide.md)
- [IAM Integration Guide](IAM_Integration_Guide.md)
- [User Management Implementation Plan](../05-development/system-administration/user-management/01-User_Management_Implementation_Plan.md)
- [User Role Management Plan](../05-development/system-administration/user-management/05-User_Role_Management_Plan.md)
- [Business Requirements Document](../00-business-requirements/business-requirements-document.md)

---

**Document Status:** Draft  
**Last Updated:** 2025-01  
**Next Review:** 2025-04

