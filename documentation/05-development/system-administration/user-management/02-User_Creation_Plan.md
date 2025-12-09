# User Creation Implementation Plan

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0
**Date:** 2025-12
**Status:** Draft
**Related Documents:**

- [User Management Implementation Plan](01-User_Management_Implementation_Plan.md)
- [Tenant Management Implementation Plan](../tenant-management/01-Tenant_Management_Implementation_Plan.md)
- [Service Architecture Document](../../../01-architecture/Service_Architecture_Document.md)
- [IAM Integration Guide](../../../03-security/IAM_Integration_Guide.md)

---

## Table of Contents

1. [Overview](#overview)
2. [UI/UX Design](#uiux-design)
3. [Frontend Implementation](#frontend-implementation)
4. [Backend Data Flow](#backend-data-flow)
5. [Domain Modeling](#domain-modeling)
6. [Keycloak Integration](#keycloak-integration)
7. [Tenant Validation](#tenant-validation)
8. [Validation Strategy](#validation-strategy)
9. [Error Handling](#error-handling)
10. [Testing Plan](#testing-plan)

---

## Overview

### Purpose

This document provides a detailed implementation plan for creating users in the system. The plan covers the complete flow from production-grade UI to backend domain modeling,
ensuring proper data routing, tenant validation, and Keycloak integration.

### Business Requirements

- **Who:** SYSTEM_ADMIN (any tenant), TENANT_ADMIN (own tenant only)
- **What:** Create new user with profile information and initial roles
- **When:** On-demand, when onboarding new users to a tenant
- **Why:** Enable new users to access the warehouse management system

### Key Features

1. **Multi-Step Form** (optional) or single comprehensive form
2. **Tenant Selector** - SYSTEM_ADMIN can select tenant, TENANT_ADMIN uses own tenant
3. **Real-Time Validation** - Client-side and server-side
4. **Keycloak Integration** - User creation in `wms-realm` with tenant attribute
5. **Tenant Validation** - Ensure tenant exists and is ACTIVE
6. **Role Assignment** - Assign initial roles during creation
7. **Success Handling** - Redirect to user detail page
8. **Error Handling** - Clear, actionable error messages

---

## UI/UX Design

### Page Layout

**Route:** `/admin/users/create`
**Access:** SYSTEM_ADMIN, TENANT_ADMIN
**Layout:** Full-width form with card container

### Form Structure

#### Single Comprehensive Form (Recommended)

```
┌─────────────────────────────────────────────────┐
│  Create User                                    │
├─────────────────────────────────────────────────┤
│                                                 │
│  Tenant Selection (SYSTEM_ADMIN only)           │
│  ┌─────────────────────────────────────────┐   │
│  │ Tenant *         [Select Tenant ▼]      │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  Basic Information                              │
│  ┌─────────────────────────────────────────┐   │
│  │ Username *       [____________]         │   │
│  │ Email *          [____________]         │   │
│  │ First Name       [____________]         │   │
│  │ Last Name        [____________]         │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  Authentication                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ Password *       [____________]         │   │
│  │ Confirm Password [____________]         │   │
│  │ ☐ Require password change on first login│   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  Role Assignment                                │
│  ┌─────────────────────────────────────────┐   │
│  │ ☐ TENANT_ADMIN                          │   │
│  │ ☐ WAREHOUSE_MANAGER                     │   │
│  │ ☐ PICKER                                │   │
│  │ ☐ USER (Default)                        │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  Keycloak Integration                           │
│  ┌─────────────────────────────────────────┐   │
│  │ ℹ️  User will be created in wms-realm   │   │
│  │    with tenant_id attribute              │   │
│  │    and assigned to tenant group          │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  [Cancel]                    [Create User]     │
│                                                 │
└─────────────────────────────────────────────────┘
```

### Form Fields

| Field            | Type     | Required | Validation                        | Max Length |
|------------------|----------|----------|-----------------------------------|------------|
| Tenant           | Select   | Yes      | Active tenant                     | -          |
| Username         | Text     | Yes      | Alphanumeric, unique              | 50         |
| Email            | Email    | Yes      | Valid emailAddress format, unique | 255        |
| First Name       | Text     | No       | -                                 | 50         |
| Last Name        | Text     | No       | -                                 | 50         |
| Password         | Password | Yes      | Min 8 chars, complexity           | 128        |
| Confirm Password | Password | Yes      | Matches password                  | 128        |
| Roles            | Checkbox | No       | At least one role                 | -          |

**Note:** Tenant selector only visible for SYSTEM_ADMIN. TENANT_ADMIN automatically uses own tenant.

### Validation Rules

#### Client-Side Validation

```typescript
const validationSchema = {
  tenantId: {
    required: 'Tenant is required'
  },
  username: {
    required: 'Username is required',
    maxLength: { value: 50, message: 'Username cannot exceed 50 characters' },
    pattern: {
      value: /^[a-zA-Z0-9._-]+$/,
      message: 'Username must be alphanumeric with periods, hyphens, or underscores only'
    }
  },
  emailAddress: {
    required: 'Email is required',
    pattern: {
      value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
      message: 'Invalid emailAddress format'
    },
    maxLength: { value: 255, message: 'Email cannot exceed 255 characters' }
  },
  firstName: {
    maxLength: { value: 50, message: 'First name cannot exceed 50 characters' }
  },
  lastName: {
    maxLength: { value: 50, message: 'Last name cannot exceed 50 characters' }
  },
  password: {
    required: 'Password is required',
    minLength: { value: 8, message: 'Password must be at least 8 characters' },
    pattern: {
      value: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/,
      message: 'Password must contain uppercase, lowercase, number, and special character'
    }
  },
  confirmPassword: {
    required: 'Confirm password is required',
    validate: (value, context) => value === context.password || 'Passwords do not match'
  }
};
```

### UI Components

#### 1. UserForm Component

```typescript
interface UserFormProps {
  onSubmit: (data: CreateUserRequest) => Promise<void>;
  onCancel: () => void;
  isLoading?: boolean;
  currentUserRole: string;
  currentUserTenantId?: string;
}

const UserForm: React.FC<UserFormProps> = ({
  onSubmit,
  onCancel,
  isLoading,
  currentUserRole,
  currentUserTenantId
}) => {
  // Form implementation using React Hook Form
  // Material-UI components
  // Real-time validation
  // Conditional tenant selector based on role
};
```

#### 2. Form Sections

- `TenantSelectorSection` - Tenant selection (SYSTEM_ADMIN only)
- `BasicInformationSection` - Username, emailAddress, names
- `AuthenticationSection` - Password fields
- `RoleAssignmentSection` - Role checkboxes
- `FormActions` - Cancel and Submit buttons

### Loading States

- **Form Loading:** Disable all inputs during submission
- **Submit Button:** Show loading spinner, disable button
- **Tenant Validation:** Show loading while validating tenant
- **Success:** Show success notification, redirect after 2 seconds
- **Error:** Show error notification, keep form editable

### Success Flow

1. Show success notification: "User created successfully"
2. Show additional info: "Welcome emailAddress sent" (if notification service enabled)
3. Redirect to user detail page: `/admin/users/{userId}`

### Error Handling

1. **Validation Errors:** Show inline errors below fields
2. **Tenant Validation Errors:** Show error at top of form
3. **Server Errors:** Show error notification at top of form
4. **Network Errors:** Show retry option
5. **Duplicate Username/Email:** Highlight relevant field with specific error
6. **Keycloak Errors:** Show error with troubleshooting guidance

---

## Frontend Implementation

### Component Structure

```
features/user-management/
├── components/
│   ├── UserForm.tsx
│   ├── TenantSelectorSection.tsx
│   ├── BasicInformationSection.tsx
│   ├── AuthenticationSection.tsx
│   └── RoleAssignmentSection.tsx
├── pages/
│   └── UserCreatePage.tsx
├── hooks/
│   └── useCreateUser.ts
└── services/
    └── userService.ts
```

### TypeScript Types

```typescript
// types/user.ts
export interface CreateUserRequest {
  tenantId: string;
  username: string;
  emailAddress: string;
  firstName?: string;
  lastName?: string;
  password: string;
  roles?: string[];
}

export interface CreateUserResponse {
  userId: string;
  success: boolean;
  message: string;
}
```

### Custom Hook: useCreateUser

```typescript
// hooks/useCreateUser.ts
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userService } from '../services/userService';
import { CreateUserRequest } from '../types/user';
import { useSnackbar } from 'notistack';

export const useCreateUser = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const createUser = async (request: CreateUserRequest) => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await userService.createUser(request);

      if (response.error) {
        throw new Error(response.error.message);
      }

      // Success
      enqueueSnackbar('User created successfully', { variant: 'success' });

      // Redirect to user detail page
      setTimeout(() => {
        navigate(`/admin/users/${response.data.userId}`);
      }, 1500);

      return response.data;
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to create user');
      setError(error);
      enqueueSnackbar(error.message, { variant: 'error' });
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { createUser, isLoading, error };
};
```

### UserCreatePage Component

```typescript
// pages/UserCreatePage.tsx
import React from 'react';
import { Container, Paper, Typography, Box } from '@mui/material';
import { UserForm } from '../components/UserForm';
import { useCreateUser } from '../hooks/useCreateUser';
import { CreateUserRequest } from '../types/user';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';

export const UserCreatePage: React.FC = () => {
  const { createUser, isLoading } = useCreateUser();
  const navigate = useNavigate();
  const { user } = useAuth();

  const handleSubmit = async (data: CreateUserRequest) => {
    await createUser(data);
  };

  const handleCancel = () => {
    navigate('/admin/users');
  };

  return (
    <Container maxWidth="md" sx={{ mt: 4, mb: 4 }}>
      <Paper elevation={3} sx={{ p: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Create User
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 4 }}>
          Create a new user account within a tenant.
        </Typography>

        <UserForm
          onSubmit={handleSubmit}
          onCancel={handleCancel}
          isLoading={isLoading}
          currentUserRole={user?.role || ''}
          currentUserTenantId={user?.tenantId}
        />
      </Paper>
    </Container>
  );
};
```

---

## Backend Data Flow

### API Endpoint

**POST** `/api/v1/users`

**Request Body:**

```json
{
  "tenantId": "ldp-001",
  "username": "john.doe",
  "emailAddress": "john.doe@ldp001.com",
  "firstName": "John",
  "lastName": "Doe",
  "password": "SecureP@ssw0rd",
  "roles": ["USER", "PICKER"]
}
```

**Response:** `201 Created`

```json
{
  "data": {
    "userId": "usr-123",
    "success": true,
    "message": "User created successfully"
  }
}
```

### Request Flow

```
1. Frontend: POST /api/v1/users
   ↓
2. API Gateway: Route to user-service
   ↓
3. Security: TenantContextInterceptor extracts tenant from JWT
   ↓
4. Security: Validate user has permission to create users in specified tenant
   ↓
5. UserCommandController.createUser(request)
   ↓
6. UserMapper.toCreateUserCommand(request)
   ↓
7. CreateUserCommandHandler.handle(command)
   ↓
8. Validate tenant exists and is ACTIVE (call Tenant Service)
   ↓
9. TenantServicePort.validateTenantActive(tenantId)
   ↓
10. Check username uniqueness (Keycloak enforces global uniqueness)
    ↓
11. User.builder()
       .userId(UserId.of(UUID.randomUUID()))
       .tenantId(TenantId.of(command.getTenantId()))
       .username(Username.of(command.getUsername()))
       .emailAddress(Email.of(command.getEmail()))
       .firstName(FirstName.of(command.getFirstName()))
       .lastName(LastName.of(command.getLastName()))
       .status(UserStatus.ACTIVE)
       .build()
    ↓
12. UserRepository.save(user) - Persist aggregate
    ↓
13. AuthenticationServicePort.createKeycloakUser()
       - Create user in wms-realm
       - Set tenant_id user attribute
       - Set initial password (temporary)
       - Assign user to tenant group: tenant-{tenantId}
       - Assign roles to user
       - Enable user account
    ↓
14. user.linkKeycloakUser(keycloakUserId) - Link Keycloak ID
    ↓
15. UserRepository.save(user) - Update with Keycloak ID
    ↓
16. UserEventPublisher.publish(user.getDomainEvents())
    ↓
17. Kafka: UserCreatedEvent published
    ↓
18. Notification Service: Send welcome emailAddress (optional)
    ↓
19. Response: CreateUserResponse
```

### Command Handler Implementation

```java
// CreateUserCommandHandler.java
@Component
@Transactional
public class CreateUserCommandHandler {
    private final UserRepository userRepository;
    private final UserEventPublisher eventPublisher;
    private final AuthenticationServicePort authenticationService;
    private final TenantServicePort tenantService;

    public CreateUserResult handle(CreateUserCommand command) {
        // Validate tenant exists and is ACTIVE
        TenantId tenantId = TenantId.of(command.getTenantId());
        if (!tenantService.isTenantActive(tenantId)) {
            throw new TenantNotActiveException(
                "Cannot create user: tenant is not active: " + tenantId.getValue()
            );
        }

        // Build domain entity
        UserId userId = UserId.of(UUID.randomUUID().toString());
        User user = User.builder()
            .userId(userId)
            .tenantId(tenantId)
            .username(Username.of(command.getUsername()))
            .emailAddress(Email.of(command.getEmail()))
            .firstName(command.getFirstName())
            .lastName(command.getLastName())
            .status(UserStatus.ACTIVE)
            .build();

        // Persist domain entity
        userRepository.save(user);

        // Create user in Keycloak
        try {
            KeycloakUserId keycloakUserId = authenticationService.createUser(
                command.getTenantId(),
                command.getUsername(),
                command.getEmail(),
                command.getPassword(),
                command.getFirstName(),
                command.getLastName()
            );

            // Link Keycloak user ID
            user.linkKeycloakUser(keycloakUserId);
            userRepository.save(user);

            // Assign roles
            if (command.getRoles() != null && !command.getRoles().isEmpty()) {
                for (String role : command.getRoles()) {
                    authenticationService.assignRole(keycloakUserId, role);
                }
            }
        } catch (KeycloakException e) {
            // Rollback: delete user from database
            userRepository.delete(user);
            throw new UserCreationException(
                "Failed to create user in Keycloak: " + e.getMessage(), e
            );
        }

        // Publish events
        eventPublisher.publish(user.getDomainEvents());
        user.clearDomainEvents();

        return new CreateUserResult(userId.getValue(), true, "User created successfully");
    }
}
```

---

## Domain Modeling

### Domain Entity: User

The `User` aggregate root is already implemented in the domain core. Key aspects:

1. **Builder Pattern** - Uses builder for construction
2. **Business Rules** - Validates required fields
3. **Status Management** - Initial status is ACTIVE
4. **Domain Events** - Publishes `UserCreatedEvent` on creation
5. **Keycloak Integration** - Links Keycloak user ID after creation

### Value Objects

1. **UserId** - From common-domain
2. **TenantId** - From common-domain
3. **Username** - Validates non-empty, max length, format
4. **Email** - Validates emailAddress format
5. **FirstName** - Optional, max length
6. **LastName** - Optional, max length
7. **KeycloakUserId** - Links to Keycloak user
8. **UserStatus** - Enum: ACTIVE, INACTIVE, SUSPENDED

### Domain Event: UserCreatedEvent

```java
// UserCreatedEvent.java (domain core)
public class UserCreatedEvent extends UserEvent<UserId> {
    private final TenantId tenantId;
    private final Username username;
    private final Email emailAddress;
    private final UserStatus status;

    public UserCreatedEvent(UserId userId, TenantId tenantId,
                            Username username, Email emailAddress, UserStatus status) {
        super(userId);
        this.tenantId = tenantId;
        this.username = username;
        this.emailAddress = emailAddress;
        this.status = status;
    }

    // Getters...
}
```

---

## Keycloak Integration

### User Creation in Keycloak

The `AuthenticationServicePort` interface defines the contract for Keycloak integration:

```java
// AuthenticationServicePort.java (application-service layer)
public interface AuthenticationServicePort {
    KeycloakUserId createUser(String tenantId, String username, String emailAddress,
                               String password, String firstName, String lastName);
    // Other methods...
}
```

### Implementation: AuthenticationServiceAdapter

```java
// AuthenticationServiceAdapter.java (messaging layer)
@Component
public class AuthenticationServiceAdapter implements AuthenticationServicePort {
    private final Keycloak keycloak;
    private final KeycloakProperties keycloakProperties;

    @Override
    public KeycloakUserId createUser(String tenantId, String username, String emailAddress,
                                       String password, String firstName, String lastName) {
        RealmResource realm = keycloak.realm(keycloakProperties.getRealm()); // wms-realm

        // Create user representation
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(emailAddress);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setEmailVerified(false);

        // Set tenant_id attribute (multi-tenancy enforcement)
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("tenant_id", Collections.singletonList(tenantId));
        user.setAttributes(attributes);

        // Set initial password (temporary, requires change on first login)
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(true);
        user.setCredentials(Collections.singletonList(credential));

        // Create user
        Response response = realm.users().create(user);
        if (response.getStatus() != 201) {
            throw new KeycloakException("Failed to create user: " + response.getStatusInfo());
        }

        // Extract user ID from location header
        String locationHeader = response.getHeaderString("Location");
        String keycloakUserId = locationHeader.substring(locationHeader.lastIndexOf('/') + 1);

        // Assign user to tenant group
        assignUserToTenantGroup(realm, keycloakUserId, tenantId);

        return KeycloakUserId.of(keycloakUserId);
    }

    private void assignUserToTenantGroup(RealmResource realm, String keycloakUserId, String tenantId) {
        // Find tenant group: tenant-{tenantId}
        String groupName = "tenant-" + tenantId;
        List<GroupRepresentation> groups = realm.groups().groups(groupName, 0, 1);

        if (groups.isEmpty()) {
            throw new KeycloakException("Tenant group not found: " + groupName);
        }

        GroupRepresentation tenantGroup = groups.get(0);

        // Assign user to group
        realm.users().get(keycloakUserId).joinGroup(tenantGroup.getId());
    }
}
```

### Keycloak User Attributes

Each user in Keycloak has the following attributes:

- `tenant_id` - Tenant identifier for multi-tenancy enforcement
- Standard attributes: username, emailAddress, firstName, lastName, enabled

### Keycloak User Groups

Each user is assigned to their tenant group:

- Group name: `tenant-{tenantId}`
- Example: `tenant-ldp-001` for tenant `ldp-001`
- Used for user organization and optional role assignment

---

## Tenant Validation

### Validation Flow

Before creating a user, validate the tenant:

1. **Tenant Exists** - Tenant must exist in Tenant Service
2. **Tenant Active** - Tenant status must be ACTIVE
3. **Permission Check** - User must have permission to create users in that tenant

### TenantServicePort Interface

```java
// TenantServicePort.java (user-application-service layer)
public interface TenantServicePort {
    boolean isTenantActive(TenantId tenantId);
    TenantStatus getTenantStatus(TenantId tenantId);
}
```

### Implementation: TenantServiceAdapter

```java
// TenantServiceAdapter.java (user-messaging layer)
@Component
public class TenantServiceAdapter implements TenantServicePort {
    private final RestTemplate restTemplate;
    private final String tenantServiceUrl;

    @Override
    public boolean isTenantActive(TenantId tenantId) {
        try {
            String url = tenantServiceUrl + "/api/v1/tenants/" + tenantId.getValue() + "/status";
            ResponseEntity<TenantStatusResponse> response = restTemplate.getForEntity(
                url,
                TenantStatusResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return "ACTIVE".equals(response.getBody().getStatus());
            }

            return false;
        } catch (HttpClientErrorException.NotFound e) {
            throw new TenantNotFoundException("Tenant not found: " + tenantId.getValue());
        } catch (Exception e) {
            throw new TenantServiceException("Failed to validate tenant: " + e.getMessage(), e);
        }
    }
}
```

---

## Validation Strategy

### Client-Side Validation

- **Real-time validation** using React Hook Form + Yup
- **Field-level errors** displayed immediately
- **Form-level validation** on submit
- **Prevents invalid submissions**

### Server-Side Validation

- **DTO validation** using Jakarta Bean Validation
- **Tenant validation** via Tenant Service
- **Business rule validation** in command handler
- **Domain validation** in aggregate builder

### Validation Layers

1. **Frontend (React Hook Form + Yup)**
    - Format validation
    - Required field validation
    - Length validation
    - Password complexity validation

2. **Backend DTO (Jakarta Bean Validation)**
    - Re-validate all constraints
    - Format validation
    - Length validation

3. **Application Service (Command Handler)**
    - Tenant validation (exists, active)
    - Permission validation
    - Business rule enforcement

4. **Domain Core (Business Rules)**
    - Invariant checking
    - Domain rule enforcement

---

## Error Handling

### Frontend Error Handling

1. **Validation Errors**
    - Display inline below fields
    - Highlight field in red
    - Show specific error message

2. **Tenant Validation Errors**
    - Display error at top of form
    - Prevent form submission
    - Suggest corrective actions

3. **Server Errors**
    - Display error notification
    - Show error message from API
    - Keep form editable for correction

4. **Keycloak Errors**
    - Display error with troubleshooting guidance
    - Suggest checking Keycloak configuration
    - Provide admin contact information

5. **Network Errors**
    - Show retry option
    - Display network error message
    - Allow form resubmission

### Backend Error Handling

1. **Validation Errors** - `400 Bad Request` with validation details
2. **Tenant Not Found** - `404 Not Found` with specific error
3. **Tenant Not Active** - `400 Bad Request` with specific error
4. **Duplicate Username/Email** - `409 Conflict` with specific error
5. **Keycloak Error** - `500 Internal Server Error`, rollback user creation
6. **Permission Denied** - `403 Forbidden` with specific error

### Error Response Format

```json
{
  "error": {
    "code": "TENANT_NOT_ACTIVE",
    "message": "Cannot create user: tenant 'ldp-001' is not active",
    "timestamp": "2025-12-04T10:30:00Z",
    "path": "/api/v1/users",
    "requestId": "req-123"
  }
}
```

---

## Testing Plan

### Frontend Tests

1. **Unit Tests**
    - UserForm component rendering
    - Form validation logic
    - useCreateUser hook behavior
    - Tenant selector visibility based on role

2. **Integration Tests**
    - Form submission flow
    - API integration
    - Error handling
    - Role-based tenant selection

3. **E2E Tests**
    - Complete user creation flow (SYSTEM_ADMIN)
    - Complete user creation flow (TENANT_ADMIN)
    - Success redirect
    - Error scenarios (tenant not active, duplicate username, etc.)

### Backend Tests

1. **Domain Tests**
    - User entity creation
    - Business rule validation
    - Event publishing

2. **Integration Tests**
    - API endpoint
    - Command handler
    - Tenant validation
    - Keycloak integration
    - Event publishing

3. **Security Tests**
    - Permission enforcement (SYSTEM_ADMIN vs TENANT_ADMIN)
    - Tenant isolation
    - Cross-tenant user creation prevention

---

## References

- [User Management Implementation Plan](01-User_Management_Implementation_Plan.md)
- [Tenant Management Implementation Plan](../tenant-management/01-Tenant_Management_Implementation_Plan.md)
- [Service Architecture Document](../../../01-architecture/Service_Architecture_Document.md)
- [Frontend Architecture Document](../../../01-architecture/Frontend_Architecture_Document.md)
- [API Specifications](../../../02-api/API_Specifications.md)
- [IAM Integration Guide](../../../03-security/IAM_Integration_Guide.md)

---

**Document Status:** Draft
**Last Updated:** 2025-12
**Next Review:** 2026-01
