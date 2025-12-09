# User Management Implementation Plan

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0
**Date:** 2025-12
**Status:** Draft
**Related Documents:**

- [Tenant Management Implementation Plan](../tenant-management/01-Tenant_Management_Implementation_Plan.md)
- [Service Architecture Document](../../../01-architecture/Service_Architecture_Document.md)
- [Frontend Architecture Document](../../../01-architecture/Frontend_Architecture_Document.md)
- [API Specifications](../../../02-api/API_Specifications.md)
- [IAM Integration Guide](../../../03-security/IAM_Integration_Guide.md)
- [Mandated Implementation Template Guide](../../../guide/mandated-Implementation-template-guide.md)

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture Alignment](#architecture-alignment)
3. [Implementation Scope](#implementation-scope)
4. [UI/UX Design Plan](#uiux-design-plan)
5. [Data Flow Architecture](#data-flow-architecture)
6. [Frontend Implementation](#frontend-implementation)
7. [Backend Integration](#backend-integration)
8. [Multi-Tenancy Enforcement](#multi-tenancy-enforcement)
9. [Testing Strategy](#testing-strategy)
10. [Implementation Phases](#implementation-phases)
11. [Detailed Feature Plans](#detailed-feature-plans)

---

## Overview

### Purpose

This document provides comprehensive implementation plans for user management functionality, covering the complete flow from production-grade UI to backend domain modeling. The
implementation follows **Domain-Driven Design**, **Clean Hexagonal Architecture**, **CQRS**, and **Event-Driven Choreography** principles, building on the foundation established in
tenant management.

### Scope

This plan covers:

- **User Creation** - Creating new users within tenants with Keycloak integration
- **User Profile Management** - Viewing and updating user profile information
- **User Role Management** - Assigning and managing user roles and permissions
- **User Lifecycle Management** - Activating, deactivating, and suspending users
- **Multi-Tenant User Management** - Managing users across multiple tenants (SYSTEM_ADMIN)
- **Keycloak Integration** - User creation, role assignment, and group membership

### Key Principles

1. **Production-Grade UI** - Modern, accessible, responsive interface
2. **Proper Data Routing** - Correct data flow from frontend to backend services
3. **Domain Modeling** - Correct domain modeling in domain core
4. **CQRS Compliance** - Separate command and query operations
5. **Event-Driven** - Domain events for state changes
6. **Multi-Tenant Enforcement** - Strict tenant isolation and context propagation
7. **Keycloak Integration** - Single Realm with tenant groups and user attributes

---

## Architecture Alignment

### System Architecture

The user management implementation aligns with the system's microservices architecture:

```
Frontend (React PWA)
    ↓ REST API
API Gateway (Spring Cloud Gateway)
    ↓ Route: /api/v1/users/**
User Service
    ├── Application Layer (REST Controllers)
    ├── Application Service Layer (Command/Query Handlers)
    ├── Domain Core (User Aggregate)
    ├── Data Access (Repository Adapters)
    └── Messaging (Event Publishers & Keycloak Integration)
        ↓ Keycloak Admin API
    Keycloak (wms-realm)
```

### Clean Hexagonal Architecture Layers

1. **Domain Core** (`user-domain-core`)
    - `User` aggregate root
    - Value objects: `Username`, `Email`, `UserStatus`, `FirstName`, `LastName`, `KeycloakUserId`
    - Domain events: `UserCreatedEvent`, `UserUpdatedEvent`, `UserDeactivatedEvent`

2. **Application Service** (`user-application-service`)
    - Command handlers: `CreateUserCommandHandler`, `UpdateUserProfileCommandHandler`, `AssignUserRoleCommandHandler`
    - Query handlers: `GetUserQueryHandler`, `ListUsersQueryHandler`, `GetUsersByTenantQueryHandler`
    - Port interfaces: `UserRepository`, `UserEventPublisher`, `AuthenticationServicePort`

3. **Application Layer** (`user-application`)
    - REST controllers: `UserCommandController`, `UserQueryController`, `BffAuthController`
    - DTOs: `CreateUserRequest`, `UserResponse`, `UpdateUserProfileRequest`, etc.
    - Mappers: `UserMapper`, `AuthMapper`

4. **Data Access** (`user-dataaccess`)
    - Repository adapters: `UserRepositoryAdapter`
    - JPA entities: `UserEntity`
    - Entity mappers: `UserEntityMapper`

5. **Messaging** (`user-messaging`)
    - Event publishers: `UserEventPublisherImpl`
    - Keycloak integration: `AuthenticationServiceAdapter`
    - Kafka integration

### CQRS Implementation

**Command Side (Write Operations):**

- `POST /api/v1/users` - Create user
- `PUT /api/v1/users/{id}/profile` - Update user profile
- `PUT /api/v1/users/{id}/activate` - Activate user
- `PUT /api/v1/users/{id}/deactivate` - Deactivate user
- `PUT /api/v1/users/{id}/suspend` - Suspend user
- `POST /api/v1/users/{id}/roles` - Assign role to user
- `DELETE /api/v1/users/{id}/roles/{roleId}` - Remove role from user

**Query Side (Read Operations):**

- `GET /api/v1/users/{id}` - Get user by ID
- `GET /api/v1/users` - List users (with pagination, filtering by tenant)
- `GET /api/v1/users/{id}/roles` - Get user roles
- `GET /api/v1/users/tenant/{tenantId}` - List users by tenant
- `GET /api/v1/users/search` - Search users

---

## Implementation Scope

### Features to Implement

#### 1. User Creation

- **UI:** Create user form with validation
- **Backend:** Create user command handler
- **Keycloak:** User creation in `wms-realm` with `tenant_id` attribute
- **Integration:** Assign user to tenant group: `tenant-{tenantId}`
- **Validation:** Client-side and server-side validation
- **Events:** `UserCreatedEvent` published

#### 2. User Profile Management

- **UI:** User profile view and edit form
- **Backend:** Update user profile command handler
- **Fields:** Email, first name, last name, phone
- **Events:** `UserUpdatedEvent` published

#### 3. User Role Management

- **UI:** Role assignment interface
- **Backend:** Assign/remove role command handlers
- **Keycloak:** Role assignment in `wms-realm`
- **Roles:** SYSTEM_ADMIN, TENANT_ADMIN, WAREHOUSE_MANAGER, PICKER, etc.
- **Events:** `UserRoleAssignedEvent`, `UserRoleRemovedEvent` published

#### 4. User Lifecycle Management

- **UI:** User list with status management
- **UI:** Activate, deactivate, suspend actions with confirmation
- **Backend:** User lifecycle command handlers
- **Keycloak:** Enable/disable user in `wms-realm`
- **Events:** Status change events published

#### 5. Multi-Tenant User Management

- **UI:** Tenant selector for SYSTEM_ADMIN
- **UI:** User list filtered by tenant
- **Backend:** Tenant-aware queries
- **Security:** Tenant context propagation and enforcement

### User Roles and Permissions

- **SYSTEM_ADMIN:** Full access to all users across all tenants
- **TENANT_ADMIN:** Manage users within own tenant only
- **WAREHOUSE_MANAGER:** View users within own tenant
- **USER:** View and edit own profile only

---

## UI/UX Design Plan

### Design Principles

1. **Material Design 3** - Following Material-UI (MUI) design system
2. **Accessibility** - WCAG 2.1 Level AA compliance
3. **Responsive Design** - Mobile-first, works on all devices
4. **Progressive Enhancement** - Core functionality without JavaScript
5. **Error Handling** - Clear, actionable error messages
6. **Loading States** - Proper loading indicators
7. **Confirmation Dialogs** - For destructive actions

### Component Structure

```
frontend-app/src/
├── features/
│   └── user-management/
│       ├── components/
│       │   ├── UserList.tsx                # User list table
│       │   ├── UserForm.tsx                # Create/Edit user form
│       │   ├── UserDetail.tsx              # User detail view
│       │   ├── UserStatusBadge.tsx         # Status indicator
│       │   ├── UserActions.tsx             # Action buttons
│       │   ├── UserRoleManager.tsx         # Role assignment
│       │   ├── TenantSelector.tsx          # Tenant filter
│       │   └── UserProfileEditor.tsx       # Profile edit form
│       ├── hooks/
│       │   ├── useUsers.ts                 # User list hook
│       │   ├── useUser.ts                  # Single user hook
│       │   ├── useCreateUser.ts            # Create user hook
│       │   ├── useUserActions.ts           # Lifecycle actions hook
│       │   └── useUserRoles.ts             # Role management hook
│       ├── services/
│       │   └── userService.ts              # API client for user operations
│       ├── types/
│       │   └── user.ts                     # TypeScript types
│       └── pages/
│           ├── UserListPage.tsx            # User list page
│           ├── UserCreatePage.tsx          # Create user page
│           └── UserDetailPage.tsx          # User detail page
```

### UI Pages

#### 1. User List Page

- **Route:** `/admin/users` (SYSTEM_ADMIN, TENANT_ADMIN)
- **Access:** SYSTEM_ADMIN sees all tenants, TENANT_ADMIN sees own tenant only
- **Features:**
    - Data table with pagination
    - Search and filtering (by tenant for SYSTEM_ADMIN)
    - Status badges
    - Quick actions (activate, deactivate, suspend)
    - Create user button
    - Link to user detail

#### 2. User Create Page

- **Route:** `/admin/users/create`
- **Access:** SYSTEM_ADMIN, TENANT_ADMIN
- **Features:**
    - Multi-step form (if needed)
    - Tenant selector (SYSTEM_ADMIN only)
    - Form validation
    - Real-time validation feedback
    - Submit with loading state
    - Success/error notifications

#### 3. User Detail Page

- **Route:** `/admin/users/{id}` or `/profile` (own profile)
- **Access:** SYSTEM_ADMIN, TENANT_ADMIN, USER (own profile)
- **Features:**
    - User information display
    - Role management (admin only)
    - Status management actions (admin only)
    - Profile edit
    - Activity timeline (future)

---

## Data Flow Architecture

### Complete Data Flow: Create User

```
1. User fills form in UserCreatePage
   ↓
2. Form validation (client-side)
   ↓
3. Submit → useCreateUser hook
   ↓
4. userService.createUser(request)
   ↓
5. API Client (axios) → POST /api/v1/users
   ↓
6. API Gateway routes to User Service
   ↓
7. UserCommandController.createUser()
   ↓
8. UserMapper.toCreateUserCommand(request)
   ↓
9. CreateUserCommandHandler.handle(command)
   ↓
10. Validate tenant exists and is ACTIVE (call Tenant Service)
    ↓
11. User.builder().build() - Domain entity creation
    ↓
12. UserRepository.save(user) - Persist aggregate
    ↓
13. AuthenticationServicePort.createKeycloakUser() - Create in Keycloak
    ↓
14. Set tenant_id user attribute in Keycloak
    ↓
15. Assign user to tenant group: tenant-{tenantId}
    ↓
16. UserEventPublisher.publish(user.getDomainEvents())
    ↓
17. Kafka: UserCreatedEvent published
    ↓
18. Notification Service: Send welcome emailAddress (optional)
    ↓
19. Response: CreateUserResponse
    ↓
20. Frontend: Success notification, redirect to detail page
```

### Complete Data Flow: Update User Profile

```
1. User edits profile in UserDetailPage
   ↓
2. Form validation (client-side)
   ↓
3. Submit → useUpdateUserProfile hook
   ↓
4. userService.updateUserProfile(id, request)
   ↓
5. API Client → PUT /api/v1/users/{id}/profile
   ↓
6. API Gateway routes to User Service
   ↓
7. UserCommandController.updateUserProfile(id, request)
   ↓
8. UpdateUserProfileCommandHandler.handle(command)
   ↓
9. UserRepository.findById(userId)
   ↓
10. user.updateProfile(emailAddress, firstName, lastName) - Domain logic
    ↓
11. UserRepository.save(user) - Persist
    ↓
12. AuthenticationServicePort.updateKeycloakUser() - Update in Keycloak
    ↓
13. UserEventPublisher.publish(UserUpdatedEvent)
    ↓
14. Kafka: UserUpdatedEvent published
    ↓
15. Response: 204 No Content
    ↓
16. Frontend: Success notification, refresh user data
```

### Complete Data Flow: List Users (Multi-Tenant)

```
1. User navigates to UserListPage
   ↓
2. useUsers hook loads data
   ↓
3. userService.listUsers(params)
   ↓
4. API Client → GET /api/v1/users?page=1&size=20&tenantId=ldp-001
   ↓
5. API Gateway routes to User Service
   ↓
6. Security: TenantContextInterceptor extracts tenant from JWT
   ↓
7. UserQueryController.listUsers(params)
   ↓
8. Security: SYSTEM_ADMIN can query any tenant, others filtered by own tenant
   ↓
9. ListUsersQueryHandler.handle(query)
   ↓
10. UserDataPort.findAll(query) - Read model query
    ↓
11. UserViewRepositoryAdapter.findAll() - JPA query with tenant filter
    ↓
12. Response: Paginated UserResponse[]
    ↓
13. Frontend: Display in data table
```

---

## Frontend Implementation

### TypeScript Types

```typescript
// types/user.ts
export interface User {
  userId: string;
  tenantId: string;
  tenantName?: string;
  username: string;
  emailAddress: string;
  firstName?: string;
  lastName?: string;
  status: UserStatus;
  keycloakUserId?: string;
  roles: string[];
  createdAt: string;
  lastModifiedAt?: string;
}

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';

export interface CreateUserRequest {
  tenantId: string;
  username: string;
  emailAddress: string;
  firstName?: string;
  lastName?: string;
  password: string;
  roles?: string[];
}

export interface UpdateUserProfileRequest {
  emailAddress: string;
  firstName?: string;
  lastName?: string;
}

export interface UserListResponse {
  data: User[];
  meta: {
    pagination: {
      page: number;
      size: number;
      totalElements: number;
      totalPages: number;
      hasNext: boolean;
      hasPrevious: boolean;
    };
  };
}

export interface UserListParams {
  page?: number;
  size?: number;
  tenantId?: string;
  status?: UserStatus;
  search?: string;
  sort?: string;
}
```

### API Service

```typescript
// services/userService.ts
import apiClient from './apiClient';
import { ApiResponse } from '../types/api';
import { User, CreateUserRequest, UpdateUserProfileRequest, UserListResponse, UserListParams } from '../types/user';

export const userService = {
  // Commands
  async createUser(request: CreateUserRequest): Promise<ApiResponse<{ userId: string; success: boolean; message: string }>> {
    const response = await apiClient.post('/users', request);
    return response.data;
  },

  async updateUserProfile(userId: string, request: UpdateUserProfileRequest): Promise<ApiResponse<void>> {
    const response = await apiClient.put(`/users/${userId}/profile`, request);
    return response.data;
  },

  async activateUser(userId: string): Promise<ApiResponse<void>> {
    const response = await apiClient.put(`/users/${userId}/activate`);
    return response.data;
  },

  async deactivateUser(userId: string): Promise<ApiResponse<void>> {
    const response = await apiClient.put(`/users/${userId}/deactivate`);
    return response.data;
  },

  async suspendUser(userId: string): Promise<ApiResponse<void>> {
    const response = await apiClient.put(`/users/${userId}/suspend`);
    return response.data;
  },

  async assignRole(userId: string, roleId: string): Promise<ApiResponse<void>> {
    const response = await apiClient.post(`/users/${userId}/roles`, { roleId });
    return response.data;
  },

  async removeRole(userId: string, roleId: string): Promise<ApiResponse<void>> {
    const response = await apiClient.delete(`/users/${userId}/roles/${roleId}`);
    return response.data;
  },

  // Queries
  async getUser(userId: string): Promise<ApiResponse<User>> {
    const response = await apiClient.get(`/users/${userId}`);
    return response.data;
  },

  async listUsers(params: UserListParams): Promise<ApiResponse<UserListResponse>> {
    const response = await apiClient.get('/users', { params });
    return response.data;
  },

  async getUserRoles(userId: string): Promise<ApiResponse<string[]>> {
    const response = await apiClient.get(`/users/${userId}/roles`);
    return response.data;
  },
};
```

### Custom Hooks

```typescript
// hooks/useCreateUser.ts
import { useState } from 'react';
import { userService } from '../services/userService';
import { CreateUserRequest } from '../types/user';
import { useSnackbar } from 'notistack';
import { useNavigate } from 'react-router-dom';

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
      enqueueSnackbar('User created successfully', { variant: 'success' });
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

---

## Backend Integration

### Existing Backend Implementation

The user service backend is already partially implemented with:

- ✅ Domain core with `User` aggregate
- ✅ BFF authentication endpoints (login, refresh, me, logout)
- ✅ Keycloak integration for authentication
- ⚠️ **MISSING:** User CRUD command handlers
- ⚠️ **MISSING:** User query handlers for listing/searching
- ⚠️ **MISSING:** User lifecycle management (activate, deactivate, suspend)
- ⚠️ **MISSING:** Role management endpoints and handlers

### Integration Points

1. **API Gateway Routing**
    - Route `/api/v1/users/**` to user-service
    - Authentication and authorization at gateway level
    - Tenant context propagation via `X-Tenant-ID` header

2. **Keycloak Integration (Single Realm Strategy)**
    - **Realm:** All users use single `wms-realm`
    - **Tenant Attribute:** Users have `tenant_id` attribute for multi-tenancy enforcement
    - **Tenant Groups:** Users assigned to tenant group: `tenant-{tenantId}`
    - **User Creation:** Create user in `wms-realm` with tenant attribute
    - **Role Assignment:** Assign realm roles to user
    - **User Management:** Enable/disable users based on status

3. **Tenant Service Integration**
    - **Validation:** Before user creation, validate tenant exists and is ACTIVE
    - **API Call:** `GET /api/v1/tenants/{id}/status`
    - **Error Handling:** Reject user creation for non-ACTIVE tenants

4. **Event Publishing**
    - Domain events published to Kafka
    - Other services can consume user events
    - Notification service sends welcome emails

### Keycloak Integration Details

**Single Realm Strategy:**

- All users share the `wms-realm` Keycloak realm
- Users differentiated by `tenant_id` user attribute (primary multi-tenancy mechanism)
- Users assigned to tenant groups for organization: `tenant-{tenantId}`

**User Lifecycle:**

- **On User Creation:**
    - Create user in `wms-realm`
    - Set `tenant_id` user attribute
    - Assign user to tenant group: `tenant-{tenantId}`
    - Set initial password (temporary, requires change on first login)
    - Enable user account

- **On User Activation:**
    - Enable user account in Keycloak

- **On User Deactivation:**
    - Disable user account in Keycloak
    - User cannot authenticate

- **On User Suspension:**
    - Disable user account in Keycloak
    - User cannot authenticate
    - Can be re-enabled when user is reactivated

**Port Interface:**

```java
// AuthenticationServicePort (from user-application-service layer)
public interface AuthenticationServicePort {
    // Existing authentication methods
    KeycloakUserId createUser(String tenantId, String username, String emailAddress,
                               String password, String firstName, String lastName);
    void updateUser(KeycloakUserId keycloakUserId, String emailAddress,
                    String firstName, String lastName);
    void enableUser(KeycloakUserId keycloakUserId);
    void disableUser(KeycloakUserId keycloakUserId);
    void assignRole(KeycloakUserId keycloakUserId, String roleName);
    void removeRole(KeycloakUserId keycloakUserId, String roleName);
    List<String> getUserRoles(KeycloakUserId keycloakUserId);
}
```

---

## Multi-Tenancy Enforcement

### Tenant Context Propagation

1. **JWT Token**
    - Token contains `tenant_id` claim
    - Extracted at API Gateway level
    - Set as `X-Tenant-ID` header

2. **TenantContextInterceptor**
    - Intercepts all requests
    - Extracts tenant ID from header or JWT
    - Sets `TenantContext.setCurrentTenant(tenantId)`
    - Clears context after request

3. **Repository Layer**
    - All queries automatically filtered by tenant ID
    - Uses `TenantContext.getCurrentTenant()`
    - Prevents cross-tenant data access

### Security Rules

1. **SYSTEM_ADMIN**
    - Can query any tenant (bypass tenant filter)
    - Can create users in any tenant
    - Can manage users across all tenants

2. **TENANT_ADMIN**
    - Can only query own tenant
    - Can create users in own tenant only
    - Can manage users in own tenant only

3. **USER**
    - Can only view and edit own profile
    - Cannot create or manage other users

### Tenant Validation

Before user creation, validate:

1. Tenant exists
2. Tenant status is ACTIVE
3. User has permission to create users in that tenant
4. Username is unique within tenant (Keycloak enforces global uniqueness)

---

## Testing Strategy

### Frontend Testing

1. **Unit Tests**
    - Component rendering
    - Form validation
    - Hook behavior
    - Service methods

2. **Integration Tests**
    - API integration
    - User flows
    - Error handling
    - Multi-tenancy enforcement

3. **E2E Tests**
    - Complete user creation flow
    - User profile update flow
    - User lifecycle management operations
    - Role assignment flow

### Backend Testing

1. **Domain Tests**
    - Business rule validation
    - Status transitions
    - Event publishing

2. **Integration Tests**
    - API endpoints
    - Repository operations
    - Keycloak integration
    - Event publishing

3. **Security Tests**
    - Tenant isolation
    - Permission enforcement
    - Cross-tenant access prevention

4. **Contract Tests**
    - API contract validation
    - DTO validation

---

## Implementation Phases

### Phase 1: Foundation (Week 1)

- [ ] Set up frontend feature structure
- [ ] Create TypeScript types
- [ ] Implement API service client
- [ ] Create custom hooks
- [ ] Implement TenantSelector component

### Phase 2: User List (Week 1-2)

- [ ] Implement UserListPage
- [ ] Implement UserList component
- [ ] Add pagination and filtering
- [ ] Add tenant filtering (SYSTEM_ADMIN)
- [ ] Add search functionality

### Phase 3: User Creation (Week 2)

- [ ] Implement UserCreatePage
- [ ] Implement UserForm component
- [ ] Add form validation
- [ ] Integrate with backend
- [ ] Implement CreateUserCommandHandler
- [ ] Integrate with Keycloak
- [ ] Validate tenant status

### Phase 4: User Detail & Profile Management (Week 2-3)

- [ ] Implement UserDetailPage
- [ ] Implement user profile view
- [ ] Implement profile edit form
- [ ] Implement UpdateUserProfileCommandHandler
- [ ] Sync profile updates with Keycloak

### Phase 5: User Lifecycle Management (Week 3)

- [ ] Implement user status management UI
- [ ] Add confirmation dialogs
- [ ] Implement ActivateUserCommandHandler
- [ ] Implement DeactivateUserCommandHandler
- [ ] Implement SuspendUserCommandHandler
- [ ] Sync status changes with Keycloak

### Phase 6: Role Management (Week 3-4)

- [ ] Implement UserRoleManager component
- [ ] Implement role assignment UI
- [ ] Implement AssignUserRoleCommandHandler
- [ ] Implement RemoveUserRoleCommandHandler
- [ ] Sync role changes with Keycloak

### Phase 7: Testing & Polish (Week 4)

- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Security audit and tenant isolation testing
- [ ] Accessibility audit
- [ ] Performance optimization
- [ ] Documentation

---

## Detailed Feature Plans

For detailed implementation plans for each feature, see:

1. **[User Creation Implementation Plan](02-User_Creation_Plan.md)**
    - Complete UI/UX design
    - Form structure and validation
    - Data flow from frontend to backend
    - Domain modeling details
    - Keycloak integration
    - Tenant validation

2. **[User Profile Management Plan](03-User_Profile_Management_Plan.md)**
    - Profile view and edit UI
    - Update workflow
    - Data synchronization with Keycloak
    - Event publishing

3. **[User Lifecycle Management Plan](04-User_Lifecycle_Management_Plan.md)**
    - Status management workflow
    - Keycloak integration
    - Event publishing
    - Error handling

4. **[User Role Management Plan](05-User_Role_Management_Plan.md)**
    - Role assignment UI
    - Role management workflow
    - Keycloak role synchronization
    - Permission enforcement

---

## References

- [Tenant Management Implementation Plan](../tenant-management/01-Tenant_Management_Implementation_Plan.md)
- [Service Architecture Document](../../../01-architecture/Service_Architecture_Document.md)
- [Frontend Architecture Document](../../../01-architecture/Frontend_Architecture_Document.md)
- [API Specifications](../../../02-api/API_Specifications.md)
- [IAM Integration Guide](../../../03-security/IAM_Integration_Guide.md)
- [Mandated Implementation Template Guide](../../../guide/mandated-Implementation-template-guide.md)
- [Security Architecture Document](../../../01-architecture/Security_Architecture_Document.md)
- [Multi-Tenancy Enforcement Guide](../../../03-security/Multi_Tenancy_Enforcement_Guide.md)

---

**Document Status:** Draft
**Last Updated:** 2025-12
**Next Review:** 2026-01
