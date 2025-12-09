# Tenant Creation Implementation Plan

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-01  
**Status:** Draft  
**Related Documents:**

- [Tenant Management Implementation Plan](01-Tenant_Management_Implementation_Plan.md)
- [Service Architecture Document](../../01-architecture/Service_Architecture_Document.md)
- [Frontend Architecture Document](../../01-architecture/Frontend_Architecture_Document.md)
- [IAM Integration Guide](../../03-security/IAM_Integration_Guide.md)

---

## Table of Contents

1. [Overview](#overview)
2. [UI/UX Design](#uiux-design)
3. [Frontend Implementation](#frontend-implementation)
4. [Backend Data Flow](#backend-data-flow)
5. [Domain Modeling](#domain-modeling)
6. [Validation Strategy](#validation-strategy)
7. [Error Handling](#error-handling)
8. [Testing Plan](#testing-plan)

---

## Overview

### Purpose

This document provides a detailed implementation plan for creating tenants (Local Distribution Partners) in the system. The plan covers the complete flow from production-grade UI
to backend domain modeling, ensuring proper data routing and domain-driven design principles.

### Business Requirements

- **Who:** SYSTEM_ADMIN users only
- **What:** Create new tenant (LDP) with required and optional information
- **When:** On-demand, when onboarding new distribution partners
- **Why:** Enable new LDPs to use the warehouse management system

### Key Features

1. **Multi-Step Form** (optional) or single comprehensive form
2. **Real-Time Validation** - Client-side and server-side
3. **Keycloak Integration** - Tenant group creation in single `wms-realm`
4. **Success Handling** - Redirect to tenant detail page
5. **Error Handling** - Clear, actionable error messages

---

## UI/UX Design

### Page Layout

**Route:** `/admin/tenants/create`  
**Access:** SYSTEM_ADMIN role required  
**Layout:** Full-width form with card container

### Form Structure

#### Option 1: Single Comprehensive Form (Recommended)

```
┌─────────────────────────────────────────────────┐
│  Create Tenant (LDP)                            │
├─────────────────────────────────────────────────┤
│                                                 │
│  Basic Information                              │
│  ┌─────────────────────────────────────────┐   │
│  │ Tenant ID *        [____________]       │   │
│  │ Tenant Name *      [____________]       │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  Contact Information                            │
│  ┌─────────────────────────────────────────┐   │
│  │ Email            [____________]         │   │
│  │ Phone            [____________]         │   │
│  │ Address          [____________]       │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  Keycloak Integration                          │
│  ┌─────────────────────────────────────────┐   │
│  │ ℹ️  Tenant will be created in wms-realm │   │
│  │    Tenant group will be created on      │   │
│  │    activation: tenant-{tenantId}         │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  [Cancel]                    [Create Tenant]   │
│                                                 │
└─────────────────────────────────────────────────┘
```

#### Option 2: Multi-Step Form (Alternative)

**Step 1: Basic Information**

- Tenant ID
- Tenant Name

**Step 2: Contact Information**

- Email
- Phone
- Address

**Step 3: Keycloak Integration**

- Information about single realm strategy
- Tenant group will be created on activation

**Step 4: Review & Submit**

- Summary of all information
- Submit button

### Form Fields

| Field       | Type     | Required | Validation                | Max Length |
|-------------|----------|----------|---------------------------|------------|
| Tenant ID   | Text     | Yes      | Alphanumeric, unique      | 50         |
| Tenant Name | Text     | Yes      | Non-empty                 | 200        |
| Email       | Email    | No       | Valid emailAddress format | 255        |
| Phone       | Text     | No       | Phone format              | 50         |
| Address     | Textarea | No       | -                         | 500        |

**Note:** Keycloak integration uses Single Realm strategy. Tenant group will be created in `wms-realm` on tenant activation.

### Validation Rules

#### Client-Side Validation

```typescript
const validationSchema = {
  tenantId: {
    required: 'Tenant ID is required',
    maxLength: { value: 50, message: 'Tenant ID cannot exceed 50 characters' },
    pattern: {
      value: /^[a-zA-Z0-9-_]+$/,
      message: 'Tenant ID must be alphanumeric with hyphens or underscores only'
    }
  },
  name: {
    required: 'Tenant name is required',
    maxLength: { value: 200, message: 'Tenant name cannot exceed 200 characters' }
  },
  emailAddress: {
    pattern: {
      value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
      message: 'Invalid emailAddress format'
    }
  },
  phone: {
    maxLength: { value: 50, message: 'Phone cannot exceed 50 characters' }
  },
  address: {
    maxLength: { value: 500, message: 'Address cannot exceed 500 characters' }
  },
};
```

### UI Components

#### 1. TenantForm Component

```typescript
interface TenantFormProps {
  onSubmit: (data: CreateTenantRequest) => Promise<void>;
  onCancel: () => void;
  isLoading?: boolean;
}

const TenantForm: React.FC<TenantFormProps> = ({ onSubmit, onCancel, isLoading }) => {
  // Form implementation using React Hook Form
  // Material-UI components
  // Real-time validation
};
```

#### 2. Form Sections

- `BasicInformationSection` - Tenant ID and name
- `ContactInformationSection` - Email, phone, address
- `KeycloakConfigurationSection` - Realm settings
- `FormActions` - Cancel and Submit buttons

### Loading States

- **Form Loading:** Disable all inputs during submission
- **Submit Button:** Show loading spinner, disable button
- **Success:** Show success notification, redirect after 2 seconds
- **Error:** Show error notification, keep form editable

### Success Flow

1. Show success notification: "Tenant created successfully"
2. Redirect to tenant detail page: `/admin/tenants/{tenantId}`
3. Optionally show toast notification

### Error Handling

1. **Validation Errors:** Show inline errors below fields
2. **Server Errors:** Show error notification at top of form
3. **Network Errors:** Show retry option
4. **Duplicate Tenant ID:** Highlight tenant ID field with specific error

---

## Frontend Implementation

### Component Structure

```
features/tenant-management/
├── components/
│   ├── TenantForm.tsx
│   ├── BasicInformationSection.tsx
│   ├── ContactInformationSection.tsx
│   └── KeycloakConfigurationSection.tsx
├── pages/
│   └── TenantCreatePage.tsx
├── hooks/
│   └── useCreateTenant.ts
└── services/
    └── tenantService.ts
```

### TypeScript Types

```typescript
// types/tenant.ts
export interface CreateTenantRequest {
  tenantId: string;
  name: string;
  emailAddress?: string;
  phone?: string;
  address?: string;
}

// Note: Keycloak integration uses Single Realm strategy
// Tenant group will be created in wms-realm on activation

export interface CreateTenantResponse {
  tenantId: string;
  success: boolean;
  message: string;
}
```

### Custom Hook: useCreateTenant

```typescript
// hooks/useCreateTenant.ts
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { tenantService } from '../services/tenantService';
import { CreateTenantRequest } from '../types/tenant';
import { useSnackbar } from 'notistack'; // or custom notification hook

export const useCreateTenant = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const createTenant = async (request: CreateTenantRequest) => {
    setIsLoading(true);
    setError(null);
    
    try {
      const response = await tenantService.createTenant(request);
      
      if (response.error) {
        throw new Error(response.error.message);
      }
      
      // Success
      enqueueSnackbar('Tenant created successfully', { variant: 'success' });
      
      // Redirect to tenant detail page
      setTimeout(() => {
        navigate(`/admin/tenants/${response.data.tenantId}`);
      }, 1500);
      
      return response.data;
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to create tenant');
      setError(error);
      enqueueSnackbar(error.message, { variant: 'error' });
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { createTenant, isLoading, error };
};
```

### TenantCreatePage Component

```typescript
// pages/TenantCreatePage.tsx
import React from 'react';
import { Container, Paper, Typography, Box } from '@mui/material';
import { TenantForm } from '../components/TenantForm';
import { useCreateTenant } from '../hooks/useCreateTenant';
import { CreateTenantRequest } from '../types/tenant';
import { useNavigate } from 'react-router-dom';

export const TenantCreatePage: React.FC = () => {
  const { createTenant, isLoading } = useCreateTenant();
  const navigate = useNavigate();

  const handleSubmit = async (data: CreateTenantRequest) => {
    await createTenant(data);
  };

  const handleCancel = () => {
    navigate('/admin/tenants');
  };

  return (
    <Container maxWidth="md" sx={{ mt: 4, mb: 4 }}>
      <Paper elevation={3} sx={{ p: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Create Tenant (LDP)
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 4 }}>
          Create a new Local Distribution Partner tenant in the system.
        </Typography>
        
        <TenantForm
          onSubmit={handleSubmit}
          onCancel={handleCancel}
          isLoading={isLoading}
        />
      </Paper>
    </Container>
  );
};
```

### TenantForm Component (Simplified)

```typescript
// components/TenantForm.tsx
import React from 'react';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import {
  Box,
  TextField,
  Button,
  Checkbox,
  FormControlLabel,
  Grid,
  CircularProgress,
} from '@mui/material';
import { CreateTenantRequest } from '../types/tenant';

const validationSchema = yup.object({
  tenantId: yup
    .string()
    .required('Tenant ID is required')
    .max(50, 'Tenant ID cannot exceed 50 characters')
    .matches(/^[a-zA-Z0-9-_]+$/, 'Tenant ID must be alphanumeric with hyphens or underscores only'),
  name: yup
    .string()
    .required('Tenant name is required')
    .max(200, 'Tenant name cannot exceed 200 characters'),
  emailAddress: yup
    .string()
    .emailAddress('Invalid emailAddress format')
    .max(255, 'Email cannot exceed 255 characters'),
  phone: yup
    .string()
    .max(50, 'Phone cannot exceed 50 characters'),
  address: yup
    .string()
    .max(500, 'Address cannot exceed 500 characters'),
});

interface TenantFormProps {
  onSubmit: (data: CreateTenantRequest) => Promise<void>;
  onCancel: () => void;
  isLoading?: boolean;
}

export const TenantForm: React.FC<TenantFormProps> = ({
  onSubmit,
  onCancel,
  isLoading = false,
}) => {
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<CreateTenantRequest>({
    resolver: yupResolver(validationSchema),
    defaultValues: {},
  });


  const onFormSubmit = async (data: CreateTenantRequest) => {
    await onSubmit(data);
  };

  return (
    <form onSubmit={handleSubmit(onFormSubmit)}>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        {/* Basic Information Section */}
        <Box>
          <Typography variant="h6" gutterBottom>
            Basic Information
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <TextField
                {...register('tenantId')}
                label="Tenant ID"
                required
                fullWidth
                error={!!errors.tenantId}
                helperText={errors.tenantId?.message}
                disabled={isLoading}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                {...register('name')}
                label="Tenant Name"
                required
                fullWidth
                error={!!errors.name}
                helperText={errors.name?.message}
                disabled={isLoading}
              />
            </Grid>
          </Grid>
        </Box>

        {/* Contact Information Section */}
        <Box>
          <Typography variant="h6" gutterBottom>
            Contact Information
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <TextField
                {...register('emailAddress')}
                label="Email"
                type="emailAddress"
                fullWidth
                error={!!errors.emailAddress}
                helperText={errors.emailAddress?.message}
                disabled={isLoading}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                {...register('phone')}
                label="Phone"
                fullWidth
                error={!!errors.phone}
                helperText={errors.phone?.message}
                disabled={isLoading}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                {...register('address')}
                label="Address"
                multiline
                rows={3}
                fullWidth
                error={!!errors.address}
                helperText={errors.address?.message}
                disabled={isLoading}
              />
            </Grid>
          </Grid>
        </Box>

        {/* Keycloak Integration Information */}
        <Box>
          <Typography variant="h6" gutterBottom>
            Keycloak Integration
          </Typography>
          <Box sx={{ p: 2, bgcolor: 'info.light', borderRadius: 1 }}>
            <Typography variant="body2" color="text.secondary">
              <strong>Single Realm Strategy:</strong> This tenant will use the shared <code>wms-realm</code> Keycloak realm.
              A tenant group will be created on activation with the name <code>tenant-{'{tenantId}'}</code> for user organization.
            </Typography>
          </Box>
        </Box>

        {/* Form Actions */}
        <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 2, mt: 2 }}>
          <Button
            variant="outlined"
            onClick={onCancel}
            disabled={isLoading}
          >
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={isLoading}
            startIcon={isLoading ? <CircularProgress size={20} /> : null}
          >
            {isLoading ? 'Creating...' : 'Create Tenant'}
          </Button>
        </Box>
      </Box>
    </form>
  );
};
```

---

## Backend Data Flow

### API Endpoint

**POST** `/api/v1/tenants`

**Request Body:**

```json
{
  "tenantId": "ldp-001",
  "name": "Local Distribution Partner 001",
  "emailAddress": "contact@ldp001.com",
  "phone": "+27123456789",
  "address": "123 Main Street, Johannesburg"
}
```

**Response:** `201 Created`

```json
{
  "data": {
    "tenantId": "ldp-001",
    "success": true,
    "message": "Tenant created successfully"
  }
}
```

### Request Flow

```
1. Frontend: POST /api/v1/tenants
   ↓
2. API Gateway: Route to tenant-service
   ↓
3. TenantCommandController.createTenant(request)
   ↓
4. TenantMapper.toCreateTenantCommand(request)
   ↓
5. CreateTenantCommandHandler.handle(command)
   ↓
6. TenantRepository.findById(tenantId) - Check uniqueness
   ↓
7. Tenant.builder()
      .tenantId(TenantId.of(command.getTenantId()))
      .name(TenantName.of(command.getName()))
      .contactInformation(ContactInformation.of(...))
      .configuration(TenantConfiguration.of(...))
      .status(TenantStatus.PENDING)
      .build()
   ↓
8. TenantRepository.save(tenant) - Persist aggregate
   ↓
9. TenantEventPublisher.publish(tenant.getDomainEvents())
   ↓
10. Kafka: TenantCreatedEvent published
    ↓
11. Response: CreateTenantResponse
```

### Command Handler Implementation

```java
// CreateTenantCommandHandler.java
@Component
@Transactional
public class CreateTenantCommandHandler {
    private final TenantRepository tenantRepository;
    private final TenantEventPublisher eventPublisher;
    private final KeycloakGroupPort keycloakGroupPort;

    public CreateTenantResult handle(CreateTenantCommand command) {
        // Check uniqueness
        TenantId tenantId = TenantId.of(command.getTenantId());
        if (tenantRepository.findById(tenantId).isPresent()) {
            throw new DuplicateTenantException("Tenant with ID already exists: " + tenantId.getValue());
        }

        // Build domain entity
        Tenant tenant = Tenant.builder()
            .tenantId(tenantId)
            .name(TenantName.of(command.getName()))
            .contactInformation(ContactInformation.of(
                command.getEmail(),
                command.getPhone(),
                command.getAddress()
            ))
            .configuration(TenantConfiguration.defaultConfiguration())
            .status(TenantStatus.PENDING)
            .build();

        // Persist
        tenantRepository.save(tenant);

        // Keycloak integration: Optionally create tenant group (can defer to activation)
        // Group name: tenant-{tenantId}
        keycloakGroupPort.createTenantGroup(tenantId.getValue());

        // Publish events
        eventPublisher.publish(tenant.getDomainEvents());
        tenant.clearDomainEvents();

        return new CreateTenantResult(tenantId.getValue(), true, "Tenant created successfully");
    }
}
```

---

## Domain Modeling

### Domain Entity: Tenant

The `Tenant` aggregate root is already implemented in the domain core. Key aspects:

1. **Builder Pattern** - Uses builder for construction
2. **Business Rules** - Validates tenant ID uniqueness, required fields
3. **Status Management** - Initial status is PENDING
4. **Domain Events** - Publishes `TenantCreatedEvent` on creation

### Value Objects

1. **TenantId** - From common-domain
2. **TenantName** - Validates non-empty, max length
3. **ContactInformation** - Email, phone, address (all optional)
4. **TenantConfiguration** - Tenant-specific settings (Keycloak uses single realm strategy)
5. **TenantStatus** - Enum: PENDING, ACTIVE, INACTIVE, SUSPENDED

### Domain Event: TenantCreatedEvent

```java
public class TenantCreatedEvent extends TenantEvent<TenantId> {
    private final TenantName name;
    private final TenantStatus status;

    public TenantCreatedEvent(TenantId tenantId, TenantName name, TenantStatus status) {
        super(tenantId);
        this.name = name;
        this.status = status;
    }

    // Getters...
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
- **Business rule validation** in command handler
- **Uniqueness check** for tenant ID
- **Domain validation** in aggregate builder

### Validation Layers

1. **Frontend (React Hook Form + Yup)**
    - Format validation
    - Required field validation
    - Length validation

2. **Backend DTO (Jakarta Bean Validation)**
    - Re-validate all constraints
    - Format validation
    - Length validation

3. **Domain Core (Business Rules)**
    - Uniqueness validation
    - Business rule enforcement
    - Invariant checking

---

## Error Handling

### Frontend Error Handling

1. **Validation Errors**
    - Display inline below fields
    - Highlight field in red
    - Show specific error message

2. **Server Errors**
    - Display error notification
    - Show error message from API
    - Keep form editable for correction

3. **Network Errors**
    - Show retry option
    - Display network error message
    - Allow form resubmission

### Backend Error Handling

1. **Validation Errors** - `400 Bad Request` with validation details
2. **Duplicate Tenant** - `409 Conflict` with specific error
3. **Server Errors** - `500 Internal Server Error` with generic message

### Error Response Format

```json
{
  "error": {
    "code": "DUPLICATE_TENANT",
    "message": "Tenant with ID 'ldp-001' already exists",
    "timestamp": "2025-01-15T10:30:00Z",
    "path": "/api/v1/tenants",
    "requestId": "req-123"
  }
}
```

---

## Testing Plan

### Frontend Tests

1. **Unit Tests**
    - TenantForm component rendering
    - Form validation logic
    - useCreateTenant hook behavior

2. **Integration Tests**
    - Form submission flow
    - API integration
    - Error handling

3. **E2E Tests**
    - Complete tenant creation flow
    - Success redirect
    - Error scenarios

### Backend Tests

1. **Domain Tests**
    - Tenant entity creation
    - Business rule validation
    - Event publishing

2. **Integration Tests**
    - API endpoint
    - Command handler
    - Repository operations

---

## References

- [Tenant Management Implementation Plan](01-Tenant_Management_Implementation_Plan.md)
- [Service Architecture Document](../../01-architecture/Service_Architecture_Document.md)
- [Frontend Architecture Document](../../01-architecture/Frontend_Architecture_Document.md)
- [API Specifications](../../02-api/API_Specifications.md)

---

**Document Status:** Draft  
**Last Updated:** 2025-01  
**Next Review:** 2025-02

