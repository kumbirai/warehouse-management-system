# User Lifecycle Management Implementation Plan

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0
**Date:** 2025-12
**Status:** Draft

---

## Overview

### Purpose

Detailed implementation plan for managing user lifecycle operations: activate, deactivate, and suspend users. Ensures proper synchronization between domain model and Keycloak.

### User Status Transitions

| From      | To        | Action     | Effect                   |
|-----------|-----------|------------|--------------------------|
| ACTIVE    | INACTIVE  | Deactivate | User cannot authenticate |
| ACTIVE    | SUSPENDED | Suspend    | User cannot authenticate |
| SUSPENDED | ACTIVE    | Activate   | User can authenticate    |
| SUSPENDED | INACTIVE  | Deactivate | User cannot authenticate |
| INACTIVE  | ACTIVE    | Activate   | User can authenticate    |

---

## UI/UX Design

### Action Buttons

```typescript
{user.status === 'ACTIVE' && (
  <>
    <Button onClick={handleDeactivate}>Deactivate</Button>
    <Button onClick={handleSuspend}>Suspend</Button>
  </>
)}
{user.status === 'SUSPENDED' && (
  <>
    <Button onClick={handleActivate}>Activate</Button>
    <Button onClick={handleDeactivate}>Deactivate</Button>
  </>
)}
{user.status === 'INACTIVE' && (
  <Button onClick={handleActivate}>Activate</Button>
)}
```

### Confirmation Dialog Example

```
┌─────────────────────────────────────────┐
│  Deactivate User                        │
├─────────────────────────────────────────┤
│  Are you sure you want to deactivate    │
│  this user?                             │
│                                         │
│  Username: john.doe                     │
│  Email: john.doe@ldp001.com             │
│                                         │
│  This will:                             │
│  • Prevent user from logging in         │
│  • Disable user account in Keycloak     │
│                                         │
│  [Cancel]              [Deactivate]     │
└─────────────────────────────────────────┘
```

---

## Backend Implementation

### API Endpoints

- `PUT /api/v1/users/{id}/activate` - Activate user
- `PUT /api/v1/users/{id}/deactivate` - Deactivate user
- `PUT /api/v1/users/{id}/suspend` - Suspend user

### Command Handlers

```java
// ActivateUserCommandHandler.java
@Component
@Transactional
public class ActivateUserCommandHandler {
    private final UserRepository userRepository;
    private final UserEventPublisher eventPublisher;
    private final AuthenticationServicePort authenticationService;

    public void handle(ActivateUserCommand command) {
        UserId userId = command.getUserId();

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId.getValue()));

        // Validate can activate
        if (!user.canActivate()) {
            throw new IllegalStateException("Cannot activate user: current status is " + user.getStatus());
        }

        // Activate user (domain logic)
        user.activate();

        // Persist
        userRepository.save(user);

        // Enable user in Keycloak
        if (user.getKeycloakUserId().isPresent()) {
            authenticationService.enableUser(user.getKeycloakUserId().get());
        }

        // Publish events
        eventPublisher.publish(user.getDomainEvents());
        user.clearDomainEvents();
    }
}

// DeactivateUserCommandHandler.java - Similar structure
// SuspendUserCommandHandler.java - Similar structure
```

---

## Domain Modeling

### User Entity Methods

```java
// User.java (domain core)
public void activate() {
    if (this.status == UserStatus.ACTIVE) {
        throw new IllegalStateException("User is already active");
    }
    if (!this.status.canTransitionTo(UserStatus.ACTIVE)) {
        throw new IllegalStateException("Cannot activate user: invalid status transition");
    }

    this.status = UserStatus.ACTIVE;
    this.lastModifiedAt = LocalDateTime.now();
    incrementVersion();

    addDomainEvent(new UserUpdatedEvent(
        this.getId(),
        this.getTenantId(),
        this.status,
        Description.of("User activated")
    ));
}

public void deactivate() {
    if (this.status == UserStatus.INACTIVE) {
        throw new IllegalStateException("User is already inactive");
    }
    if (!this.status.canTransitionTo(UserStatus.INACTIVE)) {
        throw new IllegalStateException("Cannot deactivate user");
    }

    this.status = UserStatus.INACTIVE;
    this.lastModifiedAt = LocalDateTime.now();
    incrementVersion();

    addDomainEvent(new UserDeactivatedEvent(this.getId(), this.getTenantId()));
}

public void suspend() {
    if (this.status == UserStatus.SUSPENDED) {
        throw new IllegalStateException("User is already suspended");
    }
    if (!this.status.canTransitionTo(UserStatus.SUSPENDED)) {
        throw new IllegalStateException("Cannot suspend user");
    }

    this.status = UserStatus.SUSPENDED;
    this.lastModifiedAt = LocalDateTime.now();
    incrementVersion();

    addDomainEvent(new UserUpdatedEvent(
        this.getId(),
        this.getTenantId(),
        this.status,
        Description.of("User suspended")
    ));
}
```

---

## Keycloak Integration

```java
// AuthenticationServiceAdapter.java
@Override
public void enableUser(KeycloakUserId keycloakUserId) {
    RealmResource realm = keycloak.realm(keycloakProperties.getRealm());
    UserResource userResource = realm.users().get(keycloakUserId.getValue());

    UserRepresentation user = userResource.toRepresentation();
    user.setEnabled(true);
    userResource.update(user);
}

@Override
public void disableUser(KeycloakUserId keycloakUserId) {
    RealmResource realm = keycloak.realm(keycloakProperties.getRealm());
    UserResource userResource = realm.users().get(keycloakUserId.getValue());

    UserRepresentation user = userResource.toRepresentation();
    user.setEnabled(false);
    userResource.update(user);
}
```

---

## Security and Permissions

### Permission Rules

- **TENANT_ADMIN:** Can manage users in own tenant only
- **SYSTEM_ADMIN:** Can manage all users

---

**Document Status:** Draft
**Last Updated:** 2025-12
