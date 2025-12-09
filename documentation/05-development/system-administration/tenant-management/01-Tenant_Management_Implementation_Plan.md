# Tenant Management Implementation Plan

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-01  
**Status:** Draft  
**Related Documents:**

- [Service Architecture Document](../../01-architecture/Service_Architecture_Document.md)
- [Frontend Architecture Document](../../01-architecture/Frontend_Architecture_Document.md)
- [Tenant Service Implementation Plan](../../01-architecture/Tenant_Service_Implementation_Plan.md)
- [API Specifications](../../02-api/API_Specifications.md)
- [IAM Integration Guide](../../03-security/IAM_Integration_Guide.md)
- [Mandated Implementation Template Guide](../../guide/mandated-Implementation-template-guide.md)

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture Alignment](#architecture-alignment)
3. [Implementation Scope](#implementation-scope)
4. [UI/UX Design Plan](#uiux-design-plan)
5. [Data Flow Architecture](#data-flow-architecture)
6. [Frontend Implementation](#frontend-implementation)
7. [Backend Integration](#backend-integration)
8. [Testing Strategy](#testing-strategy)
9. [Implementation Phases](#implementation-phases)
10. [Detailed Feature Plans](#detailed-feature-plans)

---

## Overview

### Purpose

This document provides comprehensive implementation plans for tenant management functionality, covering the complete flow from production-grade UI to backend domain modeling. The
implementation follows **Domain-Driven Design**, **Clean Hexagonal Architecture**, **CQRS**, and **Event-Driven Choreography** principles.

### Scope

This plan covers:

- **Tenant Creation** - Creating new Local Distribution Partners (LDPs) with Keycloak group setup
- **Tenant Activation** - Activating tenants and creating/enabling Keycloak tenant groups
- **Tenant Management** - Viewing, listing, updating, and managing tenant lifecycle
- **Keycloak Integration** - Single Realm strategy with tenant groups for user organization

### Key Principles

1. **Production-Grade UI** - Modern, accessible, responsive interface
2. **Proper Data Routing** - Correct data flow from frontend to backend services
3. **Domain Modeling** - Correct domain modeling in domain core
4. **CQRS Compliance** - Separate command and query operations
5. **Event-Driven** - Domain events for state changes
6. **Multi-Tenant Awareness** - Proper tenant context propagation
7. **Keycloak Integration** - Single Realm with tenant groups for user organization

---

## Architecture Alignment

### System Architecture

The tenant management implementation aligns with the system's microservices architecture:

```
Frontend (React PWA)
    ↓ REST API
API Gateway (Spring Cloud Gateway)
    ↓ Route: /api/v1/tenants/**
Tenant Service
    ├── Application Layer (REST Controllers)
    ├── Application Service Layer (Command/Query Handlers)
    ├── Domain Core (Tenant Aggregate)
    ├── Data Access (Repository Adapters)
    └── Messaging (Event Publishers)
```

### Clean Hexagonal Architecture Layers

1. **Domain Core** (`tenant-domain-core`)
    - `Tenant` aggregate root
    - Value objects: `TenantName`, `TenantStatus`, `ContactInformation`, `TenantConfiguration`
    - Domain events: `TenantCreatedEvent`, `TenantActivatedEvent`, etc.

2. **Application Service** (`tenant-application-service`)
    - Command handlers: `CreateTenantCommandHandler`, `ActivateTenantCommandHandler`
    - Query handlers: `GetTenantQueryHandler`, `ListTenantsQueryHandler`
    - Port interfaces: `TenantRepository`, `TenantEventPublisher`

3. **Application Layer** (`tenant-application`)
    - REST controllers: `TenantCommandController`, `TenantQueryController`
    - DTOs: `CreateTenantRequest`, `TenantResponse`, etc.
    - Mappers: `TenantMapper`

4. **Data Access** (`tenant-dataaccess`)
    - Repository adapters: `TenantRepositoryAdapter`
    - JPA entities: `TenantEntity`
    - Entity mappers: `TenantEntityMapper`

5. **Messaging** (`tenant-messaging`)
    - Event publishers: `TenantEventPublisherImpl`
    - Kafka integration

6. **Keycloak Integration** (via `common-keycloak`)
    - Keycloak group port: `KeycloakGroupPort`
    - Group creation/enabling on tenant activation
    - Group management for tenant lifecycle

### CQRS Implementation

**Command Side (Write Operations):**

- `POST /api/v1/tenants` - Create tenant
- `PUT /api/v1/tenants/{id}/activate` - Activate tenant
- `PUT /api/v1/tenants/{id}/deactivate` - Deactivate tenant
- `PUT /api/v1/tenants/{id}/suspend` - Suspend tenant
- `PUT /api/v1/tenants/{id}/configuration` - Update configuration

**Query Side (Read Operations):**

- `GET /api/v1/tenants/{id}` - Get tenant by ID
- `GET /api/v1/tenants` - List tenants (with pagination, filtering)
- `GET /api/v1/tenants/{id}/status` - Get tenant status
- `GET /api/v1/tenants/{id}/configuration` - Get tenant configuration

---

## Implementation Scope

### Features to Implement

#### 1. Tenant Creation

- **UI:** Create tenant form with validation
- **Backend:** Create tenant command handler
- **Validation:** Client-side and server-side validation
- **Events:** `TenantCreatedEvent` published

#### 2. Tenant Activation

- **UI:** Activate tenant action with confirmation
- **Backend:** Activate tenant command handler
- **Integration:** Keycloak tenant group creation/enabling in `wms-realm`
- **Events:** `TenantActivatedEvent` published

#### 3. Tenant Management

- **UI:** Tenant list with pagination and filtering
- **UI:** Tenant detail view
- **UI:** Tenant status management (activate, deactivate, suspend)
- **UI:** Tenant configuration update
- **Backend:** Query handlers for listing and viewing
- **Backend:** Command handlers for lifecycle operations

### User Roles and Permissions

- **SYSTEM_ADMIN:** Full access to all tenant management operations
- **USER:** Can view own tenant information only

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
│   └── tenant-management/
│       ├── components/
│       │   ├── TenantList.tsx          # Tenant list table
│       │   ├── TenantForm.tsx          # Create/Edit tenant form
│       │   ├── TenantDetail.tsx        # Tenant detail view
│       │   ├── TenantStatusBadge.tsx   # Status indicator
│       │   └── TenantActions.tsx      # Action buttons (activate, etc.)
│       ├── hooks/
│       │   ├── useTenants.ts           # Tenant list hook
│       │   ├── useTenant.ts            # Single tenant hook
│       │   ├── useCreateTenant.ts      # Create tenant hook
│       │   └── useTenantActions.ts     # Lifecycle actions hook
│       ├── services/
│       │   └── tenantService.ts        # API client for tenant operations
│       ├── types/
│       │   └── tenant.ts               # TypeScript types
│       └── pages/
│           ├── TenantListPage.tsx      # Tenant list page
│           ├── TenantCreatePage.tsx    # Create tenant page
│           └── TenantDetailPage.tsx    # Tenant detail page
```

### UI Pages

#### 1. Tenant List Page

- **Route:** `/admin/tenants`
- **Access:** SYSTEM_ADMIN only
- **Features:**
    - Data table with pagination
    - Search and filtering
    - Status badges
    - Quick actions (activate, deactivate, suspend)
    - Create tenant button
    - Link to tenant detail

#### 2. Tenant Create Page

- **Route:** `/admin/tenants/create`
- **Access:** SYSTEM_ADMIN only
- **Features:**
    - Multi-step form (if needed)
    - Form validation
    - Real-time validation feedback
    - Submit with loading state
    - Success/error notifications

#### 3. Tenant Detail Page

- **Route:** `/admin/tenants/{id}`
- **Access:** SYSTEM_ADMIN or USER (own tenant)
- **Features:**
    - Tenant information display
    - Status management actions
    - Configuration view/edit
    - Activity timeline (future)
    - Related information (users, etc.)

---

## Data Flow Architecture

### Complete Data Flow: Create Tenant

```
1. User fills form in TenantCreatePage
   ↓
2. Form validation (client-side)
   ↓
3. Submit → useCreateTenant hook
   ↓
4. tenantService.createTenant(request)
   ↓
5. API Client (axios) → POST /api/v1/tenants
   ↓
6. API Gateway routes to Tenant Service
   ↓
7. TenantCommandController.createTenant()
   ↓
8. TenantMapper.toCreateTenantCommand(request)
   ↓
9. CreateTenantCommandHandler.handle(command)
   ↓
10. TenantRepository.findById() - Check uniqueness
    ↓
11. Tenant.builder().build() - Domain entity creation
    ↓
12. TenantRepository.save(tenant) - Persist aggregate
    ↓
13. KeycloakGroupPort.createTenantGroup() - Create group in wms-realm (optional)
    ↓
14. TenantEventPublisher.publish(tenant.getDomainEvents())
    ↓
15. Kafka: TenantCreatedEvent published
    ↓
16. Response: CreateTenantResponse
    ↓
17. Frontend: Success notification, redirect to detail page
```

### Complete Data Flow: Activate Tenant

```
1. User clicks "Activate" in TenantDetailPage
   ↓
2. Confirmation dialog
   ↓
3. Confirm → useTenantActions.activateTenant(id)
   ↓
4. tenantService.activateTenant(id)
   ↓
5. API Client → PUT /api/v1/tenants/{id}/activate
   ↓
6. API Gateway routes to Tenant Service
   ↓
7. TenantCommandController.activateTenant(id)
   ↓
8. ActivateTenantCommandHandler.handle(command)
   ↓
9. TenantRepository.findById(tenantId)
   ↓
10. tenant.activate() - Domain logic
    ↓
11. TenantRepository.save(tenant) - Persist
    ↓
12. KeycloakGroupPort.createOrEnableTenantGroup() - Create/enable group in wms-realm
    ↓
13. TenantEventPublisher.publish(TenantActivatedEvent)
    ↓
14. Kafka: TenantActivatedEvent published
    ↓
15. Response: 204 No Content
    ↓
16. Frontend: Success notification, refresh tenant data
```

### Complete Data Flow: List Tenants

```
1. User navigates to TenantListPage
   ↓
2. useTenants hook loads data
   ↓
3. tenantService.listTenants(params)
   ↓
4. API Client → GET /api/v1/tenants?page=1&size=20
   ↓
5. API Gateway routes to Tenant Service
   ↓
6. TenantQueryController.listTenants(params)
   ↓
7. ListTenantsQueryHandler.handle(query)
   ↓
8. TenantDataPort.findAll(query) - Read model query
   ↓
9. TenantViewRepositoryAdapter.findAll() - JPA query
   ↓
10. Response: Paginated TenantResponse[]
    ↓
11. Frontend: Display in data table
```

---

## Frontend Implementation

### TypeScript Types

```typescript
// types/tenant.ts
export interface Tenant {
  tenantId: string;
  name: string;
  status: TenantStatus;
  emailAddress?: string;
  phone?: string;
  address?: string;
  keycloakRealmName?: string;
  usePerTenantRealm: boolean;
  createdAt: string;
  activatedAt?: string;
  deactivatedAt?: string;
}

export type TenantStatus = 'PENDING' | 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';

export interface CreateTenantRequest {
  tenantId: string;
  name: string;
  emailAddress?: string;
  phone?: string;
  address?: string;
  keycloakRealmName?: string;
  usePerTenantRealm?: boolean;
}

export interface TenantListResponse {
  data: Tenant[];
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
```

### API Service

```typescript
// services/tenantService.ts
import apiClient from './apiClient';
import { ApiResponse } from '../types/api';
import { Tenant, CreateTenantRequest, TenantListResponse } from '../types/tenant';

export const tenantService = {
  // Commands
  async createTenant(request: CreateTenantRequest): Promise<ApiResponse<{ tenantId: string; success: boolean; message: string }>> {
    const response = await apiClient.post('/tenants', request);
    return response.data;
  },

  async activateTenant(id: string): Promise<ApiResponse<void>> {
    const response = await apiClient.put(`/tenants/${id}/activate`);
    return response.data;
  },

  async deactivateTenant(id: string): Promise<ApiResponse<void>> {
    const response = await apiClient.put(`/tenants/${id}/deactivate`);
    return response.data;
  },

  async suspendTenant(id: string): Promise<ApiResponse<void>> {
    const response = await apiClient.put(`/tenants/${id}/suspend`);
    return response.data;
  },

  // Queries
  async getTenant(id: string): Promise<ApiResponse<Tenant>> {
    const response = await apiClient.get(`/tenants/${id}`);
    return response.data;
  },

  async listTenants(params: { page?: number; size?: number; status?: TenantStatus }): Promise<ApiResponse<TenantListResponse>> {
    const response = await apiClient.get('/tenants', { params });
    return response.data;
  },
};
```

### Custom Hooks

```typescript
// hooks/useCreateTenant.ts
import { useState } from 'react';
import { tenantService } from '../services/tenantService';
import { CreateTenantRequest } from '../types/tenant';

export const useCreateTenant = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const createTenant = async (request: CreateTenantRequest) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await tenantService.createTenant(request);
      if (response.error) {
        throw new Error(response.error.message);
      }
      return response.data;
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to create tenant');
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { createTenant, isLoading, error };
};
```

---

## Backend Integration

### Existing Backend Implementation

The tenant service backend is already implemented with:

- ✅ Domain core with `Tenant` aggregate
- ✅ Application service layer with command/query handlers
- ✅ REST API controllers (command and query separated)
- ✅ Repository adapters
- ✅ Event publishers

### Integration Points

1. **API Gateway Routing**
    - Route `/api/v1/tenants/**` to tenant-service
    - Authentication and authorization at gateway level

2. **Keycloak Integration (Single Realm Strategy)**
    - **Realm:** All tenants use single `wms-realm`
    - **Tenant Groups:** Each tenant has a corresponding group in Keycloak
    - **Group Creation:** Tenant group created on tenant creation (optional) or activation
    - **Group Naming:** Group name pattern: `tenant-{tenantId}` (e.g., `tenant-ldp-001`)
    - **Group Management:** Groups enabled/disabled based on tenant status
    - **User Assignment:** Users assigned to tenant group for organization
    - **User Attribute:** Users still have `tenant_id` attribute for multi-tenancy enforcement

3. **Event Publishing**
    - Domain events published to Kafka
    - Other services can consume tenant events

### Keycloak Integration Details

**Single Realm Strategy:**

- All tenants share the `wms-realm` Keycloak realm
- Tenant groups created in `wms-realm` for organization
- Users differentiated by `tenant_id` user attribute (primary multi-tenancy mechanism)
- Tenant groups used for user organization and optional role assignment

**Group Lifecycle:**

- **On Tenant Creation:** Optionally create tenant group (can be deferred to activation)
- **On Tenant Activation:** Create/enable tenant group if not exists
- **On Tenant Deactivation:** Disable tenant group (users cannot authenticate)
- **On Tenant Suspension:** Disable tenant group (users cannot authenticate)

**Port Interface:**

```java
// KeycloakGroupPort (from common-keycloak)
public interface KeycloakGroupPort {
    void createTenantGroup(String tenantId);
    void enableTenantGroup(String tenantId);
    void disableTenantGroup(String tenantId);
    boolean tenantGroupExists(String tenantId);
}
```

**Group Name Pattern:**

- Group name: `tenant-{tenantId}`
- Example: `tenant-ldp-001` for tenant ID `ldp-001`
- Group path: `/tenants/tenant-{tenantId}` (optional hierarchical structure)

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

3. **E2E Tests**
    - Complete tenant creation flow
    - Tenant activation flow
    - Tenant management operations

### Backend Testing

1. **Domain Tests**
    - Business rule validation
    - Status transitions
    - Event publishing

2. **Integration Tests**
    - API endpoints
    - Repository operations
    - Event publishing

3. **Contract Tests**
    - API contract validation
    - DTO validation

---

## Implementation Phases

### Phase 1: Foundation (Week 1)

- [ ] Set up frontend feature structure
- [ ] Create TypeScript types
- [ ] Implement API service client
- [ ] Create custom hooks

### Phase 2: Tenant List (Week 1-2)

- [ ] Implement TenantListPage
- [ ] Implement TenantList component
- [ ] Add pagination and filtering
- [ ] Add search functionality

### Phase 3: Tenant Creation (Week 2)

- [ ] Implement TenantCreatePage
- [ ] Implement TenantForm component
- [ ] Add form validation
- [ ] Integrate with backend

### Phase 4: Tenant Detail & Actions (Week 2-3)

- [ ] Implement TenantDetailPage
- [ ] Implement tenant status management
- [ ] Add confirmation dialogs
- [ ] Add success/error notifications

### Phase 5: Testing & Polish (Week 3)

- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Accessibility audit
- [ ] Performance optimization
- [ ] Documentation

---

## Detailed Feature Plans

For detailed implementation plans for each feature, see:

1. **[Tenant Creation Implementation Plan](02-Tenant_Creation_Plan.md)**
    - Complete UI/UX design
    - Form structure and validation
    - Data flow from frontend to backend
    - Domain modeling details

2. **[Tenant Activation Implementation Plan](03-Tenant_Activation_Plan.md)**
    - Activation workflow
    - Keycloak integration
    - Event publishing
    - Error handling

3. **[Tenant Management Implementation Plan](04-Tenant_Management_Plan.md)**
    - List view with pagination
    - Detail view
    - Status management
    - Configuration updates

---

## Keycloak Integration Strategy

### Single Realm with Tenant Groups

The system uses a **Single Realm strategy** where all tenants share the `wms-realm` Keycloak realm, with tenant groups used for user organization.

**Key Characteristics:**

- **Single Realm:** All tenants use `wms-realm`
- **Tenant Groups:** Each tenant has a corresponding group in Keycloak
- **User Attribute:** Users have `tenant_id` attribute for multi-tenancy enforcement
- **Group Purpose:** Groups used for user organization and optional role assignment

### Tenant Group Lifecycle

1. **Tenant Creation:**
    - Tenant group can be created immediately (optional)
    - Or deferred until tenant activation
    - Group name: `tenant-{tenantId}`

2. **Tenant Activation:**
    - Create tenant group if not exists
    - Enable tenant group
    - Users can now be assigned to this group

3. **Tenant Deactivation:**
    - Disable tenant group
    - Users in group cannot authenticate
    - Group remains for historical purposes

4. **Tenant Suspension:**
    - Disable tenant group
    - Users in group cannot authenticate
    - Group can be re-enabled when tenant is reactivated

### User Service Integration

**Realm Determination:**

- User Service queries Tenant Service: `GET /api/v1/tenants/{id}/realm`
- Returns `null` for single realm strategy (uses default `wms-realm`)
- User Service always uses `wms-realm` for user creation

**User Creation Flow:**

1. Validate tenant exists and is ACTIVE
2. Determine realm: Always `wms-realm` (from configuration)
3. Create user in `wms-realm`
4. Set `tenant_id` user attribute
5. Assign user to tenant group: `tenant-{tenantId}`

**Tenant Validation:**

- Before user creation, validate tenant exists in Tenant Service
- Validate tenant status is ACTIVE
- Reject user creation for non-ACTIVE tenants

## References

- [Service Architecture Document](../../01-architecture/Service_Architecture_Document.md)
- [Frontend Architecture Document](../../01-architecture/Frontend_Architecture_Document.md)
- [Tenant Service Implementation Plan](../../01-architecture/Tenant_Service_Implementation_Plan.md)
- [API Specifications](../../02-api/API_Specifications.md)
- [IAM Integration Guide](../../03-security/IAM_Integration_Guide.md)
- [Mandated Implementation Template Guide](../../guide/mandated-Implementation-template-guide.md)
- [Clean Code Guidelines](clean-code-guidelines-per-module.md)

---

**Document Status:** Draft  
**Last Updated:** 2025-01  
**Next Review:** 2025-02

