# Tenant Management Implementation Plan

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-01  
**Status:** Draft  
**Related Documents:**

- [Tenant Management Implementation Plan](01-Tenant_Management_Implementation_Plan.md)
- [Service Architecture Document](../../01-architecture/Service_Architecture_Document.md)
- [IAM Integration Guide](../../03-security/IAM_Integration_Guide.md)

---

## Table of Contents

1. [Overview](#overview)
2. [Tenant List View](#tenant-list-view)
3. [Tenant Detail View](#tenant-detail-view)
4. [Tenant Status Management](#tenant-status-management)
5. [Tenant Configuration Management](#tenant-configuration-management)
6. [Frontend Implementation](#frontend-implementation)
7. [Backend Data Flow](#backend-data-flow)
8. [Domain Modeling](#domain-modeling)
9. [Testing Plan](#testing-plan)

---

## Overview

### Purpose

This document provides a detailed implementation plan for managing tenants (Local Distribution Partners) in the system. This includes viewing tenant lists, tenant details, and
managing tenant lifecycle operations (activate, deactivate, suspend) and configuration.

### Business Requirements

- **Who:** SYSTEM_ADMIN (full access), USER (own tenant only)
- **What:** View, list, and manage tenants
- **When:** On-demand, for tenant administration
- **Why:** Enable administrators to manage LDP tenants and their lifecycle

### Key Features

1. **Tenant List** - Paginated, searchable, filterable list
2. **Tenant Detail** - Comprehensive tenant information view
3. **Status Management** - Activate, deactivate, suspend operations
4. **Configuration Management** - Update tenant configuration
5. **Quick Actions** - Inline actions from list view

### Implementation Status (2025-11)

- **Backend delivery** – `services/tenant-service/tenant-domain/tenant-application-service` now exposes paginated list queries via `TenantListQueryHandler`, the REST API in
  `tenant-application` adds `/api/v1/tenants` listing plus status endpoints, and `tenant-dataaccess` provides filterable JPA adapters. Keycloak group orchestration is implemented
  in `tenant-messaging` through `KeycloakGroupAdapter`, ensuring tenant groups are created/enabled/disabled according to lifecycle events.
- **Front-end delivery** – `frontend-app/src/features/tenant-management` contains feature-scoped services, hooks, components, and pages (`TenantListPage`, `TenantCreatePage`,
  `TenantDetailPage`). Routes `/admin/tenants`, `/admin/tenants/create`, and `/admin/tenants/:tenantId` are protected via `ProtectedRoute` for `SYSTEM_ADMIN`.
- **Testing & QA** – Domain unit tests covering schema event publication (`TenantTest`), Vitest component tests (`TenantStatusBadge.test.tsx`), and CI-friendly commands (
  `mvn -pl services/tenant-service/tenant-container -am test`, `npm run test -- --run`, `npm run lint`) were executed successfully.
- **Operational notes** – Keycloak default realm (`keycloak.admin.defaultRealm`) is used for shared tenants while per-tenant realms remain supported. Tenant-related logs surface
  via `tenant-service.log` under `logs/`.

---

## Tenant List View

### Page Layout

**Route:** `/admin/tenants`  
**Access:** SYSTEM_ADMIN only  
**Layout:** Full-width data table with toolbar

### UI Design

```
┌─────────────────────────────────────────────────────────────┐
│  Tenants (LDPs)                              [+ Create]    │
├─────────────────────────────────────────────────────────────┤
│  [Search...]  [Status: All ▼]  [Sort: Name ▼]              │
├─────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────┐ │
│  │ Tenant ID │ Name              │ Status   │ Actions   │ │
│  ├───────────┼───────────────────┼──────────┼───────────┤ │
│  │ ldp-001   │ LDP 001           │ [ACTIVE] │ [⋮]       │ │
│  │ ldp-002   │ LDP 002           │ [PENDING]│ [⋮]       │ │
│  │ ldp-003   │ LDP 003           │ [ACTIVE] │ [⋮]       │ │
│  └───────────────────────────────────────────────────────┘ │
│  Showing 1-20 of 150                    [<] 1 2 3 [>]     │
└─────────────────────────────────────────────────────────────┘
```

### Features

1. **Data Table**
    - Columns: Tenant ID, Name, Status, Created Date, Actions
    - Sortable columns
    - Row selection (for bulk actions - future)

2. **Search & Filtering**
    - Search by tenant ID or name
    - Filter by status (All, PENDING, ACTIVE, INACTIVE, SUSPENDED)
    - Filter by date range (future)

3. **Pagination**
    - Page size: 20, 50, 100
    - Page navigation
    - Total count display

4. **Quick Actions**
    - View detail (link to detail page)
    - Activate (if PENDING)
    - Deactivate (if ACTIVE or SUSPENDED)
    - Suspend (if ACTIVE)
    - Actions menu (⋮) for all options

5. **Create Button**
    - Prominent "Create Tenant" button
    - Links to create page

### Status Badge Design

- **PENDING** - Yellow/Amber badge
- **ACTIVE** - Green badge
- **INACTIVE** - Gray badge
- **SUSPENDED** - Orange/Red badge

---

## Tenant Detail View

### Page Layout

**Route:** `/admin/tenants/{id}`  
**Access:** SYSTEM_ADMIN or USER (own tenant)  
**Layout:** Card-based layout with sections

### UI Design

```
┌─────────────────────────────────────────────────────────────┐
│  Tenant Details: Local Distribution Partner 001             │
│  [← Back to List]                    [Edit] [Actions ▼]   │
├─────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────┐ │
│  │ Basic Information                                      │ │
│  ├───────────────────────────────────────────────────────┤ │
│  │ Tenant ID:     ldp-001                                 │ │
│  │ Name:          Local Distribution Partner 001          │ │
│  │ Status:        [ACTIVE]                               │ │
│  │ Created:       2025-01-10 10:00:00                    │ │
│  │ Activated:     2025-01-15 10:30:00                    │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐ │
│  │ Contact Information                                    │ │
│  ├───────────────────────────────────────────────────────┤ │
│  │ Email:         contact@ldp001.com                      │ │
│  │ Phone:         +27123456789                           │ │
│  │ Address:       123 Main Street, Johannesburg         │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐ │
│  │ Keycloak Integration                                   │ │
│  ├───────────────────────────────────────────────────────┤ │
│  │ Realm:                wms-realm                        │ │
│  │ Tenant Group:         tenant-ldp-001                   │ │
│  │ Group Status:         Enabled                          │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐ │
│  │ Actions                                                │ │
│  ├───────────────────────────────────────────────────────┤ │
│  │ [Activate] [Deactivate] [Suspend] [Update Config]     │ │
│  └───────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Features

1. **Information Sections**
    - Basic Information (ID, name, status, dates)
    - Contact Information (emailAddress, phone, address)
    - Keycloak Integration (realm and group information)
    - Related Information (users count, etc. - future)

2. **Status Management**
    - Status badge with color coding
    - Action buttons based on current status
    - Confirmation dialogs for state changes

3. **Keycloak Integration Display**
    - Realm name: `wms-realm` (always)
    - Tenant group name: `tenant-{tenantId}`
    - Group status (enabled/disabled)
    - Group creation date (if available)

4. **Navigation**
    - Back to list button
    - Edit button (future)
    - Actions dropdown menu

---

## Tenant Status Management

### Status Transitions

| From      | To        | Action     | UI Button                |
|-----------|-----------|------------|--------------------------|
| PENDING   | ACTIVE    | Activate   | "Activate" (primary)     |
| ACTIVE    | INACTIVE  | Deactivate | "Deactivate" (secondary) |
| ACTIVE    | SUSPENDED | Suspend    | "Suspend" (warning)      |
| SUSPENDED | ACTIVE    | Activate   | "Activate" (primary)     |
| SUSPENDED | INACTIVE  | Deactivate | "Deactivate" (secondary) |
| INACTIVE  | ACTIVE    | Activate   | "Activate" (primary)     |

### Action Buttons

Buttons are conditionally displayed based on current status:

```typescript
{tenant.status === 'PENDING' && (
  <Button onClick={handleActivate}>Activate</Button>
)}
{tenant.status === 'ACTIVE' && (
  <>
    <Button onClick={handleDeactivate}>Deactivate</Button>
    <Button onClick={handleSuspend}>Suspend</Button>
  </>
)}
{tenant.status === 'SUSPENDED' && (
  <>
    <Button onClick={handleActivate}>Activate</Button>
    <Button onClick={handleDeactivate}>Deactivate</Button>
  </>
)}
{tenant.status === 'INACTIVE' && (
  <Button onClick={handleActivate}>Activate</Button>
)}
```

### Confirmation Dialogs

Each status change requires confirmation:

1. **Activate** - "Are you sure you want to activate this tenant?"
2. **Deactivate** - "Are you sure you want to deactivate this tenant?"
3. **Suspend** - "Are you sure you want to suspend this tenant?"

---

## Tenant Configuration Management

### Configuration View

Display current configuration:

- Use Per-Tenant Realm (Yes/No)
- Realm Name (if applicable)
- Other configuration settings (future)

### Configuration Update

**Dialog/Form:**

```
┌─────────────────────────────────────────┐
│  Update Tenant Configuration            │
├─────────────────────────────────────────┤
│                                         │
│  ☐ Use Per-Tenant Realm                 │
│  Realm Name: [tenant-ldp-001]          │
│                                         │
│  [Cancel]              [Update]          │
│                                         │
└─────────────────────────────────────────┘
```

### Update Flow

1. Click "Update Configuration" button
2. Open configuration dialog
3. Edit configuration fields
4. Submit update
5. Show success notification
6. Refresh tenant data

---

## Frontend Implementation

### Component Structure

```
features/tenant-management/
├── components/
│   ├── TenantList.tsx              # List table component
│   ├── TenantListToolbar.tsx       # Search, filter, create button
│   ├── TenantDetail.tsx            # Detail view component
│   ├── TenantInfoCard.tsx          # Information card sections
│   ├── TenantActions.tsx           # Action buttons
│   ├── TenantStatusBadge.tsx        # Status badge
│   └── UpdateConfigDialog.tsx      # Configuration update dialog
├── pages/
│   ├── TenantListPage.tsx           # List page
│   └── TenantDetailPage.tsx        # Detail page
├── hooks/
│   ├── useTenants.ts               # List hook
│   ├── useTenant.ts                # Single tenant hook
│   └── useTenantActions.ts          # Actions hook
└── services/
    └── tenantService.ts            # API client
```

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
  keycloakGroupName?: string;
  keycloakGroupEnabled?: boolean;
  createdAt: string;
  activatedAt?: string;
  deactivatedAt?: string;
}

export interface TenantListParams {
  page?: number;
  size?: number;
  status?: TenantStatus;
  search?: string;
  sort?: string;
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

### Custom Hook: useTenants

```typescript
// hooks/useTenants.ts
import { useQuery } from '@tanstack/react-query';
import { tenantService } from '../services/tenantService';
import { TenantListParams, TenantListResponse } from '../types/tenant';

export const useTenants = (params: TenantListParams = {}) => {
  return useQuery<TenantListResponse>(
    ['tenants', params],
    () => tenantService.listTenants(params),
    {
      keepPreviousData: true,
      staleTime: 30000, // 30 seconds
    }
  );
};
```

### Custom Hook: useTenant

```typescript
// hooks/useTenant.ts
import { useQuery } from '@tanstack/react-query';
import { tenantService } from '../services/tenantService';
import { Tenant } from '../types/tenant';

export const useTenant = (tenantId: string) => {
  return useQuery<Tenant>(
    ['tenant', tenantId],
    () => tenantService.getTenant(tenantId),
    {
      enabled: !!tenantId,
      staleTime: 30000,
    }
  );
};
```

### TenantListPage Component

```typescript
// pages/TenantListPage.tsx
import React, { useState } from 'react';
import { Container, Paper, Typography } from '@mui/material';
import { TenantList } from '../components/TenantList';
import { TenantListToolbar } from '../components/TenantListToolbar';
import { useTenants } from '../hooks/useTenants';
import { TenantStatus } from '../types/tenant';

export const TenantListPage: React.FC = () => {
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [status, setStatus] = useState<TenantStatus | undefined>();
  const [search, setSearch] = useState('');

  const { data, isLoading, error } = useTenants({
    page,
    size,
    status,
    search: search || undefined,
  });

  return (
    <Container maxWidth="xl" sx={{ mt: 4, mb: 4 }}>
      <Paper elevation={3} sx={{ p: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Tenants (LDPs)
        </Typography>

        <TenantListToolbar
          search={search}
          onSearchChange={setSearch}
          status={status}
          onStatusChange={setStatus}
        />

        <TenantList
          tenants={data?.data || []}
          pagination={data?.meta.pagination}
          isLoading={isLoading}
          onPageChange={setPage}
          onPageSizeChange={setSize}
        />
      </Paper>
    </Container>
  );
};
```

### TenantDetailPage Component

```typescript
// pages/TenantDetailPage.tsx
import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Container, Paper, Typography, Button, Box } from '@mui/material';
import { ArrowBack } from '@mui/icons-material';
import { TenantDetail } from '../components/TenantDetail';
import { TenantActions } from '../components/TenantActions';
import { useTenant } from '../hooks/useTenant';
import { useTenantActions } from '../hooks/useTenantActions';

export const TenantDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: tenant, isLoading } = useTenant(id!);
  const { activateTenant, deactivateTenant, suspendTenant } = useTenantActions();

  const handleActionComplete = () => {
    // Refetch tenant data
    // This is handled by React Query invalidation in the hook
  };

  if (isLoading) {
    return <div>Loading...</div>;
  }

  if (!tenant) {
    return <div>Tenant not found</div>;
  }

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Paper elevation={3} sx={{ p: 4 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
          <Box>
            <Button
              startIcon={<ArrowBack />}
              onClick={() => navigate('/admin/tenants')}
            >
              Back to List
            </Button>
            <Typography variant="h4" component="h1" sx={{ mt: 2 }}>
              Tenant Details: {tenant.name}
            </Typography>
          </Box>
          <TenantActions tenant={tenant} onActionComplete={handleActionComplete} />
        </Box>

        <TenantDetail tenant={tenant} />
      </Paper>
    </Container>
  );
};
```

---

## Backend Data Flow

### List Tenants Endpoint

**GET** `/api/v1/tenants?page=1&size=20&status=ACTIVE&search=ldp`

**Response:** `200 OK`

```json
{
  "data": [
    {
      "tenantId": "ldp-001",
      "name": "Local Distribution Partner 001",
      "status": "ACTIVE",
      "emailAddress": "contact@ldp001.com",
      "phone": "+27123456789",
      "address": "123 Main Street, Johannesburg",
      "keycloakRealmName": "tenant-ldp-001",
      "usePerTenantRealm": true,
      "createdAt": "2025-01-10T10:00:00Z",
      "activatedAt": "2025-01-15T10:30:00Z"
    }
  ],
  "meta": {
    "pagination": {
      "page": 1,
      "size": 20,
      "totalElements": 150,
      "totalPages": 8,
      "hasNext": true,
      "hasPrevious": false
    }
  }
}
```

### Get Tenant Endpoint

**GET** `/api/v1/tenants/{id}`

**Response:** `200 OK`

```json
{
  "data": {
    "tenantId": "ldp-001",
    "name": "Local Distribution Partner 001",
    "status": "ACTIVE",
    "emailAddress": "contact@ldp001.com",
    "phone": "+27123456789",
    "address": "123 Main Street, Johannesburg",
    "keycloakRealmName": "tenant-ldp-001",
    "usePerTenantRealm": true,
    "createdAt": "2025-01-10T10:00:00Z",
    "activatedAt": "2025-01-15T10:30:00Z"
  }
}
```

### Query Handler Implementation

```java
// ListTenantsQueryHandler.java
@Component
@Transactional(readOnly = true)
public class ListTenantsQueryHandler {
    private final TenantDataPort tenantDataPort;

    public TenantListResult handle(ListTenantsQuery query) {
        // Query read model (projection)
        Page<TenantView> views = tenantDataPort.findAll(
            query.getPage(),
            query.getSize(),
            query.getStatus(),
            query.getSearch()
        );

        // Map to DTOs
        List<TenantResponse> tenants = views.getContent().stream()
            .map(TenantMapper::toTenantResponse)
            .collect(Collectors.toList());

        return new TenantListResult(
            tenants,
            new PaginationMeta(
                views.getNumber(),
                views.getSize(),
                views.getTotalElements(),
                views.getTotalPages(),
                views.hasNext(),
                views.hasPrevious()
            )
        );
    }
}
```

---

## Domain Modeling

### Read Model: TenantView

For query operations, a read model (projection) is used:

```java
// TenantView.java (read model)
@Entity
@Table(name = "tenant_views")
public class TenantView {
    @Id
    private String tenantId;
    private String name;
    private String status;
    private String emailAddress;
    private String phone;
    private String address;
    private String keycloakRealmName;
    private boolean usePerTenantRealm;
    private LocalDateTime createdAt;
    private LocalDateTime activatedAt;
    private LocalDateTime deactivatedAt;
    
    // Getters and setters...
}
```

### Projection Updates

The read model is updated via event projections:

```java
// TenantViewProjection.java
@Component
public class TenantViewProjection {
    private final TenantViewRepository tenantViewRepository;

    @KafkaListener(topics = "tenant-events")
    public void handle(TenantCreatedEvent event) {
        // Create tenant view
        TenantView view = new TenantView();
        // Map from event...
        tenantViewRepository.save(view);
    }

    @KafkaListener(topics = "tenant-events")
    public void handle(TenantActivatedEvent event) {
        // Update tenant view status
        TenantView view = tenantViewRepository.findById(event.getAggregateId().getValue())
            .orElseThrow();
        view.setStatus("ACTIVE");
        view.setActivatedAt(event.getActivatedAt());
        tenantViewRepository.save(view);
    }
    
    // Other event handlers...
}
```

---

## Testing Plan

### Frontend Tests

1. **Unit Tests**
    - TenantList component
    - TenantDetail component
    - TenantActions component
    - Custom hooks

2. **Integration Tests**
    - List page with pagination
    - Detail page data loading
    - Status management actions
    - Configuration updates

3. **E2E Tests**
    - Complete tenant management flow
    - Search and filtering
    - Status transitions
    - Error scenarios

### Backend Tests

1. **Query Tests**
    - List tenants with pagination
    - Filtering and search
    - Get tenant by ID

2. **Integration Tests**
    - API endpoints
    - Query handlers
    - Read model queries

---

## References

- [Tenant Management Implementation Plan](01-Tenant_Management_Implementation_Plan.md)
- [Tenant Creation Plan](02-Tenant_Creation_Plan.md)
- [Tenant Activation Plan](03-Tenant_Activation_Plan.md)
- [Service Architecture Document](../../01-architecture/Service_Architecture_Document.md)
- [API Specifications](../../02-api/API_Specifications.md)

---

**Document Status:** Draft  
**Last Updated:** 2025-01  
**Next Review:** 2025-02

