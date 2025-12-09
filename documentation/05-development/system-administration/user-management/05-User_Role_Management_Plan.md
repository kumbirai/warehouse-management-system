# User Role Management Implementation Plan

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0
**Date:** 2025-12
**Status:** Draft

---

## Overview

### Purpose

Detailed implementation plan for managing user roles and permissions. Ensures proper role assignment and synchronization with Keycloak.

### Available Roles

- **SYSTEM_ADMIN** - Full system access across all tenants
- **TENANT_ADMIN** - Full access within own tenant
- **WAREHOUSE_MANAGER** - Warehouse operations management
- **PICKER** - Picking operations
- **USER** - Basic user access

---

## UI/UX Design

### Role Management Interface

```
┌─────────────────────────────────────────────────┐
│  User Roles: john.doe                           │
├─────────────────────────────────────────────────┤
│  Current Roles:                                 │
│  • USER                                         │
│  • PICKER                                       │
│                                                 │
│  Available Roles:                               │
│  ┌─────────────────────────────────────────┐   │
│  │ ☐ SYSTEM_ADMIN                          │   │
│  │ ☐ TENANT_ADMIN                          │   │
│  │ ☐ WAREHOUSE_MANAGER                     │   │
│  │ ☑ PICKER                                │   │
│  │ ☑ USER                                  │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  [Cancel]                    [Save Changes]     │
└─────────────────────────────────────────────────┘
```

---

## Backend Implementation

### API Endpoints

- `POST /api/v1/users/{id}/roles` - Assign role to user
- `DELETE /api/v1/users/{id}/roles/{roleId}` - Remove role from user
- `GET /api/v1/users/{id}/roles` - Get user roles

### Command Handlers

```java
// AssignUserRoleCommandHandler.java
@Component
@Transactional
public class AssignUserRoleCommandHandler {
    private final UserRepository userRepository;
    private final UserEventPublisher eventPublisher;
    private final AuthenticationServicePort authenticationService;

    public void handle(AssignUserRoleCommand command) {
        UserId userId = command.getUserId();
        String roleName = command.getRoleName();

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Validate role
        if (!isValidRole(roleName)) {
            throw new IllegalArgumentException("Invalid role: " + roleName);
        }

        // Assign role in Keycloak
        if (user.getKeycloakUserId().isPresent()) {
            authenticationService.assignRole(user.getKeycloakUserId().get(), roleName);
        }

        // Update last modified timestamp
        user.setLastModifiedAt(LocalDateTime.now());
        user.incrementVersion();
        userRepository.save(user);

        // Publish event
        UserRoleAssignedEvent event = new UserRoleAssignedEvent(
            user.getId(),
            user.getTenantId(),
            roleName
        );
        eventPublisher.publish(Collections.singletonList(event));
    }

    private boolean isValidRole(String roleName) {
        return Set.of("SYSTEM_ADMIN", "TENANT_ADMIN", "WAREHOUSE_MANAGER", "PICKER", "USER")
                .contains(roleName);
    }
}

// RemoveUserRoleCommandHandler.java - Similar structure
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

### Permission Rules

1. **SYSTEM_ADMIN**
    - Can assign/remove any role
    - Can assign SYSTEM_ADMIN role

2. **TENANT_ADMIN**
    - Can assign/remove roles within own tenant
    - Cannot assign SYSTEM_ADMIN role
    - Cannot assign TENANT_ADMIN role to users outside own tenant

3. **Other Roles**
    - Cannot manage roles

---

**Document Status:** Draft
**Last Updated:** 2025-12
