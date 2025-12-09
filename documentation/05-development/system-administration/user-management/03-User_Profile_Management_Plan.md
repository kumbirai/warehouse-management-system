# User Profile Management Implementation Plan

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0
**Date:** 2025-12
**Status:** Draft
**Related Documents:**

- [User Management Implementation Plan](01-User_Management_Implementation_Plan.md)
- [Service Architecture Document](../../../01-architecture/Service_Architecture_Document.md)
- [API Specifications](../../../02-api/API_Specifications.md)

---

## Table of Contents

1. [Overview](#overview)
2. [UI/UX Design](#uiux-design)
3. [Frontend Implementation](#frontend-implementation)
4. [Backend Data Flow](#backend-data-flow)
5. [Domain Modeling](#domain-modeling)
6. [Keycloak Synchronization](#keycloak-synchronization)
7. [Security and Permissions](#security-and-permissions)
8. [Testing Plan](#testing-plan)

---

## Overview

### Purpose

This document provides a detailed implementation plan for user profile management, allowing users to view and update their profile information. The plan ensures proper
synchronization between the domain model and Keycloak.

### Business Requirements

- **Who:** USER (own profile), TENANT_ADMIN (users in own tenant), SYSTEM_ADMIN (all users)
- **What:** View and update user profile information (emailAddress, first name, last name)
- **When:** On-demand, when users need to update their information
- **Why:** Enable users to maintain accurate profile information

### Key Features

1. **Profile View** - Display current user profile information
2. **Profile Edit Form** - Edit form with validation
3. **Real-Time Validation** - Client-side and server-side
4. **Keycloak Synchronization** - Update Keycloak user information
5. **Success Handling** - Show success notification, refresh data
6. **Error Handling** - Clear, actionable error messages

---

## UI/UX Design

### Profile View

```
┌─────────────────────────────────────────────────┐
│  User Profile: john.doe                         │
│  [← Back]                          [Edit]       │
├─────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────┐ │
│  │ Basic Information                          │ │
│  ├───────────────────────────────────────────┤ │
│  │ Username:      john.doe                    │ │
│  │ Email:         john.doe@ldp001.com         │ │
│  │ First Name:    John                        │ │
│  │ Last Name:     Doe                         │ │
│  │ Status:        [ACTIVE]                    │ │
│  └───────────────────────────────────────────┘ │
│                                                 │
│  ┌───────────────────────────────────────────┐ │
│  │ Tenant Information                         │ │
│  ├───────────────────────────────────────────┤ │
│  │ Tenant:        Local Distribution Partner  │ │
│  │ Tenant ID:     ldp-001                     │ │
│  └───────────────────────────────────────────┘ │
│                                                 │
│  ┌───────────────────────────────────────────┐ │
│  │ Roles & Permissions                        │ │
│  ├───────────────────────────────────────────┤ │
│  │ • USER                                     │ │
│  │ • PICKER                                   │ │
│  └───────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

### Profile Edit Form

```
┌─────────────────────────────────────────────────┐
│  Edit Profile: john.doe                         │
│  [Cancel]                          [Save]       │
├─────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────┐ │
│  │ Email *          [john.doe@ldp001.com]    │ │
│  │ First Name       [John]                   │ │
│  │ Last Name        [Doe]                    │ │
│  └───────────────────────────────────────────┘ │
│                                                 │
│  Note: Username and tenant cannot be changed.   │
│                                                 │
│  [Cancel]                          [Save]       │
└─────────────────────────────────────────────────┘
```

### Editable Fields

| Field      | Editable               | Validation                |
|------------|------------------------|---------------------------|
| Username   | No                     | -                         |
| Tenant     | No                     | -                         |
| Email      | Yes                    | Valid emailAddress format |
| First Name | Yes                    | Max 50 characters         |
| Last Name  | Yes                    | Max 50 characters         |
| Status     | No (admin action only) | -                         |

---

## Frontend Implementation

### Components

```typescript
// components/UserProfileView.tsx
export const UserProfileView: React.FC<{ user: User }> = ({ user }) => {
  // Display user profile information
  // Edit button (if user has permission)
};

// components/UserProfileEditor.tsx
export const UserProfileEditor: React.FC<UserProfileEditorProps> = ({
  user,
  onSave,
  onCancel,
  isLoading
}) => {
  // Form to edit profile
  // Validation
  // Save/Cancel actions
};
```

### Custom Hook: useUpdateUserProfile

```typescript
// hooks/useUpdateUserProfile.ts
import { useState } from 'react';
import { userService } from '../services/userService';
import { UpdateUserProfileRequest } from '../types/user';
import { useSnackbar } from 'notistack';
import { useQueryClient } from '@tanstack/react-query';

export const useUpdateUserProfile = (userId: string) => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const { enqueueSnackbar } = useSnackbar();
  const queryClient = useQueryClient();

  const updateProfile = async (request: UpdateUserProfileRequest) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await userService.updateUserProfile(userId, request);
      if (response.error) {
        throw new Error(response.error.message);
      }

      // Invalidate and refetch user data
      await queryClient.invalidateQueries(['user', userId]);
      await queryClient.invalidateQueries(['users']);

      enqueueSnackbar('Profile updated successfully', { variant: 'success' });
      return true;
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to update profile');
      setError(error);
      enqueueSnackbar(error.message, { variant: 'error' });
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { updateProfile, isLoading, error };
};
```

---

## Backend Data Flow

### API Endpoint

**PUT** `/api/v1/users/{id}/profile`

**Request Body:**

```json
{
  "emailAddress": "john.doe@ldp001.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response:** `204 No Content`

### Request Flow

```
1. Frontend: PUT /api/v1/users/{id}/profile
   ↓
2. API Gateway: Route to user-service
   ↓
3. Security: TenantContextInterceptor extracts tenant from JWT
   ↓
4. Security: Validate user has permission to update this profile
   ↓
5. UserCommandController.updateUserProfile(id, request)
   ↓
6. UserMapper.toUpdateUserProfileCommand(id, request)
   ↓
7. UpdateUserProfileCommandHandler.handle(command)
   ↓
8. UserRepository.findById(userId)
   ↓
9. user.updateProfile(emailAddress, firstName, lastName) - Domain logic
   ↓
10. UserRepository.save(user) - Persist
    ↓
11. AuthenticationServicePort.updateKeycloakUser() - Sync with Keycloak
    ↓
12. UserEventPublisher.publish(UserUpdatedEvent)
    ↓
13. Kafka: UserUpdatedEvent published
    ↓
14. Response: 204 No Content
```

### Command Handler Implementation

```java
// UpdateUserProfileCommandHandler.java
@Component
@Transactional
public class UpdateUserProfileCommandHandler {
    private final UserRepository userRepository;
    private final UserEventPublisher eventPublisher;
    private final AuthenticationServicePort authenticationService;

    public void handle(UpdateUserProfileCommand command) {
        UserId userId = command.getUserId();

        // Load user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId.getValue()));

        // Update profile (domain logic)
        user.updateProfile(
            Email.of(command.getEmail()),
            FirstName.of(command.getFirstName()),
            LastName.of(command.getLastName())
        );

        // Persist
        userRepository.save(user);

        // Sync with Keycloak
        if (user.getKeycloakUserId().isPresent()) {
            try {
                authenticationService.updateKeycloakUser(
                    user.getKeycloakUserId().get(),
                    command.getEmail(),
                    command.getFirstName(),
                    command.getLastName()
                );
            } catch (KeycloakException e) {
                // Log error but don't fail the operation
                // User data is source of truth, Keycloak sync can be retried
                log.error("Failed to sync user profile with Keycloak: {}", e.getMessage(), e);
            }
        }

        // Publish events
        eventPublisher.publish(user.getDomainEvents());
        user.clearDomainEvents();
    }
}
```

---

## Domain Modeling

### Domain Entity: User.updateProfile()

```java
// User.java (domain core)
public void updateProfile(Email emailAddress, FirstName firstName, LastName lastName) {
    if (emailAddress == null) {
        throw new IllegalArgumentException("Email cannot be null");
    }

    this.emailAddress = emailAddress;
    this.firstName = firstName;
    this.lastName = lastName;
    this.lastModifiedAt = LocalDateTime.now();
    incrementVersion();

    // Publish domain event
    addDomainEvent(new UserUpdatedEvent(
        this.getId(),
        this.getTenantId(),
        this.status,
        Description.of("User profile updated")
    ));
}
```

### Domain Event: UserUpdatedEvent

```java
// UserUpdatedEvent.java (domain core)
public class UserUpdatedEvent extends UserEvent<UserId> {
    private final TenantId tenantId;
    private final UserStatus status;
    private final Description description;

    public UserUpdatedEvent(UserId userId, TenantId tenantId,
                            UserStatus status, Description description) {
        super(userId);
        this.tenantId = tenantId;
        this.status = status;
        this.description = description;
    }

    // Getters...
}
```

---

## Keycloak Synchronization

### Update Keycloak User

```java
// AuthenticationServiceAdapter.java (messaging layer)
@Override
public void updateKeycloakUser(KeycloakUserId keycloakUserId,
                                String emailAddress,
                                String firstName,
                                String lastName) {
    RealmResource realm = keycloak.realm(keycloakProperties.getRealm());
    UserResource userResource = realm.users().get(keycloakUserId.getValue());

    UserRepresentation user = userResource.toRepresentation();
    user.setEmail(emailAddress);
    user.setFirstName(firstName);
    user.setLastName(lastName);

    userResource.update(user);
}
```

### Synchronization Strategy

1. **Domain First** - Update domain model first
2. **Async Sync** - Sync with Keycloak asynchronously (best effort)
3. **Error Handling** - Log errors but don't fail the operation
4. **Retry Mechanism** - Implement retry for failed syncs (future)
5. **Source of Truth** - Domain model is source of truth

---

## Security and Permissions

### Permission Rules

1. **USER**
    - Can view own profile
    - Can edit own profile

2. **TENANT_ADMIN**
    - Can view profiles of users in own tenant
    - Can edit profiles of users in own tenant

3. **SYSTEM_ADMIN**
    - Can view all user profiles
    - Can edit all user profiles

### Permission Check

```java
// Security check in command handler
if (!securityService.canUpdateUserProfile(currentUserId, targetUserId)) {
    throw new AccessDeniedException("You do not have permission to update this profile");
}
```

---

## Testing Plan

### Frontend Tests

1. **Unit Tests**
    - UserProfileView component
    - UserProfileEditor component
    - useUpdateUserProfile hook
    - Form validation

2. **Integration Tests**
    - Profile edit flow
    - API integration
    - Success handling
    - Error handling

3. **E2E Tests**
    - View own profile
    - Edit own profile
    - Admin edits user profile
    - Permission enforcement

### Backend Tests

1. **Domain Tests**
    - User.updateProfile() method
    - Event publishing
    - Validation rules

2. **Integration Tests**
    - API endpoint
    - Command handler
    - Keycloak synchronization
    - Event publishing

3. **Security Tests**
    - Permission enforcement
    - Own profile vs other profiles
    - Tenant isolation

---

**Document Status:** Draft
**Last Updated:** 2025-12
**Next Review:** 2026-01
