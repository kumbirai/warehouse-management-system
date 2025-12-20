# Mandated Frontend Templates

## Warehouse Management System - CCBSA LDP System

**Document Version:** 2.0
**Date:** 2025-01
**Status:** Draft

---

## Table of Contents

1. [Overview](#overview)
2. [Package Structure](#package-structure)
3. [Core Principles](#core-principles)
4. [Shared Components Library](#shared-components-library)
5. [Page Layout Templates](#page-layout-templates)
6. [Form Patterns](#form-patterns)
7. [List Page Patterns](#list-page-patterns)
8. [Detail Page Patterns](#detail-page-patterns)
9. [Navigation and Routing](#navigation-and-routing)
10. [Mobile Responsiveness](#mobile-responsiveness)
11. [State Management](#state-management)
12. [API Integration](#api-integration)
13. [Error Handling](#error-handling)
14. [Validation Patterns](#validation-patterns)
15. [Testing Patterns](#testing-patterns)
16. [Accessibility Guidelines](#accessibility-guidelines)

---

## Overview

This document defines the mandatory patterns, components, and templates for the **Frontend** application (`frontend-app`). All features must follow these patterns to ensure:

- **Consistency**: Uniform user experience across all features
- **Maintainability**: Reusable components reduce code duplication
- **Mobile-First**: Responsive design that works on all devices
- **CQRS Compliance**: Clear separation between commands and queries
- **Type Safety**: Full TypeScript coverage with strict typing
- **Accessibility**: WCAG 2.1 AA compliance

---

## Package Structure

The Frontend application follows a strict feature-based package structure:

\`\`\`
frontend-app/
├── src/
│   ├── components/
│   │   ├── common/                     # Shared UI components
│   │   │   ├── PageBreadcrumbs.tsx
│   │   │   ├── PageHeader.tsx
│   │   │   ├── LoadingSpinner.tsx
│   │   │   ├── EmptyState.tsx
│   │   │   ├── ActionDialog.tsx
│   │   │   ├── FilterBar.tsx
│   │   │   ├── StatusBadge.tsx
│   │   │   ├── FormActions.tsx
│   │   │   ├── ResponsiveTable.tsx
│   │   │   ├── Pagination.tsx
│   │   │   └── index.ts
│   │   ├── layouts/                    # Page layout components
│   │   │   ├── DetailPageLayout.tsx
│   │   │   ├── FormPageLayout.tsx
│   │   │   ├── ListPageLayout.tsx
│   │   │   └── index.ts
│   │   ├── layout/                     # App-level layout
│   │   │   └── Header.tsx
│   │   └── auth/                       # Authentication components
│   │       └── ProtectedRoute.tsx
│   ├── features/                       # Feature modules
│   │   └── {feature-name}/
│   │       ├── components/             # Feature-specific components
│   │       │   ├── {Feature}List.tsx
│   │       │   ├── {Feature}Detail.tsx
│   │       │   ├── {Feature}Form.tsx
│   │       │   └── {Feature}Actions.tsx
│   │       ├── pages/                  # Page components
│   │       │   ├── {Feature}ListPage.tsx
│   │       │   ├── {Feature}DetailPage.tsx
│   │       │   ├── {Feature}CreatePage.tsx
│   │       │   └── {Feature}EditPage.tsx
│   │       ├── hooks/                  # Feature-specific hooks
│   │       │   ├── use{Features}.ts
│   │       │   ├── use{Feature}.ts
│   │       │   └── useCreate{Feature}.ts
│   │       ├── services/               # API service clients
│   │       │   └── {feature}Service.ts
│   │       └── types/                  # TypeScript types
│   │           └── {feature}.ts
│   ├── services/                       # Shared services
│   │   ├── apiClient.ts                # Base API client
│   │   └── correlationIdService.ts     # Correlation ID service
│   ├── utils/                          # Utility functions
│   │   ├── dateUtils.ts
│   │   ├── validationUtils.ts
│   │   ├── navigationUtils.ts
│   │   ├── logger.ts
│   │   └── theme.ts
│   ├── hooks/                          # Shared custom hooks
│   │   └── useTenant.ts
│   ├── store/                          # Global state (Redux/Zustand)
│   │   └── index.ts
│   └── App.tsx
└── public/
\`\`\`

### Package Naming Conventions

| Type | Convention | Example |
|------|-----------|---------|
| Feature directory | kebab-case | `tenant-management`, `location-management` |
| Component files | PascalCase.tsx | `TenantList.tsx`, `LocationForm.tsx` |
| Page files | PascalCase + Page.tsx | `TenantListPage.tsx` |
| Hook files | camelCase + use prefix | `useTenants.ts`, `useCreateTenant.ts` |
| Service files | camelCase + Service | `tenantService.ts` |
| Type files | kebab-case | `tenant.ts`, `location.ts` |
| Utility files | camelCase | `dateUtils.ts`, `validationUtils.ts` |

---

## Core Principles

### Domain-Driven Design (DDD)

- Features organized by domain bounded contexts
- Components reflect domain concepts and ubiquitous language
- Domain types match backend domain model exactly
- Avoid technical naming (use domain terms)

**Example:**
```typescript
// ❌ Bad - technical naming
interface DataDTO { id: string; name: string; }

// ✅ Good - domain naming
interface Tenant { tenantId: string; tenantName: string; }
```

### Clean Architecture

```
┌─────────────────────────────────────┐
│   Presentation Layer (Components)   │
├─────────────────────────────────────┤
│   Application Layer (Hooks/State)   │
├─────────────────────────────────────┤
│   Adapter Layer (Services/API)      │
├─────────────────────────────────────┤
│   Domain Layer (Types/Validation)   │
└─────────────────────────────────────┘
```

- **Presentation**: React components (pages, forms, lists)
- **Application**: Custom hooks, state management
- **Adapters**: API clients, service adapters
- **Domain**: TypeScript types, validation schemas

### CQRS Principles

**Command-Query Separation:**
- Commands modify state (POST, PUT, PATCH, DELETE)
- Queries read state (GET)
- Different endpoints and DTOs for commands vs queries

**Frontend Implementation:**
- Forms submit commands
- Lists/Details display query results
- Separate hooks for commands and queries

**Example:**
```typescript
// Query hook
const { location, isLoading } = useLocation(locationId);

// Command hook
const { createLocation, isCreating } = useCreateLocation();
```

### Event-Driven Architecture

- API client injects correlation ID for request tracing
- Correlation ID persists across session for log correlation
- WebSocket connections for real-time updates (future)
- Optimistic UI updates with server confirmation

---

## Shared Components Library

All shared components are located in `src/components/common/` and must be used consistently across features.

### PageBreadcrumbs

Renders breadcrumb navigation with proper routing.

**Usage:**
\`\`\`typescript
import { PageBreadcrumbs } from '@/components/common';

<PageBreadcrumbs
  items={[
    { label: 'Dashboard', href: '/dashboard' },
    { label: 'Tenants', href: '/admin/tenants' },
    { label: tenantName }, // Last item has no href
  ]}
/>
\`\`\`

**Props:**
- `items: BreadcrumbItem[]` - Array of breadcrumb items
  - `label: string` - Display text
  - `href?: string` - Optional link (omit for last/current item)

**Standard Spacing:** `sx={{ mb: 3 }}`

---

### PageHeader

Renders page title, description, and action buttons.

**Usage:**
\`\`\`typescript
import { PageHeader } from '@/components/common';
import { Button } from '@mui/material';
import { Add as AddIcon } from '@mui/icons-material';

<PageHeader
  title="Tenants"
  description="Manage tenant organizations and configurations"
  actions={
    <Button variant="contained" startIcon={<AddIcon />} onClick={handleCreate}>
      Create Tenant
    </Button>
  }
/>
\`\`\`

**Props:**
- `title: string` - Page title (h4 variant)
- `description?: string` - Optional description (body1, text.secondary)
- `actions?: ReactNode` - Optional action buttons (right-aligned on desktop, stacked on mobile)

**Standard Spacing:** `sx={{ mb: 3 }}`

---

### LoadingSpinner

Centered loading indicator with consistent styling.

**Usage:**
\`\`\`typescript
import { LoadingSpinner } from '@/components/common';

{isLoading && <LoadingSpinner />}
\`\`\`

**Props:**
- `size?: number` - Spinner size in pixels (default: 40)
- `minHeight?: string | number` - Minimum container height (default: '400px')

---

### EmptyState

Displays when no data is available.

**Usage:**
\`\`\`typescript
import { EmptyState } from '@/components/common';
import { Inbox as InboxIcon } from '@mui/icons-material';

{locations.length === 0 && (
  <EmptyState
    title="No locations found"
    description="Get started by creating your first location"
    icon={<InboxIcon sx={{ fontSize: 64 }} />}
    action={{
      label: 'Create Location',
      onClick: handleCreate,
    }}
  />
)}
\`\`\`

**Props:**
- `title: string` - Main message
- `description?: string` - Optional secondary text
- `icon?: ReactNode` - Optional icon (default: InboxIcon)
- `action?: { label: string; onClick: () => void }` - Optional CTA button

---

### ActionDialog

Confirmation dialog for actions.

**Usage:**
\`\`\`typescript
import { ActionDialog } from '@/components/common';

const [isOpen, setIsOpen] = useState(false);

<ActionDialog
  open={isOpen}
  title="Deactivate Tenant"
  description="Are you sure you want to deactivate this tenant? Users will lose access immediately."
  confirmLabel="Deactivate"
  cancelLabel="Cancel"
  onConfirm={handleDeactivate}
  onCancel={() => setIsOpen(false)}
  isLoading={isDeactivating}
  variant="danger"
/>
\`\`\`

**Props:**
- `open: boolean` - Dialog visibility
- `title: string` - Dialog title
- `description: string | ReactNode` - Dialog content
- `confirmLabel?: string` - Confirm button text (default: "Confirm")
- `cancelLabel?: string` - Cancel button text (default: "Cancel")
- `onConfirm: () => void | Promise<void>` - Confirm handler
- `onCancel: () => void` - Cancel handler
- `isLoading?: boolean` - Loading state (default: false)
- `variant?: 'default' | 'danger'` - Button color variant (default: 'default')

---

### FilterBar

Container for filter controls with clear button.

**Usage:**
\`\`\`typescript
import { FilterBar } from '@/components/common';
import { TextField, Select, MenuItem } from '@mui/material';

<FilterBar
  onClearFilters={handleClearFilters}
  hasActiveFilters={!!searchQuery || status !== 'all'}
>
  <TextField
    label="Search"
    value={searchQuery}
    onChange={handleSearchChange}
    fullWidth
  />
  <Select value={status} onChange={handleStatusChange} sx={{ minWidth: 180 }}>
    <MenuItem value="all">All Statuses</MenuItem>
    <MenuItem value="active">Active</MenuItem>
    <MenuItem value="inactive">Inactive</MenuItem>
  </Select>
</FilterBar>
\`\`\`

**Props:**
- `children: ReactNode` - Filter controls
- `onClearFilters?: () => void` - Clear filters handler
- `hasActiveFilters?: boolean` - Enable/disable clear button (default: false)

---

### StatusBadge

Displays status with color-coded chip.

**Usage:**
\`\`\`typescript
import { StatusBadge, getStatusVariant } from '@/components/common';

<StatusBadge
  label={location.status}
  variant={getStatusVariant(location.status)}
/>
\`\`\`

**Props:**
- `label: string` - Status text
- `variant?: StatusVariant` - Color variant (default: 'default')
  - `'success'` - Green (active, available, confirmed)
  - `'warning'` - Orange (pending, reserved, in_progress)
  - `'error'` - Red (inactive, suspended, unavailable)
  - `'info'` - Blue (informational)
  - `'default'` - Gray (unknown)
- `size?: 'small' | 'medium'` - Chip size (default: 'small')

**Helper Function:**
\`\`\`typescript
getStatusVariant(status: string): StatusVariant
\`\`\`
Automatically maps status strings to color variants.

---

### FormActions

Standardized form button group (Cancel + Submit).

**Usage:**
\`\`\`typescript
import { FormActions } from '@/components/common';

<form onSubmit={handleSubmit(onSubmit)}>
  {/* Form fields */}

  <FormActions
    onCancel={handleCancel}
    isSubmitting={isSubmitting}
    submitLabel="Create Tenant"
    cancelLabel="Cancel"
  />
</form>
\`\`\`

**Props:**
- `onCancel: () => void` - Cancel button handler
- `onSubmit?: () => void` - Optional submit handler (default: form submit)
- `isSubmitting: boolean` - Loading state
- `submitLabel?: string` - Submit button text (default: 'Submit')
- `cancelLabel?: string` - Cancel button text (default: 'Cancel')
- `submitDisabled?: boolean` - Additional disable condition (default: false)

**Standard Layout:**
- Right-aligned button group
- Cancel button: `variant="outlined"`, disabled during submission
- Submit button: `variant="contained"`, shows "{label}..." during submission
- `mt: 3` spacing

---

### ResponsiveTable

Table that converts to cards on mobile.

**Usage:**
\`\`\`typescript
import { ResponsiveTable, Column } from '@/components/common';

const columns: Column<Location>[] = [
  {
    key: 'code',
    label: 'Code',
    render: (location) => <Typography>{location.code}</Typography>,
  },
  {
    key: 'status',
    label: 'Status',
    render: (location) => (
      <StatusBadge
        label={location.status}
        variant={getStatusVariant(location.status)}
      />
    ),
  },
  {
    key: 'actions',
    label: 'Actions',
    hideOnMobile: true,
    render: (location) => (
      <Button size="small" onClick={() => navigate(\`/locations/\${location.id}\`)}>
        View
      </Button>
    ),
  },
];

<ResponsiveTable
  data={locations}
  columns={columns}
  getRowKey={(location) => location.id}
  onRowClick={handleRowClick}
  emptyMessage="No locations found"
/>
\`\`\`

**Props:**
- `data: T[]` - Array of items to display
- `columns: Column<T>[]` - Column definitions
  - `key: string` - Unique column key
  - `label: string` - Column header text
  - `render: (item: T) => ReactNode` - Cell renderer
  - `hideOnMobile?: boolean` - Hide column on mobile (default: false)
- `getRowKey: (item: T) => string | number` - Row key function
- `onRowClick?: (item: T) => void` - Optional row click handler
- `emptyMessage?: string` - Empty state message (default: 'No items found')
- `mobileCardRender?: (item: T) => ReactNode` - Custom mobile card renderer

**Behavior:**
- **Desktop (≥md):** Renders as table
- **Mobile (<md):** Renders as stacked cards
- Columns with `hideOnMobile: true` are hidden on mobile
- Custom `mobileCardRender` overrides default card layout

---

### Pagination

Pagination controls with item count.

**Usage:**
\`\`\`typescript
import { Pagination } from '@/components/common';

<Pagination
  currentPage={page}
  totalPages={Math.ceil(totalItems / itemsPerPage)}
  totalItems={totalItems}
  itemsPerPage={itemsPerPage}
  onPageChange={setPage}
/>
\`\`\`

**Props:**
- `currentPage: number` - Current page (1-indexed)
- `totalPages: number` - Total number of pages
- `totalItems: number` - Total item count
- `itemsPerPage: number` - Items per page
- `onPageChange: (page: number) => void` - Page change handler

**Behavior:**
- Automatically hides if `totalPages <= 1`
- Shows "Showing X-Y of Z" text
- First/Last buttons enabled

---

## Page Layout Templates

All pages must use layout components from `src/components/layouts/`.

### DetailPageLayout

Standard layout for detail/view pages.

**Features:**
- Header with navigation
- Breadcrumbs
- Page title with actions
- Loading state
- Error alert
- Content area

**Usage:**
\`\`\`typescript
import { DetailPageLayout } from '@/components/layouts';
import { getBreadcrumbs, Routes } from '@/utils/navigationUtils';

export const TenantDetailPage = () => {
  const { id } = useParams();
  const { tenant, isLoading, error } = useTenant(id);
  const navigate = useNavigate();

  return (
    <DetailPageLayout
      breadcrumbs={getBreadcrumbs.tenantDetail(tenant?.tenantName || '...')}
      title={tenant?.tenantName || 'Loading...'}
      actions={
        <Button variant="outlined" onClick={() => navigate(Routes.admin.tenants)}>
          Back to List
        </Button>
      }
      isLoading={isLoading}
      error={error}
      maxWidth="lg"
    >
      <TenantDetail tenant={tenant} />
    </DetailPageLayout>
  );
};
\`\`\`

**Props:**
- `breadcrumbs: BreadcrumbItem[]` - Breadcrumb items
- `title: string` - Page title
- `actions?: ReactNode` - Optional action buttons
- `isLoading: boolean` - Loading state
- `error: string | null` - Error message
- `children: ReactNode` - Page content (rendered only when not loading/error)
- `maxWidth?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'` - Container max width (default: 'lg')

---

### FormPageLayout

Standard layout for create/edit form pages.

**Features:**
- Header with navigation
- Breadcrumbs
- Page title and description
- Error alert
- Form content area

**Usage:**
\`\`\`typescript
import { FormPageLayout } from '@/components/layouts';
import { getBreadcrumbs } from '@/utils/navigationUtils';

export const TenantCreatePage = () => {
  const { createTenant, isCreating, error } = useCreateTenant();
  const navigate = useNavigate();

  const handleSubmit = async (data: CreateTenantRequest) => {
    const tenant = await createTenant(data);
    navigate(\`/admin/tenants/\${tenant.tenantId}\`);
  };

  return (
    <FormPageLayout
      breadcrumbs={getBreadcrumbs.tenantCreate()}
      title="Create Tenant"
      description="Create a new tenant organization with its own isolated environment"
      error={error}
      maxWidth="md"
    >
      <TenantForm
        onSubmit={handleSubmit}
        onCancel={() => navigate('/admin/tenants')}
        isSubmitting={isCreating}
      />
    </FormPageLayout>
  );
};
\`\`\`

**Props:**
- `breadcrumbs: BreadcrumbItem[]` - Breadcrumb items
- `title: string` - Page title
- `description?: string` - Optional page description
- `error: string | null` - Error message
- `children: ReactNode` - Form component
- `maxWidth?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'` - Container max width (default: 'md')

---

### ListPageLayout

Standard layout for list/index pages.

**Features:**
- Header with navigation
- Breadcrumbs
- Page title, description, and action buttons
- Error alert
- Loading state
- List content area

**Usage:**
\`\`\`typescript
import { ListPageLayout } from '@/components/layouts';
import { getBreadcrumbs } from '@/utils/navigationUtils';

export const TenantListPage = () => {
  const { tenants, isLoading, error, filters, updateFilters } = useTenants();
  const navigate = useNavigate();

  return (
    <ListPageLayout
      breadcrumbs={getBreadcrumbs.tenantList()}
      title="Tenants"
      description="Manage tenant organizations"
      actions={
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => navigate('/admin/tenants/create')}
        >
          Create Tenant
        </Button>
      }
      isLoading={isLoading}
      error={error}
      maxWidth="lg"
    >
      <FilterBar {...filters} />
      <TenantList tenants={tenants} />
    </ListPageLayout>
  );
};
\`\`\`

**Props:**
- `breadcrumbs: BreadcrumbItem[]` - Breadcrumb items
- `title: string` - Page title
- `description?: string` - Optional page description
- `actions?: ReactNode` - Optional action buttons (e.g., Create button)
- `isLoading: boolean` - Loading state
- `error: string | null` - Error message
- `children: ReactNode` - List content (rendered only when not loading)
- `maxWidth?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'` - Container max width (default: 'lg')

---

## Form Patterns

All forms must follow these patterns for consistency and validation.

### Form Structure

**Standard Form Template:**

```typescript
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { FormActions } from '@/components/common';
import { TextField, Grid, Paper } from '@mui/material';

// 1. Define validation schema
const schema = z.object({
  code: z.string().min(1, 'Code is required'),
  name: z.string().min(1, 'Name is required'),
  // ... other fields
});

type FormValues = z.infer<typeof schema>;

interface EntityFormProps {
  initialValues?: Partial<FormValues>;
  onSubmit: (data: FormValues) => void;
  onCancel: () => void;
  isSubmitting: boolean;
}

export const EntityForm: React.FC<EntityFormProps> = ({
  initialValues,
  onSubmit,
  onCancel,
  isSubmitting,
}) => {
  // 2. Initialize form
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: initialValues || {
      code: '',
      name: '',
    },
  });

  // 3. Render form
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <Paper sx={{ p: 3 }}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <TextField
              {...register('code')}
              label="Code"
              fullWidth
              required
              error={!!errors.code}
              helperText={errors.code?.message}
              disabled={isSubmitting}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              {...register('name')}
              label="Name"
              fullWidth
              required
              error={!!errors.name}
              helperText={errors.name?.message}
              disabled={isSubmitting}
            />
          </Grid>
        </Grid>

        <FormActions
          onCancel={onCancel}
          isSubmitting={isSubmitting}
          submitLabel="Create Entity"
        />
      </Paper>
    </form>
  );
};
```

### Validation with Zod

Use `validationUtils.ts` for common validation schemas:

```typescript
import { z } from 'zod';
import { CommonSchemas, ValidationMessages } from '@/utils/validationUtils';

const tenantSchema = z.object({
  tenantId: CommonSchemas.tenantId,
  tenantName: CommonSchemas.requiredString,
  contactEmail: CommonSchemas.email,
  phoneNumber: CommonSchemas.phoneOptional,
});
```

### Async Validation

For uniqueness checks or external validation:

```typescript
import { useCheckProductCodeUniqueness } from '../hooks/useCheckProductCodeUniqueness';

const ProductForm = ({ onSubmit, onCancel, isSubmitting }) => {
  const { register, handleSubmit, watch, formState: { errors } } = useForm();
  const productCode = watch('code');
  
  // Async validation hook
  const { isUnique, isChecking } = useCheckProductCodeUniqueness(productCode);

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <TextField
        {...register('code')}
        label="Product Code"
        error={!!errors.code || (productCode && !isChecking && !isUnique)}
        helperText={
          errors.code?.message ||
          (isChecking && 'Checking availability...') ||
          (!isUnique && 'Code already exists')
        }
      />
      {/* ... */}
      <FormActions
        onCancel={onCancel}
        isSubmitting={isSubmitting}
        submitDisabled={!isUnique}
      />
    </form>
  );
};
```

### Grid Layout Guidelines

- Use Material-UI Grid system
- Standard breakpoints:
  - `xs={12}` - Full width on mobile
  - `md={6}` - Two columns on desktop
  - `md={4}` - Three columns on desktop
- Section spacing: `spacing={3}`
- Paper padding: `sx={{ p: 3 }}`

### Form Field Standards

**Required Fields:**
```typescript
<TextField
  {...register('fieldName')}
  label="Field Label"
  required  // Shows asterisk
  fullWidth
  error={!!errors.fieldName}
  helperText={errors.fieldName?.message}
/>
```

**Optional Fields:**
```typescript
<TextField
  {...register('fieldName')}
  label="Field Label (Optional)"
  fullWidth
  error={!!errors.fieldName}
  helperText={errors.fieldName?.message}
/>
```

**Select Fields:**
```typescript
<TextField
  {...register('status')}
  select
  label="Status"
  fullWidth
  required
  error={!!errors.status}
  helperText={errors.status?.message}
>
  <MenuItem value="active">Active</MenuItem>
  <MenuItem value="inactive">Inactive</MenuItem>
</TextField>
```

**Number Fields:**
```typescript
<TextField
  {...register('quantity', { valueAsNumber: true })}
  type="number"
  label="Quantity"
  fullWidth
  required
  inputProps={{ min: 0, step: 1 }}
  error={!!errors.quantity}
  helperText={errors.quantity?.message}
/>
```

---

## List Page Patterns

All list pages follow the same structure for consistency.

### List Page Structure

**Template:**

```typescript
import { ListPageLayout } from '@/components/layouts';
import { FilterBar, ResponsiveTable, Pagination } from '@/components/common';
import { getBreadcrumbs, Routes } from '@/utils/navigationUtils';
import { Button } from '@mui/material';
import { Add as AddIcon } from '@mui/icons-material';

export const EntityListPage = () => {
  // 1. Hooks
  const navigate = useNavigate();
  const {
    entities,
    isLoading,
    error,
    filters,
    pagination,
    updatePage,
    updateSearch,
    updateStatus,
  } = useEntities({
    page: 1,
    pageSize: 20,
    search: '',
    status: 'all',
  });

  // 2. Handlers
  const handleClearFilters = () => {
    updateSearch('');
    updateStatus('all');
  };

  const hasActiveFilters = filters.search || filters.status !== 'all';

  const columns = [
    {
      key: 'code',
      label: 'Code',
      render: (entity) => <Typography>{entity.code}</Typography>,
    },
    {
      key: 'name',
      label: 'Name',
      render: (entity) => <Typography>{entity.name}</Typography>,
    },
    {
      key: 'status',
      label: 'Status',
      render: (entity) => (
        <StatusBadge
          label={entity.status}
          variant={getStatusVariant(entity.status)}
        />
      ),
    },
    {
      key: 'actions',
      label: 'Actions',
      hideOnMobile: true,
      render: (entity) => (
        <Button
          size="small"
          onClick={() => navigate(`/entities/${entity.id}`)}
        >
          View
        </Button>
      ),
    },
  ];

  // 3. Render
  return (
    <ListPageLayout
      breadcrumbs={getBreadcrumbs.entityList()}
      title="Entities"
      description="Manage entities in the system"
      actions={
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => navigate('/entities/create')}
        >
          Create Entity
        </Button>
      }
      isLoading={isLoading}
      error={error}
    >
      <FilterBar onClearFilters={handleClearFilters} hasActiveFilters={hasActiveFilters}>
        <TextField
          label="Search"
          placeholder="Search by code or name..."
          value={filters.search}
          onChange={(e) => updateSearch(e.target.value)}
          fullWidth
        />
        <TextField
          select
          label="Status"
          value={filters.status}
          onChange={(e) => updateStatus(e.target.value)}
          sx={{ minWidth: 180 }}
        >
          <MenuItem value="all">All Statuses</MenuItem>
          <MenuItem value="active">Active</MenuItem>
          <MenuItem value="inactive">Inactive</MenuItem>
        </TextField>
      </FilterBar>

      {entities.length === 0 ? (
        <EmptyState
          title="No entities found"
          description="Create your first entity to get started"
          action={{
            label: 'Create Entity',
            onClick: () => navigate('/entities/create'),
          }}
        />
      ) : (
        <>
          <ResponsiveTable
            data={entities}
            columns={columns}
            getRowKey={(entity) => entity.id}
            onRowClick={(entity) => navigate(`/entities/${entity.id}`)}
          />

          {pagination && (
            <Pagination
              currentPage={pagination.currentPage}
              totalPages={pagination.totalPages}
              totalItems={pagination.totalItems}
              itemsPerPage={pagination.pageSize}
              onPageChange={updatePage}
            />
          )}
        </>
      )}
    </ListPageLayout>
  );
};
```

### Filter Patterns

**Standard Filters:**
- **Search:** Full-width text field, debounced (500ms)
- **Status:** Dropdown select, min-width 180px
- **Tenant:** (Admin only) Dropdown select for multi-tenant filtering
- **Date Range:** Two date pickers (From/To)

**Clear Filters:**
- Always include "Clear Filters" button
- Disable when no filters active
- Reset all filters to defaults

### Pagination Standards

- **Default Page Size:** 20 items per page
- **Page Size Options:** 10, 20, 50, 100
- Always show "Showing X-Y of Z"
- Hide pagination if total pages <= 1

### Empty States

- Show when `data.length === 0`
- Include descriptive message
- Include icon (relevant to feature)
- Include Create button if user has permission

---

## Detail Page Patterns

Detail pages display entity information with actions.

### Detail Page Structure

**Template:**

```typescript
import { DetailPageLayout } from '@/components/layouts';
import { getBreadcrumbs, Routes } from '@/utils/navigationUtils';
import { Button, Grid, Paper, Typography, Divider } from '@mui/material';
import { Edit as EditIcon, Delete as DeleteIcon } from '@mui/icons-material';

export const EntityDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { entity, isLoading, error, refetch } = useEntity(id);

  const actions = (
    <>
      <Button
        variant="outlined"
        onClick={() => navigate(Routes.entityList)}
      >
        Back to List
      </Button>
      <Button
        variant="outlined"
        startIcon={<EditIcon />}
        onClick={() => navigate(`/entities/${id}/edit`)}
      >
        Edit
      </Button>
    </>
  );

  return (
    <DetailPageLayout
      breadcrumbs={getBreadcrumbs.entityDetail(entity?.name || '...')}
      title={entity?.name || 'Loading...'}
      actions={actions}
      isLoading={isLoading}
      error={error}
    >
      <EntityDetail entity={entity} onActionCompleted={refetch} />
    </DetailPageLayout>
  );
};
```

### Detail Component Structure

**Card-Based Layout:**

```typescript
import { Grid, Paper, Typography, Divider, Stack } from '@mui/material';
import { formatDateTime } from '@/utils/dateUtils';
import { StatusBadge, getStatusVariant } from '@/components/common';

interface EntityDetailProps {
  entity: Entity;
  onActionCompleted?: () => void;
}

export const EntityDetail: React.FC<EntityDetailProps> = ({
  entity,
  onActionCompleted,
}) => {
  return (
    <Grid container spacing={3}>
      {/* Basic Information */}
      <Grid item xs={12} md={6}>
        <Paper elevation={1} sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Basic Information
          </Typography>
          <Divider sx={{ mb: 2 }} />

          <Stack spacing={2}>
            <Box>
              <Typography variant="caption" color="text.secondary">
                Code
              </Typography>
              <Typography variant="body1">{entity.code}</Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Name
              </Typography>
              <Typography variant="body1">{entity.name}</Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Status
              </Typography>
              <Box mt={0.5}>
                <StatusBadge
                  label={entity.status}
                  variant={getStatusVariant(entity.status)}
                />
              </Box>
            </Box>
          </Stack>
        </Paper>
      </Grid>

      {/* Additional Information */}
      <Grid item xs={12} md={6}>
        <Paper elevation={1} sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Additional Information
          </Typography>
          <Divider sx={{ mb: 2 }} />

          <Stack spacing={2}>
            <Box>
              <Typography variant="caption" color="text.secondary">
                Created At
              </Typography>
              <Typography variant="body1">
                {formatDateTime(entity.createdAt)}
              </Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Updated At
              </Typography>
              <Typography variant="body1">
                {formatDateTime(entity.updatedAt)}
              </Typography>
            </Box>
          </Stack>
        </Paper>
      </Grid>

      {/* Actions Section */}
      <Grid item xs={12}>
        <EntityActions entity={entity} onActionCompleted={onActionCompleted} />
      </Grid>
    </Grid>
  );
};
```

### Detail Field Patterns

**Standard Field Display:**
```typescript
<Box>
  <Typography variant="caption" color="text.secondary">
    Field Label
  </Typography>
  <Typography variant="body1">{value || '—'}</Typography>
</Box>
```

**Badge Field:**
```typescript
<Box>
  <Typography variant="caption" color="text.secondary">
    Status
  </Typography>
  <Box mt={0.5}>
    <StatusBadge label={status} variant={getStatusVariant(status)} />
  </Box>
</Box>
```

**Date Field:**
```typescript
<Box>
  <Typography variant="caption" color="text.secondary">
    Created At
  </Typography>
  <Typography variant="body1">{formatDateTime(createdAt)}</Typography>
</Box>
```

**List Field:**
```typescript
<Box>
  <Typography variant="caption" color="text.secondary">
    Tags
  </Typography>
  <Stack direction="row" spacing={1} mt={0.5}>
    {tags.map((tag) => (
      <Chip key={tag} label={tag} size="small" />
    ))}
  </Stack>
</Box>
```

---

## Navigation and Routing

Use `navigationUtils.ts` for consistent routing and breadcrumbs.

### Route Definitions

All routes are defined in `Routes` object:

```typescript
import { Routes, getBreadcrumbs } from '@/utils/navigationUtils';

// Usage in components
navigate(Routes.admin.tenants);
navigate(Routes.locationDetail(locationId));
navigate(Routes.productCreate);
```

### Breadcrumb Patterns

Always use `getBreadcrumbs` helper:

```typescript
// List page
breadcrumbs={getBreadcrumbs.tenantList()}

// Detail page
breadcrumbs={getBreadcrumbs.tenantDetail(tenant.tenantName)}

// Create page
breadcrumbs={getBreadcrumbs.tenantCreate()}
```

### Navigation Patterns

**Back to List:**
```typescript
<Button
  variant="outlined"
  onClick={() => navigate(Routes.admin.tenants)}
>
  Back to List
</Button>
```

**Cancel Navigation:**
```typescript
const handleCancel = () => {
  navigate(getListPageRoute(location.pathname));
};
```

**After Creation:**
```typescript
const handleSubmit = async (data) => {
  const entity = await createEntity(data);
  navigate(Routes.entityDetail(entity.id));
};
```

---

## Mobile Responsiveness

All components must be mobile-responsive following these patterns.

### Breakpoint Strategy

Material-UI breakpoints:
- `xs`: 0-600px (Mobile)
- `sm`: 600-960px (Tablet)
- `md`: 960-1280px (Desktop)
- `lg`: 1280-1920px (Large Desktop)
- `xl`: 1920px+ (Extra Large)

**Primary Breakpoint:** `md` (960px)
- Below `md`: Mobile layout
- Above `md`: Desktop layout

### Stack Direction Patterns

**Page Headers:**
```typescript
<Stack
  direction={{ xs: 'column', md: 'row' }}
  justifyContent="space-between"
  alignItems={{ xs: 'flex-start', md: 'center' }}
  spacing={2}
>
  <Typography variant="h4">Title</Typography>
  <Button variant="contained">Action</Button>
</Stack>
```

**Filter Bars:**
```typescript
<Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
  <TextField fullWidth />
  <TextField sx={{ minWidth: { md: 180 } }} />
</Stack>
```

**Button Groups:**
```typescript
<Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
  <Button>Action 1</Button>
  <Button>Action 2</Button>
</Stack>
```

### Grid Responsiveness

**Two-Column Layout:**
```typescript
<Grid container spacing={3}>
  <Grid item xs={12} md={6}>Column 1</Grid>
  <Grid item xs={12} md={6}>Column 2</Grid>
</Grid>
```

**Three-Column Layout:**
```typescript
<Grid container spacing={3}>
  <Grid item xs={12} sm={6} md={4}>Column 1</Grid>
  <Grid item xs={12} sm={6} md={4}>Column 2</Grid>
  <Grid item xs={12} sm={6} md={4}>Column 3</Grid>
</Grid>
```

### Table Responsiveness

Always use `ResponsiveTable` component:
- Automatically converts to cards on mobile
- Use `hideOnMobile` for action columns
- Provide `mobileCardRender` for custom mobile layout

### Touch Targets

Minimum touch target size: 44x44px

**Buttons:**
```typescript
<Button size="large">Mobile-friendly</Button>
<IconButton size="large"><Icon /></IconButton>
```

**Clickable Areas:**
```typescript
<Card
  sx={{
    cursor: 'pointer',
    minHeight: 44,
    '&:active': { bgcolor: 'action.selected' },
  }}
>
  {/* Content */}
</Card>
```

---

## State Management

Custom hooks manage feature state using React hooks pattern.

### List Hook Pattern

**Standard List Hook:**

```typescript
// hooks/useEntities.ts

import { useState, useEffect, useCallback, useRef } from 'react';
import { entityService } from '../services/entityService';
import type { Entity, EntityListFilters } from '../types/entity';

export interface EntityListResponse {
  entities: Entity[];
  isLoading: boolean;
  error: string | null;
  filters: EntityListFilters;
  pagination?: PaginationMeta;
  updatePage: (page: number) => void;
  updateSearch: (search: string) => void;
  updateStatus: (status: string) => void;
  refetch: () => void;
}

export const useEntities = (
  initialFilters: EntityListFilters
): EntityListResponse => {
  const [entities, setEntities] = useState<Entity[]>([]);
  const [filters, setFilters] = useState<EntityListFilters>(initialFilters);
  const [pagination, setPagination] = useState<PaginationMeta | undefined>();
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Abort controller to prevent race conditions
  const abortControllerRef = useRef<AbortController | null>(null);

  const fetchEntities = useCallback(async () => {
    // Abort previous request
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    abortControllerRef.current = new AbortController();

    setIsLoading(true);
    setError(null);

    try {
      const response = await entityService.listEntities(filters, {
        signal: abortControllerRef.current.signal,
      });

      setEntities(Array.isArray(response.data) ? response.data : []);
      setPagination(response.meta?.pagination);
    } catch (err: any) {
      if (err.name !== 'AbortError') {
        setError(err.message || 'Failed to load entities');
        setEntities([]);
      }
    } finally {
      if (abortControllerRef.current) {
        setIsLoading(false);
      }
    }
  }, [filters]);

  useEffect(() => {
    fetchEntities();

    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, [fetchEntities]);

  // Filter update functions
  const updatePage = useCallback((page: number) => {
    setFilters((prev) => ({ ...prev, page }));
  }, []);

  const updateSearch = useCallback((search: string) => {
    setFilters((prev) => ({ ...prev, search, page: 1 }));
  }, []);

  const updateStatus = useCallback((status: string) => {
    setFilters((prev) => ({ ...prev, status, page: 1 }));
  }, []);

  const refetch = useCallback(() => {
    fetchEntities();
  }, [fetchEntities]);

  return {
    entities,
    isLoading,
    error,
    filters,
    pagination,
    updatePage,
    updateSearch,
    updateStatus,
    refetch,
  };
};
```

### Single Entity Hook Pattern

**Standard Detail Hook:**

```typescript
// hooks/useEntity.ts

import { useState, useEffect } from 'react';
import { entityService } from '../services/entityService';
import type { Entity } from '../types/entity';

export interface EntityResponse {
  entity: Entity | null;
  isLoading: boolean;
  error: string | null;
  refetch: () => void;
}

export const useEntity = (id: string | undefined): EntityResponse => {
  const [entity, setEntity] = useState<Entity | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchEntity = async () => {
    if (!id) {
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await entityService.getEntity(id);
      setEntity(response.data);
    } catch (err: any) {
      setError(err.message || 'Failed to load entity');
      setEntity(null);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchEntity();
  }, [id]);

  const refetch = () => {
    fetchEntity();
  };

  return { entity, isLoading, error, refetch };
};
```

### Command Hook Pattern

**Create Hook:**

```typescript
// hooks/useCreateEntity.ts

import { useState } from 'react';
import { entityService } from '../services/entityService';
import type { CreateEntityRequest, Entity } from '../types/entity';

export interface CreateEntityResponse {
  createEntity: (request: CreateEntityRequest) => Promise<Entity>;
  isCreating: boolean;
  error: string | null;
}

export const useCreateEntity = (): CreateEntityResponse => {
  const [isCreating, setIsCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const createEntity = async (request: CreateEntityRequest): Promise<Entity> => {
    setIsCreating(true);
    setError(null);

    try {
      const response = await entityService.createEntity(request);
      return response.data;
    } catch (err: any) {
      const message = err.response?.data?.error?.message || 'Failed to create entity';
      setError(message);
      throw err;
    } finally {
      setIsCreating(false);
    }
  };

  return { createEntity, isCreating, error };
};
```

**Update Hook:**

```typescript
// hooks/useUpdateEntity.ts

import { useState } from 'react';
import { entityService } from '../services/entityService';
import type { UpdateEntityRequest, Entity } from '../types/entity';

export interface UpdateEntityResponse {
  updateEntity: (id: string, request: UpdateEntityRequest) => Promise<Entity>;
  isUpdating: boolean;
  error: string | null;
}

export const useUpdateEntity = (): UpdateEntityResponse => {
  const [isUpdating, setIsUpdating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const updateEntity = async (
    id: string,
    request: UpdateEntityRequest
  ): Promise<Entity> => {
    setIsUpdating(true);
    setError(null);

    try {
      const response = await entityService.updateEntity(id, request);
      return response.data;
    } catch (err: any) {
      const message = err.response?.data?.error?.message || 'Failed to update entity';
      setError(message);
      throw err;
    } finally {
      setIsUpdating(false);
    }
  };

  return { updateEntity, isUpdating, error };
};
```

### Hook Best Practices

1. **Always use abort controllers** in list hooks to prevent race conditions
2. **Reset page to 1** when changing search/filters
3. **Defensive null checks** on response data
4. **Extract error messages** from API response structure
5. **Consistent naming**: `isLoading`, `isCreating`, `isUpdating`
6. **Refetch function** for manual refresh
7. **Cleanup on unmount** (abort requests, clear timeouts)

---

## API Integration

All API calls use the standardized service layer.

### Service Structure

**Template:**

```typescript
// services/entityService.ts

import apiClient from '@/services/apiClient';
import type {
  Entity,
  CreateEntityRequest,
  UpdateEntityRequest,
  EntityListFilters,
  ApiResponse,
} from '../types/entity';

class EntityService {
  private baseUrl = '/entities';

  async listEntities(
    filters: EntityListFilters,
    options?: { signal?: AbortSignal }
  ): Promise<ApiResponse<Entity[]>> {
    const params = new URLSearchParams();
    if (filters.page) params.append('page', filters.page.toString());
    if (filters.pageSize) params.append('pageSize', filters.pageSize.toString());
    if (filters.search) params.append('search', filters.search);
    if (filters.status && filters.status !== 'all') {
      params.append('status', filters.status);
    }

    const response = await apiClient.get<ApiResponse<Entity[]>>(
      `${this.baseUrl}?${params.toString()}`,
      { signal: options?.signal }
    );
    return response.data;
  }

  async getEntity(id: string): Promise<ApiResponse<Entity>> {
    const response = await apiClient.get<ApiResponse<Entity>>(
      `${this.baseUrl}/${id}`
    );
    return response.data;
  }

  async createEntity(
    request: CreateEntityRequest
  ): Promise<ApiResponse<Entity>> {
    const response = await apiClient.post<ApiResponse<Entity>>(
      this.baseUrl,
      request
    );
    return response.data;
  }

  async updateEntity(
    id: string,
    request: UpdateEntityRequest
  ): Promise<ApiResponse<Entity>> {
    const response = await apiClient.put<ApiResponse<Entity>>(
      `${this.baseUrl}/${id}`,
      request
    );
    return response.data;
  }

  async deleteEntity(id: string): Promise<void> {
    await apiClient.delete(`${this.baseUrl}/${id}`);
  }
}

export const entityService = new EntityService();
```

### API Client Configuration

The base API client is configured with:

1. **Base URL** from environment
2. **Correlation ID** injection (automatic)
3. **Auth token** injection (automatic)
4. **Tenant ID** header (where applicable)
5. **Error interceptors** for global error handling

```typescript
// services/apiClient.ts (already implemented)

import axios from 'axios';
import { correlationIdService } from './correlationIdService';
import { tokenStorage } from '@/utils/tokenStorage';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000,
  withCredentials: true,
});

// Request interceptor
apiClient.interceptors.request.use((config) => {
  // Inject token
  const token = tokenStorage.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  // Inject correlation ID
  const correlationId = correlationIdService.getCorrelationId();
  config.headers[correlationIdService.getCorrelationIdHeader()] = correlationId;

  return config;
});

export default apiClient;
```

### Correlation ID Service

```typescript
// services/correlationIdService.ts (already provided in template)

const CORRELATION_ID_STORAGE_KEY = 'wms_correlation_id';
const CORRELATION_ID_HEADER = 'X-Correlation-Id';

export const correlationIdService = {
  getCorrelationId: () => {
    let id = sessionStorage.getItem(CORRELATION_ID_STORAGE_KEY);
    if (!id) {
      id = crypto.randomUUID();
      sessionStorage.setItem(CORRELATION_ID_STORAGE_KEY, id);
    }
    return id;
  },

  clearCorrelationId: () => {
    sessionStorage.removeItem(CORRELATION_ID_STORAGE_KEY);
  },

  getCorrelationIdHeader: () => CORRELATION_ID_HEADER,
};
```

### API Response Types

All API responses follow this structure:

```typescript
export interface ApiResponse<T> {
  data: T;
  error: ApiError | null;
  meta: ApiMeta | null;
}

export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, any>;
}

export interface ApiMeta {
  correlationId: string;
  timestamp: string;
  pagination?: PaginationMeta;
}

export interface PaginationMeta {
  currentPage: number;
  pageSize: number;
  totalPages: number;
  totalItems: number;
}
```

---

## Error Handling

### Error Display Patterns

**Page-Level Errors:**
```typescript
// Handled by layout components
<ListPageLayout
  error={error}  // Shows Alert at top of page
  {/* ... */}
>
  {/* Content */}
</ListPageLayout>
```

**Form-Level Errors:**
```typescript
// Handled by FormPageLayout
<FormPageLayout
  error={error}  // Shows Alert above form
  {/* ... */}
>
  <EntityForm />
</FormPageLayout>
```

**Field-Level Errors:**
```typescript
// Handled by react-hook-form
<TextField
  {...register('fieldName')}
  error={!!errors.fieldName}
  helperText={errors.fieldName?.message}
/>
```

**Action Errors:**
```typescript
// Show in Snackbar
const [snackbar, setSnackbar] = useState({ open: false, message: '' });

const handleAction = async () => {
  try {
    await entityService.doAction(id);
    setSnackbar({ open: true, message: 'Action completed successfully' });
  } catch (err) {
    setSnackbar({ open: true, message: err.message || 'Action failed' });
  }
};

<Snackbar
  open={snackbar.open}
  message={snackbar.message}
  onClose={() => setSnackbar({ open: false, message: '' })}
  autoHideDuration={6000}
/>
```

### Error Message Standards

**User-Friendly Messages:**
```typescript
// ❌ Bad - technical
"Error: NetworkError at line 42"

// ✅ Good - user-friendly
"Unable to load tenants. Please check your connection and try again."
```

**Specific Error Messages:**
```typescript
const getErrorMessage = (err: any): string => {
  // Extract from API response
  if (err.response?.data?.error?.message) {
    return err.response.data.error.message;
  }

  // Network errors
  if (err.code === 'ERR_NETWORK') {
    return 'Network error. Please check your connection.';
  }

  // Timeout errors
  if (err.code === 'ECONNABORTED') {
    return 'Request timed out. Please try again.';
  }

  // Generic fallback
  return 'An unexpected error occurred. Please try again.';
};
```

---

## Validation Patterns

Use `validationUtils.ts` for consistent validation.

### Common Validation Schemas

```typescript
import { z } from 'zod';
import { CommonSchemas, ValidationMessages } from '@/utils/validationUtils';

// Tenant schema
const tenantSchema = z.object({
  tenantId: CommonSchemas.tenantId,
  tenantName: CommonSchemas.requiredString,
  contactEmail: CommonSchemas.email,
  phoneNumber: CommonSchemas.phoneOptional,
});

// Product schema
const productSchema = z.object({
  code: z.string().min(1, 'Code is required').max(50),
  name: z.string().min(1, 'Name is required').max(200),
  description: CommonSchemas.optionalString,
  unitOfMeasure: z.enum(['UNIT', 'BOX', 'PALLET']),
  weight: CommonSchemas.nonNegativeDecimal,
  quantity: CommonSchemas.nonNegativeInteger,
});

// Location schema
const locationSchema = z.object({
  code: z.string().min(1, 'Code is required').max(20),
  barcode: CommonSchemas.optionalString,
  zone: CommonSchemas.requiredString,
  aisle: CommonSchemas.optionalString,
  rack: CommonSchemas.optionalString,
  shelf: CommonSchemas.optionalString,
  bin: CommonSchemas.optionalString,
  capacity: CommonSchemas.positiveInteger,
});
```

### Custom Validation

```typescript
import { z } from 'zod';

const passwordConfirmSchema = z
  .object({
    password: z.string().min(8, 'Password must be at least 8 characters'),
    confirmPassword: z.string(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords don't match",
    path: ['confirmPassword'],
  });
```

### Async Validation

```typescript
const productCodeSchema = z.string().min(1).refine(
  async (code) => {
    const response = await entityService.checkCodeUniqueness(code);
    return response.data.isUnique;
  },
  { message: 'Product code already exists' }
);
```

---

## Testing Patterns

### Component Testing

Use React Testing Library for component tests.

**List Component Test:**
```typescript
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { EntityList } from './EntityList';

describe('EntityList', () => {
  it('renders entities', () => {
    const entities = [
      { id: '1', code: 'E001', name: 'Entity 1', status: 'active' },
      { id: '2', code: 'E002', name: 'Entity 2', status: 'inactive' },
    ];

    render(<EntityList entities={entities} />);

    expect(screen.getByText('E001')).toBeInTheDocument();
    expect(screen.getByText('E002')).toBeInTheDocument();
  });

  it('shows empty state when no entities', () => {
    render(<EntityList entities={[]} />);
    expect(screen.getByText(/no entities found/i)).toBeInTheDocument();
  });

  it('handles row click', async () => {
    const onRowClick = jest.fn();
    const entities = [{ id: '1', code: 'E001', name: 'Entity 1' }];

    render(<EntityList entities={entities} onRowClick={onRowClick} />);

    await userEvent.click(screen.getByText('E001'));
    expect(onRowClick).toHaveBeenCalledWith(entities[0]);
  });
});
```

**Form Component Test:**
```typescript
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { EntityForm } from './EntityForm';

describe('EntityForm', () => {
  it('validates required fields', async () => {
    const onSubmit = jest.fn();
    render(<EntityForm onSubmit={onSubmit} isSubmitting={false} />);

    await userEvent.click(screen.getByRole('button', { name: /submit/i }));

    expect(await screen.findByText(/code is required/i)).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('submits valid form', async () => {
    const onSubmit = jest.fn();
    render(<EntityForm onSubmit={onSubmit} isSubmitting={false} />);

    await userEvent.type(screen.getByLabelText(/code/i), 'E001');
    await userEvent.type(screen.getByLabelText(/name/i), 'Test Entity');
    await userEvent.click(screen.getByRole('button', { name: /submit/i }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith({
        code: 'E001',
        name: 'Test Entity',
      });
    });
  });
});
```

### Hook Testing

```typescript
import { renderHook, waitFor } from '@testing-library/react';
import { useEntities } from './useEntities';
import { entityService } from '../services/entityService';

jest.mock('../services/entityService');

describe('useEntities', () => {
  it('fetches entities on mount', async () => {
    const mockEntities = [{ id: '1', code: 'E001', name: 'Entity 1' }];
    (entityService.listEntities as jest.Mock).mockResolvedValue({
      data: mockEntities,
    });

    const { result } = renderHook(() =>
      useEntities({ page: 1, pageSize: 20, search: '', status: 'all' })
    );

    expect(result.current.isLoading).toBe(true);

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
      expect(result.current.entities).toEqual(mockEntities);
    });
  });
});
```

---

## Accessibility Guidelines

All components must meet WCAG 2.1 AA standards.

### Keyboard Navigation

**Focus Management:**
```typescript
// Auto-focus first field in forms
<TextField
  {...register('code')}
  autoFocus
  label="Code"
/>

// Tab order follows visual order
<form>
  <TextField tabIndex={1} />
  <TextField tabIndex={2} />
  <Button tabIndex={3} />
</form>
```

**Keyboard Shortcuts:**
```typescript
// ESC to close dialogs
<Dialog onClose={handleClose} onKeyDown={(e) => e.key === 'Escape' && handleClose()}>
  {/* Content */}
</Dialog>

// Enter to submit forms (native behavior, ensure buttons have correct type)
<Button type="submit">Submit</Button>
```

### ARIA Labels

**Landmarks:**
```typescript
<nav aria-label="breadcrumb">
  <Breadcrumbs />
</nav>

<main aria-label="main content">
  {/* Page content */}
</main>
```

**Form Labels:**
```typescript
// Use label prop for automatic association
<TextField label="Email" />

// Or explicit labels for custom inputs
<label htmlFor="custom-input">Custom Field</label>
<input id="custom-input" />
```

**Button Labels:**
```typescript
// Icon buttons must have aria-label
<IconButton aria-label="Delete item">
  <DeleteIcon />
</IconButton>

// Loading state announcements
<Button aria-busy={isLoading} aria-live="polite">
  {isLoading ? 'Loading...' : 'Submit'}
</Button>
```

### Color Contrast

- Text: Minimum 4.5:1 contrast ratio
- Large text (18pt+): Minimum 3:1 contrast ratio
- UI components: Minimum 3:1 contrast ratio

**Status Colors:**
```typescript
// Success: Green with sufficient contrast
success: '#2e7d32'

// Warning: Orange/amber with sufficient contrast
warning: '#ed6c02'

// Error: Red with sufficient contrast
error: '#d32f2f'

// Info: Blue with sufficient contrast
info: '#0288d1'
```

### Screen Reader Support

**Live Regions:**
```typescript
// Announce loading states
<div role="status" aria-live="polite">
  {isLoading ? 'Loading entities...' : `${entities.length} entities loaded`}
</div>

// Announce errors
<Alert role="alert" aria-live="assertive">
  {error}
</Alert>
```

**Hidden Content:**
```typescript
// Visually hidden but accessible to screen readers
<Box sx={{ position: 'absolute', left: '-10000px', width: 1, height: 1 }}>
  Additional context for screen readers
</Box>
```

---

## Implementation Checklist

When implementing a new feature, follow this checklist:

### Planning Phase

- [ ] Define domain types in `types/{feature}.ts`
- [ ] Define validation schemas using Zod
- [ ] Design responsive layouts (mobile-first)
- [ ] Plan API endpoints and DTOs
- [ ] Identify reusable components

### Implementation Phase

**Service Layer:**
- [ ] Create `services/{feature}Service.ts` with all CRUD operations
- [ ] Use `apiClient` for all requests
- [ ] Return `ApiResponse<T>` types
- [ ] Handle pagination, filters, sorting

**Hooks:**
- [ ] Create `hooks/use{Features}.ts` (list hook)
- [ ] Create `hooks/use{Feature}.ts` (single entity hook)
- [ ] Create `hooks/useCreate{Feature}.ts` (create command hook)
- [ ] Create `hooks/useUpdate{Feature}.ts` (update command hook)
- [ ] Implement abort controllers for race condition prevention

**Components:**
- [ ] Create `components/{Feature}List.tsx` using ResponsiveTable
- [ ] Create `components/{Feature}Detail.tsx` with card layout
- [ ] Create `components/{Feature}Form.tsx` with validation
- [ ] Create `components/{Feature}Actions.tsx` for entity actions

**Pages:**
- [ ] Create `pages/{Feature}ListPage.tsx` using ListPageLayout
- [ ] Create `pages/{Feature}DetailPage.tsx` using DetailPageLayout
- [ ] Create `pages/{Feature}CreatePage.tsx` using FormPageLayout
- [ ] Create `pages/{Feature}EditPage.tsx` using FormPageLayout

**Navigation:**
- [ ] Add routes to `Routes` object in `navigationUtils.ts`
- [ ] Add breadcrumb helpers to `getBreadcrumbs`
- [ ] Update router configuration

### Testing Phase

- [ ] Unit tests for hooks
- [ ] Component tests for forms
- [ ] Component tests for lists
- [ ] Integration tests for page flows
- [ ] Accessibility tests (axe-core)
- [ ] Responsive design tests (mobile, tablet, desktop)

### Documentation Phase

- [ ] Document any custom components
- [ ] Document validation rules
- [ ] Document API contracts
- [ ] Update this guide if new patterns emerge

---

## Anti-Patterns to Avoid

### ❌ Don't: Inline Breadcrumbs in Components

```typescript
// Bad - breadcrumbs inside component
const LocationDetail = ({ location }) => (
  <>
    <Breadcrumbs>...</Breadcrumbs>
    <Card>{/* Details */}</Card>
  </>
);
```

```typescript
// Good - breadcrumbs in page
const LocationDetailPage = () => (
  <DetailPageLayout breadcrumbs={getBreadcrumbs.locationDetail(location.code)}>
    <LocationDetail location={location} />
  </DetailPageLayout>
);
```

### ❌ Don't: Hardcode Routes

```typescript
// Bad
navigate('/admin/tenants');

// Good
import { Routes } from '@/utils/navigationUtils';
navigate(Routes.admin.tenants);
```

### ❌ Don't: Inconsistent Error Messages

```typescript
// Bad - technical error
setError('Error: NetworkError');

// Good - user-friendly
setError('Unable to load data. Please check your connection.');
```

### ❌ Don't: Skip Loading States

```typescript
// Bad - no loading indicator
return <TenantList tenants={tenants} />;

// Good - show loading state
return isLoading ? <LoadingSpinner /> : <TenantList tenants={tenants} />;
```

### ❌ Don't: Custom Table Implementations

```typescript
// Bad - custom table
return (
  <Table>
    {entities.map(entity => <TableRow>...</TableRow>)}
  </Table>
);

// Good - use ResponsiveTable
return <ResponsiveTable data={entities} columns={columns} />;
```

### ❌ Don't: Skip Form Validation

```typescript
// Bad - no validation
const handleSubmit = (data) => {
  createEntity(data);
};

// Good - Zod validation
const schema = z.object({ ... });
const { handleSubmit } = useForm({ resolver: zodResolver(schema) });
```

### ❌ Don't: Ignore Mobile Responsiveness

```typescript
// Bad - fixed direction
<Stack direction="row">...</Stack>

// Good - responsive direction
<Stack direction={{ xs: 'column', md: 'row' }}>...</Stack>
```

---

## Document Control

**Version History:**
- v2.0 (2025-01) - Complete rewrite with shared components, responsive patterns, and standardized templates
- v1.0 (2025-01) - Initial template creation

**Review Cycle:**
- Review quarterly or when significant pattern changes emerge
- Update when new shared components are added
- Maintain backward compatibility when possible

**Governance:**
- All deviations from this guide must be approved by technical lead
- New patterns must be documented and shared
- Regular pattern review sessions to improve standards

---

## Quick Reference

### File Locations

| File Type | Location | Example |
|-----------|----------|---------|
| Shared Components | `src/components/common/` | `PageBreadcrumbs.tsx` |
| Layout Components | `src/components/layouts/` | `DetailPageLayout.tsx` |
| Feature Components | `src/features/{feature}/components/` | `TenantList.tsx` |
| Feature Pages | `src/features/{feature}/pages/` | `TenantListPage.tsx` |
| Feature Hooks | `src/features/{feature}/hooks/` | `useTenants.ts` |
| Feature Services | `src/features/{feature}/services/` | `tenantService.ts` |
| Feature Types | `src/features/{feature}/types/` | `tenant.ts` |
| Utils | `src/utils/` | `dateUtils.ts` |

### Import Shortcuts

```typescript
// Shared components
import {
  PageBreadcrumbs,
  PageHeader,
  LoadingSpinner,
  EmptyState,
  ActionDialog,
  FilterBar,
  StatusBadge,
  FormActions,
  ResponsiveTable,
  Pagination,
} from '@/components/common';

// Layouts
import {
  DetailPageLayout,
  FormPageLayout,
  ListPageLayout,
} from '@/components/layouts';

// Utils
import { formatDateTime, formatDate } from '@/utils/dateUtils';
import { Routes, getBreadcrumbs } from '@/utils/navigationUtils';
import {
  CommonSchemas,
  ValidationMessages,
  ValidationPatterns,
} from '@/utils/validationUtils';
```

### Common Patterns Quick Copy

**List Page:**
```typescript
<ListPageLayout breadcrumbs={} title="" actions={}>
  <FilterBar>...</FilterBar>
  <ResponsiveTable data={} columns={} />
  <Pagination {...} />
</ListPageLayout>
```

**Detail Page:**
```typescript
<DetailPageLayout breadcrumbs={} title="" actions={}>
  <Grid container spacing={3}>
    <Grid item xs={12} md={6}>
      <Paper elevation={1} sx={{ p: 3 }}>...</Paper>
    </Grid>
  </Grid>
</DetailPageLayout>
```

**Form Page:**
```typescript
<FormPageLayout breadcrumbs={} title="" description="">
  <form onSubmit={handleSubmit(onSubmit)}>
    <Paper sx={{ p: 3 }}>
      <Grid container spacing={3}>...</Grid>
      <FormActions onCancel={} isSubmitting={} />
    </Paper>
  </form>
</FormPageLayout>
```

---

**End of Document**

