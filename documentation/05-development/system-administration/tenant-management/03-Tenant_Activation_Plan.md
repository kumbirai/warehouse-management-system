# Tenant Activation Implementation Plan

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
2. [UI/UX Design](#uiux-design)
3. [Frontend Implementation](#frontend-implementation)
4. [Backend Data Flow](#backend-data-flow)
5. [Keycloak Integration](#keycloak-integration)
6. [Domain Modeling](#domain-modeling)
7. [Event Publishing](#event-publishing)
8. [Error Handling](#error-handling)
9. [Testing Plan](#testing-plan)

---

## Overview

### Purpose

This document provides a detailed implementation plan for activating tenants (Local Distribution Partners) in the system. Activation involves changing tenant status from PENDING to
ACTIVE and optionally creating/enabling Keycloak realms.

### Business Requirements

- **Who:** SYSTEM_ADMIN users only
- **What:** Activate a PENDING tenant
- **When:** After tenant creation, before tenant can be used
- **Why:** Enable tenant to use the system and create users

### Key Features

1. **Status Transition** - PENDING → ACTIVE
2. **Keycloak Integration** - Tenant group creation/enabling in `wms-realm`
3. **Event Publishing** - `TenantActivatedEvent` published
4. **Confirmation Dialog** - Confirm activation action
5. **Success Handling** - Update UI, show notification

### Business Rules

- Only PENDING tenants can be activated
- Activation cannot be undone (must deactivate instead)
- Keycloak realm created/enabled if configured
- Domain event published for downstream services

---

## UI/UX Design

### Activation Trigger

Activation can be triggered from:

1. **Tenant Detail Page** - Primary action button
2. **Tenant List Page** - Quick action in table row
3. **Bulk Actions** - Activate multiple tenants (future)

### Confirmation Dialog

```
┌─────────────────────────────────────────┐
│  Activate Tenant                        │
├─────────────────────────────────────────┤
│                                         │
│  Are you sure you want to activate      │
│  this tenant?                          │
│                                         │
│  Tenant: Local Distribution Partner 001 │
│  ID: ldp-001                            │
│                                         │
│  This will:                             │
│  • Change status to ACTIVE              │
│  • Create/enable Keycloak tenant group │
│  • Allow user creation for this tenant │
│                                         │
│  [Cancel]              [Activate]       │
│                                         │
└─────────────────────────────────────────┘
```

### Status Badge Update

**Before Activation:**

```
Status: [PENDING] (yellow badge)
```

**After Activation:**

```
Status: [ACTIVE] (green badge)
Activated: 2025-01-15 10:30:00
```

### Loading States

- **Button Loading:** Show spinner, disable button
- **Optimistic Update:** Update UI immediately (optional)
- **Success:** Show success notification
- **Error:** Show error notification, revert UI

---

## Frontend Implementation

### Component Structure

```
features/tenant-management/
├── components/
│   ├── TenantActions.tsx          # Action buttons
│   ├── ActivateTenantDialog.tsx   # Confirmation dialog
│   └── TenantStatusBadge.tsx      # Status display
├── hooks/
│   └── useTenantActions.ts        # Activation hook
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
  activatedAt?: string;
  // ... other fields
}

export type TenantStatus = 'PENDING' | 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
```

### Custom Hook: useTenantActions

```typescript
// hooks/useTenantActions.ts
import { useState } from 'react';
import { tenantService } from '../services/tenantService';
import { useSnackbar } from 'notistack';
import { useQueryClient } from '@tanstack/react-query'; // or similar

export const useTenantActions = () => {
  const [isActivating, setIsActivating] = useState(false);
  const { enqueueSnackbar } = useSnackbar();
  const queryClient = useQueryClient();

  const activateTenant = async (tenantId: string) => {
    setIsActivating(true);
    try {
      const response = await tenantService.activateTenant(tenantId);
      
      if (response.error) {
        throw new Error(response.error.message);
      }
      
      // Invalidate and refetch tenant data
      await queryClient.invalidateQueries(['tenant', tenantId]);
      await queryClient.invalidateQueries(['tenants']);
      
      enqueueSnackbar('Tenant activated successfully', { variant: 'success' });
      return true;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to activate tenant';
      enqueueSnackbar(message, { variant: 'error' });
      throw error;
    } finally {
      setIsActivating(false);
    }
  };

  return { activateTenant, isActivating };
};
```

### ActivateTenantDialog Component

```typescript
// components/ActivateTenantDialog.tsx
import React from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
} from '@mui/material';
import { CheckCircle } from '@mui/icons-material';
import { Tenant } from '../types/tenant';

interface ActivateTenantDialogProps {
  open: boolean;
  tenant: Tenant;
  onConfirm: () => Promise<void>;
  onCancel: () => void;
  isLoading?: boolean;
}

export const ActivateTenantDialog: React.FC<ActivateTenantDialogProps> = ({
  open,
  tenant,
  onConfirm,
  onCancel,
  isLoading = false,
}) => {
  const handleConfirm = async () => {
    await onConfirm();
  };

  return (
    <Dialog open={open} onClose={onCancel} maxWidth="sm" fullWidth>
      <DialogTitle>Activate Tenant</DialogTitle>
      <DialogContent>
        <Typography variant="body1" gutterBottom>
          Are you sure you want to activate this tenant?
        </Typography>
        
        <Box sx={{ mt: 2, mb: 2, p: 2, bgcolor: 'grey.50', borderRadius: 1 }}>
          <Typography variant="subtitle2" gutterBottom>
            Tenant Information
          </Typography>
          <Typography variant="body2">Name: {tenant.name}</Typography>
          <Typography variant="body2">ID: {tenant.tenantId}</Typography>
        </Box>

        <Typography variant="subtitle2" gutterBottom>
          This will:
        </Typography>
        <List dense>
          <ListItem>
            <ListItemIcon>
              <CheckCircle color="primary" fontSize="small" />
            </ListItemIcon>
            <ListItemText primary="Change status to ACTIVE" />
          </ListItem>
          <ListItem>
            <ListItemIcon>
              <CheckCircle color="primary" fontSize="small" />
            </ListItemIcon>
            <ListItemText primary="Create/enable Keycloak tenant group in wms-realm" />
          </ListItem>
          <ListItem>
            <ListItemIcon>
              <CheckCircle color="primary" fontSize="small" />
            </ListItemIcon>
            <ListItemText primary="Allow user creation for this tenant" />
          </ListItem>
        </List>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancel} disabled={isLoading}>
          Cancel
        </Button>
        <Button
          onClick={handleConfirm}
          variant="contained"
          color="primary"
          disabled={isLoading}
        >
          {isLoading ? 'Activating...' : 'Activate'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};
```

### TenantActions Component

```typescript
// components/TenantActions.tsx
import React, { useState } from 'react';
import { Button, ButtonGroup, IconButton, Menu, MenuItem } from '@mui/material';
import { MoreVert, CheckCircle, Cancel, Pause } from '@mui/icons-material';
import { Tenant } from '../types/tenant';
import { useTenantActions } from '../hooks/useTenantActions';
import { ActivateTenantDialog } from './ActivateTenantDialog';

interface TenantActionsProps {
  tenant: Tenant;
  onActionComplete?: () => void;
}

export const TenantActions: React.FC<TenantActionsProps> = ({
  tenant,
  onActionComplete,
}) => {
  const [activateDialogOpen, setActivateDialogOpen] = useState(false);
  const { activateTenant, isActivating } = useTenantActions();

  const handleActivate = async () => {
    try {
      await activateTenant(tenant.tenantId);
      setActivateDialogOpen(false);
      onActionComplete?.();
    } catch (error) {
      // Error handled in hook
    }
  };

  return (
    <>
      {tenant.status === 'PENDING' && (
        <Button
          variant="contained"
          color="primary"
          startIcon={<CheckCircle />}
          onClick={() => setActivateDialogOpen(true)}
          size="small"
        >
          Activate
        </Button>
      )}
      
      <ActivateTenantDialog
        open={activateDialogOpen}
        tenant={tenant}
        onConfirm={handleActivate}
        onCancel={() => setActivateDialogOpen(false)}
        isLoading={isActivating}
      />
    </>
  );
};
```

---

## Backend Data Flow

### API Endpoint

**PUT** `/api/v1/tenants/{id}/activate`

**Response:** `204 No Content`

### Request Flow

```
1. Frontend: PUT /api/v1/tenants/{id}/activate
   ↓
2. API Gateway: Route to tenant-service
   ↓
3. TenantCommandController.activateTenant(id)
   ↓
4. ActivateTenantCommandHandler.handle(command)
   ↓
5. TenantRepository.findById(tenantId)
   ↓
6. tenant.activate() - Domain logic
   ↓
7. TenantRepository.save(tenant) - Persist
   ↓
8. KeycloakRealmPort.createOrEnableRealm() - Integration
   ↓
9. TenantEventPublisher.publish(TenantActivatedEvent)
   ↓
10. Kafka: TenantActivatedEvent published
    ↓
11. Response: 204 No Content
```

### Command Handler Implementation

```java
// ActivateTenantCommandHandler.java
@Component
@Transactional
public class ActivateTenantCommandHandler {
    private final TenantRepository tenantRepository;
    private final TenantEventPublisher eventPublisher;
    private final KeycloakGroupPort keycloakGroupPort;

    public void handle(ActivateTenantCommand command) {
        TenantId tenantId = command.getTenantId();
        
        // Load tenant
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId.getValue()));
        
        // Validate can activate
        if (!tenant.canActivate()) {
            throw new IllegalStateException(
                String.format("Cannot activate tenant: current status is %s", tenant.getStatus())
            );
        }
        
        // Activate tenant (domain logic)
        tenant.activate();
        
        // Persist
        tenantRepository.save(tenant);
        
        // Keycloak integration: Create/enable tenant group in wms-realm
        // Group name pattern: tenant-{tenantId}
        String groupName = "tenant-" + tenantId.getValue();
        if (keycloakGroupPort.tenantGroupExists(tenantId.getValue())) {
            keycloakGroupPort.enableTenantGroup(tenantId.getValue());
        } else {
            keycloakGroupPort.createTenantGroup(tenantId.getValue());
        }
        
        // Publish events
        eventPublisher.publish(tenant.getDomainEvents());
        tenant.clearDomainEvents();
    }
}
```

---

## Keycloak Integration

### Single Realm with Tenant Groups Strategy

The tenant service integrates with Keycloak using a **Single Realm strategy** where all tenants share the `wms-realm` Keycloak realm, with tenant groups used for user organization.

**Key Characteristics:**

- **Single Realm:** All tenants use `wms-realm`
- **Tenant Groups:** Each tenant has a corresponding group in Keycloak
- **Group Naming:** Group name pattern: `tenant-{tenantId}` (e.g., `tenant-ldp-001`)
- **User Attribute:** Users have `tenant_id` attribute for multi-tenancy enforcement
- **Group Purpose:** Groups used for user organization and optional role assignment

### KeycloakGroupPort Interface

```java
// Port interface in application-service layer (from common-keycloak)
public interface KeycloakGroupPort {
    void createTenantGroup(String tenantId);
    void enableTenantGroup(String tenantId);
    void disableTenantGroup(String tenantId);
    boolean tenantGroupExists(String tenantId);
}
```

### Group Creation Implementation

```java
// KeycloakGroupAdapter.java (in tenant-service infrastructure)
@Component
public class KeycloakGroupAdapter implements KeycloakGroupPort {
    private final KeycloakGroupPort keycloakGroupPort; // From common-keycloak
    
    @Override
    public void createTenantGroup(String tenantId) {
        String groupName = "tenant-" + tenantId;
        GroupRepresentation group = new GroupRepresentation();
        group.setName(groupName);
        group.setPath("/tenants/" + groupName); // Optional hierarchical path
        keycloakGroupPort.createGroup("wms-realm", group);
    }
    
    @Override
    public void enableTenantGroup(String tenantId) {
        String groupName = "tenant-" + tenantId;
        keycloakGroupPort.updateGroup("wms-realm", groupName, group -> {
            group.setEnabled(true);
        });
    }
    
    @Override
    public void disableTenantGroup(String tenantId) {
        String groupName = "tenant-" + tenantId;
        keycloakGroupPort.updateGroup("wms-realm", groupName, group -> {
            group.setEnabled(false);
        });
    }
    
    @Override
    public boolean tenantGroupExists(String tenantId) {
        String groupName = "tenant-" + tenantId;
        return keycloakGroupPort.groupExists("wms-realm", groupName);
    }
}
```

### Group Lifecycle

1. **On Tenant Creation (Optional):**
    - Tenant group can be created immediately
    - Or deferred until tenant activation
    - Group created but may be disabled until activation

2. **On Tenant Activation:**
    - Create tenant group if not exists
    - Enable tenant group
    - Users can now be assigned to this group

3. **On Tenant Deactivation:**
    - Disable tenant group
    - Users in group cannot authenticate
    - Group remains for historical purposes

4. **On Tenant Suspension:**
    - Disable tenant group
    - Users in group cannot authenticate
    - Group can be re-enabled when tenant is reactivated

---

## Domain Modeling

### Domain Entity: Tenant.activate()

The activation logic is encapsulated in the domain entity:

```java
// Tenant.java (domain core)
public void activate() {
    if (this.status == TenantStatus.ACTIVE) {
        throw new IllegalStateException("Tenant is already active");
    }
    if (!this.status.canTransitionTo(TenantStatus.ACTIVE)) {
        throw new IllegalStateException(
            String.format("Cannot activate tenant: invalid status transition from %s", this.status)
        );
    }

    this.status = TenantStatus.ACTIVE;
    this.activatedAt = LocalDateTime.now();
    incrementVersion();

    // Publish domain event
    addDomainEvent(new TenantActivatedEvent(this.getId()));
}
```

### Business Rules

1. **Status Validation**
    - Only PENDING tenants can be activated
    - ACTIVE tenants cannot be activated again
    - Status transition validated by `TenantStatus.canTransitionTo()`

2. **Timestamp Recording**
    - `activatedAt` set to current time
    - Used for audit and reporting

3. **Version Increment**
    - Optimistic locking support
    - Prevents concurrent modification conflicts

### Domain Event: TenantActivatedEvent

```java
// TenantActivatedEvent.java (domain core)
public class TenantActivatedEvent extends TenantEvent<TenantId> {
    private final LocalDateTime activatedAt;

    public TenantActivatedEvent(TenantId tenantId) {
        super(tenantId);
        this.activatedAt = LocalDateTime.now();
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }
}
```

---

## Event Publishing

### Event Flow

```
1. Tenant.activate() called
   ↓
2. TenantActivatedEvent added to domain events
   ↓
3. TenantRepository.save(tenant)
   ↓
4. Transaction commits
   ↓
5. ActivateTenantCommandHandler publishes events
   ↓
6. TenantEventPublisher.publish(TenantActivatedEvent)
   ↓
7. Kafka: Event published to tenant-events topic
   ↓
8. Downstream services consume event
```

### Event Consumers

Potential consumers of `TenantActivatedEvent`:

1. **User Service** - Can now create users for this tenant
2. **Schema Creation Service** - Create tenant schema (if schema-per-tenant)
3. **Notification Service** - Send activation notification
4. **Audit Service** - Record activation in audit log

### Event Schema

```json
{
  "eventId": "evt-123",
  "eventType": "TenantActivatedEvent",
  "aggregateId": "ldp-001",
  "aggregateType": "Tenant",
  "tenantId": null,
  "timestamp": "2025-01-15T10:30:00Z",
  "version": 1,
  "payload": {
    "activatedAt": "2025-01-15T10:30:00Z"
  },
  "metadata": {
    "correlationId": "req-456",
    "causationId": "evt-122",
    "userId": "admin-001"
  }
}
```

---

## Error Handling

### Frontend Error Handling

1. **Validation Errors**
    - Tenant not found: Show error, redirect to list
    - Invalid status: Show error message
    - Already active: Show info message

2. **Keycloak Errors**
    - Group creation failed: Show error, tenant may still be activated
    - Log error for admin review
    - Group creation is non-blocking (tenant can be activated without group)

3. **Network Errors**
    - Show retry option
    - Allow manual retry

### Backend Error Handling

1. **Tenant Not Found** - `404 Not Found`
2. **Invalid Status** - `400 Bad Request` with specific message
3. **Keycloak Error** - Log error, continue with activation (tenant activated, group issue logged)

### Error Response Format

```json
{
  "error": {
    "code": "INVALID_STATUS_TRANSITION",
    "message": "Cannot activate tenant: current status is ACTIVE",
    "timestamp": "2025-01-15T10:30:00Z",
    "path": "/api/v1/tenants/ldp-001/activate",
    "requestId": "req-123"
  }
}
```

---

## Testing Plan

### Frontend Tests

1. **Unit Tests**
    - ActivateTenantDialog component
    - useTenantActions hook
    - TenantActions component

2. **Integration Tests**
    - Activation flow with confirmation
    - Success handling
    - Error handling

3. **E2E Tests**
    - Complete activation flow
    - Status update verification
    - Notification display

### Backend Tests

1. **Domain Tests**
    - Tenant.activate() business rules
    - Status transition validation
    - Event publishing

2. **Integration Tests**
    - API endpoint
    - Command handler
    - Keycloak integration
    - Event publishing

---

## References

- [Tenant Management Implementation Plan](01-Tenant_Management_Implementation_Plan.md)
- [Tenant Creation Plan](02-Tenant_Creation_Plan.md)
- [Service Architecture Document](../../01-architecture/Service_Architecture_Document.md)
- [Keycloak Integration Guide](../../01-architecture/Keycloak_Integration_DRY_Strategy.md)

---

**Document Status:** Draft  
**Last Updated:** 2025-01  
**Next Review:** 2025-02

