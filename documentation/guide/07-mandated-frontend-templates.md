# Mandated Frontend Templates

## Warehouse Management System - CCBSA LDP System

**Document Version:** 2.1
**Date:** 2025-01
**Status:** Active

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
│   │   │   ├── SkeletonTable.tsx
│   │   │   ├── SkeletonCard.tsx
│   │   │   ├── SkeletonForm.tsx
│   │   │   ├── ErrorBoundaryWithRetry.tsx
│   │   │   └── index.ts
│   │   ├── layouts/                    # Page layout components
│   │   │   ├── DetailPageLayout.tsx
│   │   │   ├── FormPageLayout.tsx
│   │   │   ├── ListPageLayout.tsx
│   │   │   ├── DashboardPageLayout.tsx
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
│   │   ├── useTenant.ts
│   │   └── useToast.ts
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

Centered loading indicator with consistent styling. **Note:** For better UX, prefer using skeleton loaders (SkeletonTable, SkeletonCard, SkeletonForm) instead of LoadingSpinner.

**Usage:**
\`\`\`typescript
import { LoadingSpinner } from '@/components/common';

{isLoading && <LoadingSpinner />}
\`\`\`

**Props:**
- `size?: number` - Spinner size in pixels (default: 40)
- `minHeight?: string | number` - Minimum container height (default: '400px')

---

### SkeletonTable

Skeleton loader for table/list content. Automatically used by ListPageLayout when loading.

**Usage:**
\`\`\`typescript
import { SkeletonTable } from '@/components/common';

{isLoading ? <SkeletonTable rows={5} columns={4} /> : <DataTable data={data} />}
\`\`\`

**Props:**
- `rows?: number` - Number of skeleton rows (default: 5)
- `columns?: number` - Number of skeleton columns (default: 4)

---

### SkeletonCard

Skeleton loader for card/detail content. Automatically used by DetailPageLayout when loading.

**Usage:**
\`\`\`typescript
import { SkeletonCard } from '@/components/common';

{isLoading ? <SkeletonCard lines={6} /> : <DetailContent data={data} />}
\`\`\`

**Props:**
- `lines?: number` - Number of skeleton field lines (default: 4)

---

### SkeletonForm

Skeleton loader for form content. Use when loading form default values.

**Usage:**
\`\`\`typescript
import { SkeletonForm } from '@/components/common';

{isLoadingProduct ? (
  <SkeletonForm fields={8} />
) : (
  <ProductForm defaultValues={product} />
)}
\`\`\`

**Props:**
- `fields?: number` - Number of skeleton form fields (default: 6)

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

Standardized form button group (Cancel + Submit). **Mobile-responsive:** Buttons stack vertically on mobile, horizontal on desktop.

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
- Right-aligned button group on desktop
- Stacked vertically on mobile (xs breakpoint)
- Buttons full-width on mobile
- Cancel button: `variant="outlined"`, disabled during submission
- Submit button: `variant="contained"`, shows "{label}..." during submission
- `mt: 3` spacing
- ARIA labels included for accessibility

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
- Responsive: Stacks on mobile, horizontal on desktop

**Note:** Always use this custom Pagination component instead of MUI's Pagination directly for consistency.

---

### BarcodeInput

Universal barcode input component that supports both scanning and manual entry. **Barcode scanning is the primary input method** - manual entry is provided as a fallback when scanning fails or is unavailable.

**Barcode-First Principle:**
- **Primary:** Barcode scanning (handheld scanner or camera)
- **Fallback:** Manual keyboard input
- Always provide both options for maximum flexibility

**Usage:**
\`\`\`typescript
import { BarcodeInput } from '@/components/common';

<BarcodeInput
  label="Product Barcode"
  value={barcode}
  onChange={setBarcode}
  onScan={handleBarcodeScan}
  placeholder="Scan or enter barcode"
  autoFocus
  helperText="Scan barcode first, or enter manually if scanning fails"
/>
\`\`\`

**Props:**
- `value: string` - Current barcode value
- `onChange: (value: string) => void` - Value change handler (for manual input)
- `onScan?: (barcode: string) => void` - Barcode scan handler (called when barcode is scanned)
- `enableCamera?: boolean` - Enable camera scanning button (default: true)
- `autoSubmitOnEnter?: boolean` - Auto-submit on Enter key (default: false)
- All standard `TextField` props are supported

**Features:**
- **Handheld Scanner Support:** Automatically captures input from USB/Bluetooth scanners (acts as keyboard)
- **Camera Scanning:** Built-in camera button opens barcode scanner dialog
- **Manual Entry:** Full keyboard input support as fallback
- **Auto-Focus:** Optional auto-focus for better scanner UX
- **Enter Key Handling:** Supports Enter key submission for handheld scanners

**Implementation Pattern:**
\`\`\`typescript
const ProductForm = () => {
  const [barcode, setBarcode] = useState('');
  const [barcodeError, setBarcodeError] = useState<string | null>(null);

  // Handle barcode scan (primary method)
  const handleBarcodeScan = async (scannedBarcode: string) => {
    setBarcode(scannedBarcode);
    setBarcodeError(null);
    
    // Validate barcode
    try {
      const product = await validateBarcode(scannedBarcode);
      // Auto-fill product information
      setProductCode(product.code);
    } catch (error) {
      setBarcodeError('Barcode not found or invalid');
    }
  };

  // Handle manual input (fallback)
  const handleBarcodeChange = (value: string) => {
    setBarcode(value);
    setBarcodeError(null);
  };

  return (
    <BarcodeInput
      label="Product Barcode"
      value={barcode}
      onChange={handleBarcodeChange}
      onScan={handleBarcodeScan}
      error={!!barcodeError}
      helperText={barcodeError || 'Scan barcode first, or enter manually'}
      autoFocus
    />
  );
};
\`\`\`

**When to Use:**
- **Product Barcodes:** Product forms, consignment entry, stock count
- **Location Barcodes:** Location assignment, stock movement, picking
- **Search Fields:** Product search, location search, consignment search
- **Any Identifier Input:** Any field that can be scanned should use BarcodeInput

**Standard Spacing:** `sx={{ mb: 2 }}` (when used in forms)

**Accessibility:**
- Auto-focus on mount for handheld scanners
- Clear labels and helper text
- Error messages for invalid barcodes
- Keyboard navigation support

---

## Page Layout Templates

All pages must use layout components from `src/components/layouts/`.

### DetailPageLayout

Standard layout for detail/view pages.

**Features:**
- Header with navigation
- Breadcrumbs
- Page title with actions
- Loading state (uses SkeletonCard automatically)
- Error alert (with ARIA live region)
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
- Error alert (with ARIA live region)
- Form content area (with ARIA region)
- Main landmark with aria-label

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
- Error alert (with ARIA live region)
- Loading state (uses SkeletonTable automatically)
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

### DashboardPageLayout

Standard layout for dashboard pages (Admin/User dashboards).

**Features:**
- Header with navigation
- Page title and subtitle
- Content area with Grid layout support
- Main landmark with aria-label

**Usage:**
\`\`\`typescript
import { DashboardPageLayout } from '@/components/layouts';

export const UserDashboard = () => {
  const { user } = useAuth();

  return (
    <DashboardPageLayout
      title="Warehouse Operations Dashboard"
      subtitle={`Welcome, ${user?.firstName || user?.username}!`}
    >
      <Grid container spacing={3}>
        {/* Dashboard cards */}
      </Grid>
    </DashboardPageLayout>
  );
};
\`\`\`

**Props:**
- `title: string` - Page title (h4 variant)
- `subtitle?: string` - Optional subtitle (body1, text.secondary)
- `children: ReactNode` - Dashboard content
- `maxWidth?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'` - Container max width (default: 'lg')

---

### ErrorBoundaryWithRetry

Error boundary component with retry functionality. Wraps the entire App to catch React errors gracefully.

**Usage:**
\`\`\`typescript
import { ErrorBoundaryWithRetry } from '@/components/common';

function App() {
  return (
    <ErrorBoundaryWithRetry>
      <Routes>
        {/* Routes */}
      </Routes>
    </ErrorBoundaryWithRetry>
  );
}
\`\`\`

**Features:**
- Catches React component errors
- Displays user-friendly error message
- Provides "Retry" button to reset error state
- Provides "Go Home" button for navigation
- Logs errors for debugging

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

For uniqueness checks or external validation with visual feedback:

```typescript
import { useDebounce } from '@/hooks/useDebounce';
import { CircularProgress, InputAdornment } from '@mui/material';
import { useState, useEffect } from 'react';

const ProductForm = ({ onSubmit, onCancel, isSubmitting, isUpdate }) => {
  const { register, handleSubmit, watch, formState: { errors } } = useForm();
  const productCode = watch('productCode');
  const debouncedProductCode = useDebounce(productCode, 500);
  const [validationState, setValidationState] = useState<'idle' | 'validating' | 'valid' | 'invalid'>('idle');
  const [productCodeError, setProductCodeError] = useState<string | null>(null);
  const [isCheckingUniqueness, setIsCheckingUniqueness] = useState(false);

  // Async validation with debounce
  useEffect(() => {
    if (!isUpdate && debouncedProductCode && debouncedProductCode.length > 0) {
      setValidationState('validating');
      setIsCheckingUniqueness(true);
      checkUniqueness(debouncedProductCode, tenantId)
        .then(isUnique => {
          if (!isUnique) {
            setProductCodeError('Product code already exists');
            setValidationState('invalid');
          } else {
            setProductCodeError(null);
            setValidationState('valid');
          }
        })
        .catch(() => {
          setProductCodeError('Failed to check product code uniqueness');
          setValidationState('invalid');
        })
        .finally(() => {
          setIsCheckingUniqueness(false);
        });
    } else if (!debouncedProductCode || debouncedProductCode.length === 0) {
      setValidationState('idle');
      setProductCodeError(null);
    }
  }, [debouncedProductCode, isUpdate, tenantId]);

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <TextField
        {...register('productCode')}
        label="Product Code"
        fullWidth
        required
        disabled={isUpdate}
        error={!!errors.productCode || validationState === 'invalid'}
        helperText={
          errors.productCode?.message ||
          productCodeError ||
          (validationState === 'validating' ? 'Checking availability...' : '') ||
          (validationState === 'valid' && !isUpdate ? 'Product code available' : '') ||
          ''
        }
        aria-label="Product code input field"
        aria-required="true"
        aria-describedby="product-code-helper"
        FormHelperTextProps={{ id: 'product-code-helper' }}
        autoFocus
        InputProps={{
          endAdornment:
            validationState === 'validating' ? (
              <InputAdornment position="end">
                <CircularProgress size={20} aria-label="Validating product code" />
              </InputAdornment>
            ) : null,
        }}
      />
      {/* ... */}
      <FormActions
        onCancel={onCancel}
        isSubmitting={isSubmitting}
        submitDisabled={!!productCodeError || validationState === 'validating' || validationState === 'invalid'}
      />
    </form>
  );
};
```

**Best Practices:**
- Use `useDebounce` hook to avoid excessive API calls (500ms delay recommended)
- Show visual feedback (CircularProgress) during validation
- Provide clear helper text for each validation state
- Disable submit button during validation or when invalid
- Use validation state enum for type safety

### Grid Layout Guidelines

- Use Material-UI Grid system
- Standard breakpoints:
  - `xs={12}` - Full width on mobile
  - `md={6}` - Two columns on desktop
  - `md={4}` - Three columns on desktop
- Section spacing: `spacing={3}`
- Paper padding: `sx={{ p: 3 }}`

### Form Field Standards

**Required Fields with ARIA:**
```typescript
<TextField
  {...register('fieldName')}
  label="Field Label"
  required  // Shows asterisk
  fullWidth
  error={!!errors.fieldName}
  helperText={errors.fieldName?.message}
  aria-label="Field label input field"
  aria-required="true"
  aria-describedby="field-name-helper"
  FormHelperTextProps={{ id: 'field-name-helper' }}
  autoFocus  // For first field in form
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

**Barcode Fields (Barcode-First Principle):**
\`\`\`typescript
import { BarcodeInput } from '@/components/common';
import { Controller } from 'react-hook-form';

// In form component
<Controller
  name="barcode"
  control={control}
  render={({ field }) => (
    <BarcodeInput
      {...field}
      label="Barcode"
      fullWidth
      required
      error={!!errors.barcode}
      helperText={errors.barcode?.message || 'Scan barcode first, or enter manually if scanning fails'}
      onScan={async (scannedBarcode) => {
        field.onChange(scannedBarcode);
        // Validate and auto-fill related fields
        try {
          const product = await validateBarcode(scannedBarcode);
          setValue('productCode', product.code);
          setValue('productName', product.name);
        } catch (error) {
          setError('barcode', { message: 'Barcode not found' });
        }
      }}
      autoFocus
    />
  )}
/>
\`\`\`

**Barcode Field Guidelines:**
- **Always use BarcodeInput** for any field that can be scanned (product codes, location codes, barcodes)
- **Scan first, manual input as fallback** - Helper text should guide users to scan first
- **Auto-focus** barcode fields for better scanner UX
- **Auto-validate** scanned barcodes and populate related fields when possible
- **Clear error messages** when barcode validation fails

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
        <BarcodeInput
          label="Search"
          placeholder="Scan barcode or search by code, name..."
          value={filters.search}
          onChange={(value) => updateSearch(value)}
          onScan={(barcode) => {
            updateSearch(barcode);
            // Optionally trigger search immediately
          }}
          fullWidth
          autoSubmitOnEnter={true}
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

Custom hooks manage feature state using React hooks pattern. For server state, use `@tanstack/react-query` for caching, synchronization, and optimistic updates.

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

### React Query Hooks (Recommended)

For server state management, use `@tanstack/react-query`:

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

// Query hook
export const useLocation = (locationId: string, tenantId: string) => {
  return useQuery({
    queryKey: ['location', locationId, tenantId],
    queryFn: () => locationService.getLocation(locationId, tenantId),
    enabled: !!locationId && !!tenantId,
  });
};

// Mutation hook with optimistic updates
export const useUpdateLocationStatus = () => {
  const queryClient = useQueryClient();
  const { success, error: showError } = useToast();

  return useMutation({
    mutationFn: ({ locationId, status, reason, tenantId }) =>
      locationService.updateLocationStatus(locationId, { status, reason }, tenantId),
    onMutate: async ({ locationId, status, tenantId }) => {
      // Optimistic update
      const queryKey = ['location', locationId, tenantId];
      const previousLocation = queryClient.getQueryData(queryKey);
      
      if (previousLocation?.data) {
        queryClient.setQueryData(queryKey, {
          ...previousLocation,
          data: { ...previousLocation.data, status },
        });
      }
      
      return { previousLocation };
    },
    onSuccess: () => {
      success('Location status updated successfully');
    },
    onError: (error, variables, context) => {
      // Rollback optimistic update
      if (context?.previousLocation) {
        queryClient.setQueryData(
          ['location', variables.locationId, variables.tenantId],
          context.previousLocation
        );
      }
      showError(error.message);
      queryClient.invalidateQueries({ queryKey: ['locations'] });
    },
  });
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
8. **Use React Query** for server state (caching, synchronization, optimistic updates)
9. **Optimistic updates** for better UX (update UI immediately, rollback on error)
10. **Toast notifications** for user feedback (success/error messages)

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
6. **Retry logic** with exponential backoff for 5xx errors (max 3 retries)
7. **Rate limit handling** for 429 errors (respects Retry-After header)
8. **Token refresh** on 401 errors (automatic)
9. **Timeout handling** (30 seconds default)

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

**Action Errors and Success Messages:**
```typescript
// Use Toast notifications (recommended)
import { useToast } from '@/hooks/useToast';

const MyComponent = () => {
  const { success, error: showError } = useToast();

  const handleAction = async () => {
    try {
      await entityService.doAction(id);
      success('Action completed successfully');
    } catch (err) {
      showError(err.message || 'Action failed');
    }
  };

  // ToastContainer is already included in main.tsx
  return <Button onClick={handleAction}>Do Action</Button>;
};
```

**Toast Hook API:**
```typescript
const { success, error, info, warning } = useToast();

// Success notification (5s auto-close)
success('Product created successfully');

// Error notification (7s auto-close)
error('Failed to create product. Please try again.');

// Info notification (5s auto-close)
info('Product code is available');

// Warning notification (6s auto-close)
warning('Some items could not be processed');
```

**Alternative: Inline Alerts (for page-level messages):**
```typescript
// For persistent page-level messages
<Alert severity="success" sx={{ mb: 3 }} onClose={() => setSuccessMessage(null)}>
  {successMessage}
</Alert>
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

### Async Validation (Legacy Pattern)

**Note:** For better UX, use the debounced validation pattern with visual feedback (see Form Patterns > Async Validation section above).

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

// Tab order follows visual order (native HTML order, no explicit tabIndex needed)
<form>
  <TextField />  {/* Tab order: 1 */}
  <TextField />  {/* Tab order: 2 */}
  <Button type="submit">Submit</Button>  {/* Tab order: 3 */}
</form>
```

**Keyboard Shortcuts:**
```typescript
// ESC to close dialogs (handled automatically by MUI Dialog)
<Dialog
  open={isOpen}
  onClose={handleClose}
  aria-labelledby="dialog-title"
  aria-describedby="dialog-description"
  aria-modal="true"
>
  {/* Content - ESC key automatically closes */}
</Dialog>

// Enter to submit forms (native behavior, ensure buttons have correct type)
<Button type="submit">Submit</Button>

// Focus-visible styles for keyboard navigation (configured in theme)
// Theme includes :focus-visible styles for Button, TextField, IconButton, Link
```

**Focus Indicators:**
All interactive elements have visible focus indicators configured in the theme:
- Buttons: 3px solid outline with 2px offset
- TextFields: 2px solid outline with 2px offset
- IconButtons: 3px solid outline with 2px offset
- Links: 2px solid outline with 2px offset

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

// Action buttons with aria-label
<Button
  variant="contained"
  onClick={handleAction}
  aria-label="Create new tenant"
>
  Create Tenant
</Button>

// Loading state announcements
<Button
  aria-busy={isLoading}
  aria-label={isLoading ? 'Submitting form...' : 'Submit form'}
>
  {isLoading ? 'Submitting...' : 'Submit'}
</Button>
```

**Dialog ARIA:**
```typescript
<Dialog
  open={isOpen}
  onClose={handleClose}
  aria-labelledby="dialog-title"
  aria-describedby="dialog-description"
  aria-modal="true"
>
  <DialogTitle id="dialog-title">Confirm Action</DialogTitle>
  <DialogContent>
    <DialogContentText id="dialog-description">
      Are you sure you want to proceed?
    </DialogContentText>
  </DialogContent>
  <DialogActions>
    <Button onClick={handleCancel} aria-label="Cancel action">Cancel</Button>
    <Button onClick={handleConfirm} aria-label="Confirm action">Confirm</Button>
  </DialogActions>
</Dialog>
```

### Color Contrast

- Text: Minimum 4.5:1 contrast ratio (WCAG AA)
- Large text (18pt+): Minimum 3:1 contrast ratio (WCAG AA)
- UI components: Minimum 3:1 contrast ratio (WCAG AA)

**Status Colors (MUI Default - WCAG AA Compliant):**
```typescript
// MUI theme colors are WCAG AA compliant by default
// StatusBadge uses MUI Chip with standard color props:
<StatusBadge
  label={status}
  variant={getStatusVariant(status)}  // Maps to MUI color: success, warning, error, info, default
/>

// MUI default colors (verified WCAG AA):
// Success: '#2e7d32' (4.5:1+ contrast)
// Warning: '#ed6c02' (4.5:1+ contrast)
// Error: '#d32f2f' (4.5:1+ contrast)
// Info: '#0288d1' (4.5:1+ contrast)
```

**Custom Colors:**
If using custom colors, verify contrast ratios using tools like:
- WebAIM Contrast Checker
- axe DevTools
- Chrome DevTools Accessibility panel

### Screen Reader Support

**Live Regions:**
```typescript
// Announce loading states
<div role="status" aria-live="polite">
  {isLoading ? 'Loading entities...' : `${entities.length} entities loaded`}
</div>

// Announce errors (automatically handled by layout components)
<Alert role="alert" aria-live="assertive">
  {error}
</Alert>

// Layout components include ARIA live regions:
// - ListPageLayout: Error alerts have role="alert" aria-live="assertive"
// - DetailPageLayout: Error alerts have role="alert" aria-live="assertive"
// - FormPageLayout: Error alerts have role="alert" aria-live="assertive"
// - FormPageLayout: Content wrapped in role="region" aria-label
```

**Hidden Content:**
```typescript
// Visually hidden but accessible to screen readers
<Box sx={{ position: 'absolute', left: '-10000px', width: 1, height: 1 }}>
  Additional context for screen readers
</Box>
```

---

## Advanced Patterns

### Optimistic UI Updates

Update UI immediately, then sync with server. Rollback on error.

**Pattern:**
```typescript
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useToast } from '@/hooks/useToast';

export const useUpdateLocationStatus = () => {
  const queryClient = useQueryClient();
  const { success, error: showError } = useToast();

  return useMutation({
    mutationFn: ({ locationId, status, tenantId }) =>
      locationService.updateLocationStatus(locationId, { status }, tenantId),
    onMutate: async ({ locationId, status, tenantId }) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: ['location', locationId] });

      // Snapshot previous value
      const previousLocation = queryClient.getQueryData(['location', locationId, tenantId]);

      // Optimistically update
      if (previousLocation?.data) {
        queryClient.setQueryData(['location', locationId, tenantId], {
          ...previousLocation,
          data: { ...previousLocation.data, status },
        });
      }

      return { previousLocation };
    },
    onError: (err, variables, context) => {
      // Rollback on error
      if (context?.previousLocation) {
        queryClient.setQueryData(
          ['location', variables.locationId, variables.tenantId],
          context.previousLocation
        );
      }
      queryClient.invalidateQueries({ queryKey: ['locations'] });
      showError(err.message);
    },
    onSuccess: () => {
      success('Location status updated successfully');
    },
  });
};
```

### Network Error Recovery

The API client automatically retries 5xx errors with exponential backoff:
- Max 3 retries
- Backoff: 1s, 2s, 4s
- 429 errors respect Retry-After header
- 401 errors trigger token refresh

**No additional code needed** - handled automatically by `apiClient`.

### Toast Notifications

Use toast notifications for user feedback instead of inline alerts.

**Setup (already in main.tsx):**
```typescript
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

// In main.tsx
<ToastContainer
  position="top-right"
  autoClose={5000}
  hideProgressBar={false}
  newestOnTop={false}
  closeOnClick
  rtl={false}
  pauseOnFocusLoss
  draggable
  pauseOnHover
  theme="light"
/>
```

**Usage:**
```typescript
import { useToast } from '@/hooks/useToast';

const MyComponent = () => {
  const { success, error, info, warning } = useToast();

  const handleAction = async () => {
    try {
      await doAction();
      success('Action completed successfully');
    } catch (err) {
      error(err.message || 'Action failed');
    }
  };
};
```

### Capacity Visualization

Display location capacity with color-coded progress bars.

**Pattern:**
```typescript
import { LinearProgress } from '@mui/material';

const LocationCapacity = ({ capacity }) => {
  if (!capacity || capacity.maximumQuantity <= 0) return null;

  const utilization = (capacity.currentQuantity / capacity.maximumQuantity) * 100;
  const color = utilization >= 80 ? 'error' : utilization >= 50 ? 'warning' : 'success';

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
        <Typography variant="caption">Capacity Utilization</Typography>
        <Typography variant="body2" fontWeight="medium">
          {Math.round(utilization)}%
        </Typography>
      </Box>
      <LinearProgress
        variant="determinate"
        value={Math.min(utilization, 100)}
        color={color}
        sx={{ height: 8, borderRadius: 4 }}
        aria-label="Location capacity utilization"
        aria-valuenow={Math.round(utilization)}
        aria-valuemin={0}
        aria-valuemax={100}
      />
      <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
        {capacity.currentQuantity} / {capacity.maximumQuantity} units
      </Typography>
    </Box>
  );
};
```

### Expiry Alerts

Display expiring consignments on dashboard.

**Pattern:**
```typescript
import { Card, CardContent, Button, Typography } from '@mui/material';
import { useConsignments } from '../hooks/useConsignments';
import { Routes } from '@/utils/navigationUtils';

export const ExpiryAlertCard = () => {
  const { user } = useAuth();
  const { consignments, isLoading } = useConsignments(
    { expiringWithinDays: 7, page: 0, size: 1 },
    user?.tenantId
  );

  const expiringCount = consignments.length;

  return (
    <Card
      sx={{
        bgcolor: expiringCount > 0 ? 'error.light' : 'background.paper',
        border: expiringCount > 0 ? '2px solid' : 'none',
        borderColor: expiringCount > 0 ? 'error.main' : 'transparent',
      }}
    >
      <CardContent>
        <Typography variant="h6">Expiring Consignments</Typography>
        <Typography variant="h3" color={expiringCount > 0 ? 'error.main' : 'text.primary'}>
          {expiringCount}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Consignments expiring within 7 days
        </Typography>
        {expiringCount > 0 && (
          <Button
            variant="contained"
            color="error"
            component={RouterLink}
            to={`${Routes.consignments}?expiringWithinDays=7`}
            fullWidth
          >
            View Details
          </Button>
        )}
      </CardContent>
    </Card>
  );
};
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

### ❌ Don't: Use LoadingSpinner Instead of Skeleton Loaders

```typescript
// Bad - generic spinner
{isLoading && <LoadingSpinner />}

// Good - content-aware skeleton
{isLoading ? <SkeletonTable rows={5} columns={4} /> : <DataTable data={data} />}
```

### ❌ Don't: Use Inline Alerts for Success Messages

```typescript
// Bad - inline alert that takes up space
{successMessage && (
  <Alert severity="success" sx={{ mb: 3 }}>
    {successMessage}
  </Alert>
)}

// Good - toast notification
const { success } = useToast();
success('Action completed successfully');
```

### ❌ Don't: Skip Optimistic Updates

```typescript
// Bad - UI waits for server
const handleUpdate = async () => {
  setIsLoading(true);
  await updateStatus(newStatus);
  setIsLoading(false);
  // UI only updates after server responds
};

// Good - optimistic update
const mutation = useMutation({
  mutationFn: updateStatus,
  onMutate: async (newStatus) => {
    // Update UI immediately
    queryClient.setQueryData(['location'], { ...location, status: newStatus });
  },
  onError: (err, variables, context) => {
    // Rollback on error
    queryClient.setQueryData(['location'], context.previousLocation);
  },
});
```

---

## Document Control

**Version History:**
- v2.1 (2025-01) - Added DashboardPageLayout, Skeleton loaders (SkeletonTable, SkeletonCard, SkeletonForm), ErrorBoundaryWithRetry, Toast notifications (useToast hook), optimistic updates pattern, network retry with exponential backoff, comprehensive mobile responsiveness patterns, enhanced async validation with visual feedback, capacity visualization patterns, expiry alert patterns, FEFO assignment UI patterns, notification system patterns, stock levels monitoring patterns
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
  SkeletonTable,
  SkeletonCard,
  SkeletonForm,
  ErrorBoundaryWithRetry,
} from '@/components/common';

// Layouts
import {
  DetailPageLayout,
  FormPageLayout,
  ListPageLayout,
  DashboardPageLayout,
} from '@/components/layouts';

// Utils
import { formatDateTime, formatDate } from '@/utils/dateUtils';
import { Routes, getBreadcrumbs } from '@/utils/navigationUtils';
import {
  CommonSchemas,
  ValidationMessages,
  ValidationPatterns,
} from '@/utils/validationUtils';

// Hooks
import { useToast } from '@/hooks/useToast';
import { useDebounce } from '@/hooks/useDebounce';
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

**Dashboard Page:**
```typescript
<DashboardPageLayout title="" subtitle="">
  <Grid container spacing={3}>
    <Grid item xs={12} md={6}>
      <Card>...</Card>
    </Grid>
  </Grid>
</DashboardPageLayout>
```

---

## Hierarchical Tree View Pattern

The hierarchical tree view pattern is used for displaying and navigating through hierarchical data structures, such as warehouse locations (warehouse → zone → aisle → rack → bin).

### When to Use Tree Views

Use tree views when:
- Data has a clear parent-child hierarchy
- Users need to navigate through multiple levels
- The hierarchy depth is known and limited (typically 3-5 levels)
- Each level has a manageable number of items (< 100 per level)

**Examples:**
- Location hierarchy (warehouse → zone → aisle → rack → bin)
- Organizational structure
- Category hierarchies
- File system navigation

### LocationTreeView Component

The `LocationTreeView` component displays hierarchical location data with drill-down navigation.

**Location:** `src/features/location-management/components/LocationTreeView.tsx`

**Usage:**
```typescript
import { LocationTreeView } from '../components/LocationTreeView';
import { useLocationHierarchy } from '../hooks/useLocationHierarchy';

export const LocationListPage = () => {
  const { data, isLoading, error, navigationState, navigateToZone, navigateToAisle, navigateToRack, navigateToBin } = useLocationHierarchy();

  return (
    <ListPageLayout>
      <LocationTreeView
        data={data}
        isLoading={isLoading}
        error={error}
        level={navigationState.level}
        onExpand={(locationId) => {
          switch (navigationState.level) {
            case 'warehouse':
              navigateToZone(locationId);
              break;
            case 'zone':
              navigateToAisle(locationId);
              break;
            // ... other levels
          }
        }}
      />
    </ListPageLayout>
  );
};
```

**Props:**
- `data?: LocationHierarchyQueryResult` - Hierarchy data from API
- `isLoading: boolean` - Loading state
- `error: Error | null` - Error state
- `level: LocationHierarchyLevel` - Current hierarchy level
- `onExpand?: (locationId: string) => void` - Handler for expanding/navigating to children
- `onItemClick?: (locationId: string) => void` - Handler for clicking item (default: navigates to detail page)

### LocationTreeItem Component

Individual tree item component for displaying location information.

**Location:** `src/features/location-management/components/LocationTreeItem.tsx`

**Features:**
- Expandable/collapsible indicator (if has children)
- Location name, code, and description
- Status badge
- Child count badge
- Click to navigate to detail page
- Visual hierarchy with indentation

### Hierarchy Navigation Hook

The `useLocationHierarchy` hook manages hierarchy navigation state and provides navigation functions.

**Location:** `src/features/location-management/hooks/useLocationHierarchy.ts`

**Usage:**
```typescript
const {
  data,
  isLoading,
  error,
  navigationState,
  navigateToWarehouse,
  navigateToZone,
  navigateToAisle,
  navigateToRack,
  navigateToBin,
  navigateUp,
} = useLocationHierarchy();
```

**Features:**
- Manages current hierarchy level state
- Provides navigation functions for each level
- Uses React Query for caching (5-minute stale time)
- Handles loading and error states

### Breadcrumb Patterns for Hierarchies

Breadcrumbs should reflect the current hierarchy path:

```typescript
const getBreadcrumbItems = () => {
  const items = [
    { label: 'Dashboard', href: '/dashboard' },
    { label: 'Locations', href: Routes.locations },
  ];

  // Add parent location to breadcrumb if not at root level
  if (navigationState.level !== 'warehouse' && data?.parent) {
    const parentName = data.parent.name || data.parent.code || data.parent.barcode;
    items.push({ label: parentName });
  }

  return items;
};
```

### Mobile Responsiveness for Tree Views

Tree views should be mobile-responsive:

- **Desktop:** Full tree view with expand/collapse
- **Mobile:** Stack items vertically, full-width touch targets (minimum 44px height)
- Use `Stack` with `spacing={2}` for mobile layout
- Hide child count badges on mobile if space is limited
- Use `useMediaQuery` to adjust layout:

```typescript
const theme = useTheme();
const isMobile = useMediaQuery(theme.breakpoints.down('md'));

// Adjust layout based on screen size
```

### Hierarchy API Integration

Backend APIs follow RESTful hierarchy pattern:

- `GET /api/v1/location-management/locations/warehouses` - List warehouses
- `GET /api/v1/location-management/locations/warehouses/{warehouseId}/zones` - List zones
- `GET /api/v1/location-management/locations/zones/{zoneId}/aisles` - List aisles
- `GET /api/v1/location-management/locations/aisles/{aisleId}/racks` - List racks
- `GET /api/v1/location-management/locations/racks/{rackId}/bins` - List bins

Each endpoint returns:
- `parent: Location | null` - Parent location (null for root level)
- `items: LocationHierarchyItem[]` - Child locations with metadata
- `hierarchyLevel: string` - Current hierarchy level

### Best Practices

1. **Caching:** Use React Query with appropriate stale times (5 minutes for hierarchy data)
2. **Loading States:** Use SkeletonTable during initial load
3. **Error Handling:** Display user-friendly error messages
4. **Empty States:** Show EmptyState component when no items found
5. **Navigation:** Provide "Back" button when not at root level
6. **Accessibility:** Use proper ARIA labels for expand/collapse actions
7. **Performance:** Lazy load children only when parent is expanded/clicked

### Example: Complete Hierarchy Page

```typescript
export const LocationListPage = () => {
  const navigate = useNavigate();
  const {
    data,
    isLoading,
    error,
    navigationState,
    navigateToZone,
    navigateToAisle,
    navigateToRack,
    navigateToBin,
    navigateUp,
  } = useLocationHierarchy();

  const getBreadcrumbItems = () => {
    const items = [
      { label: 'Dashboard', href: '/dashboard' },
      { label: 'Locations', href: Routes.locations },
    ];
    if (navigationState.level !== 'warehouse' && data?.parent) {
      const parentName = data.parent.name || data.parent.code || data.parent.barcode;
      items.push({ label: parentName });
    }
    return items;
  };

  const getPageTitle = () => {
    switch (navigationState.level) {
      case 'warehouse': return 'Warehouses';
      case 'zone': return 'Zones';
      case 'aisle': return 'Aisles';
      case 'rack': return 'Racks';
      case 'bin': return 'Bins';
      default: return 'Locations';
    }
  };

  return (
    <ListPageLayout
      breadcrumbs={getBreadcrumbItems()}
      title={getPageTitle()}
      actions={
        <Stack direction="row" spacing={1}>
          {navigationState.level !== 'warehouse' && (
            <Button variant="outlined" startIcon={<ArrowBackIcon />} onClick={navigateUp}>
              Back
            </Button>
          )}
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate(Routes.locationCreate)}>
            Create Location
          </Button>
        </Stack>
      }
      isLoading={isLoading}
      error={error?.message || null}
    >
      <LocationTreeView
        data={data}
        isLoading={isLoading}
        error={error}
        level={navigationState.level}
        onExpand={(locationId) => {
          switch (navigationState.level) {
            case 'warehouse':
              navigateToZone(locationId);
              break;
            case 'zone':
              navigateToAisle(locationId);
              break;
            case 'aisle':
              navigateToRack(locationId);
              break;
            case 'rack':
              navigateToBin(locationId);
              break;
          }
        }}
      />
    </ListPageLayout>
  );
};
```

---

## New Components and Patterns (Sprint 05)

The following components and patterns were added during Sprint 05 - Frontend Production Hardening:

### New Layout Components

1. **DashboardPageLayout** - Standardized layout for dashboard pages (Admin/User dashboards)
   - Location: `src/components/layouts/DashboardPageLayout.tsx`
   - Usage: Replaces raw Container components in dashboard pages

### New Skeleton Loaders

1. **SkeletonTable** - Skeleton loader for table/list content
   - Location: `src/components/common/SkeletonTable.tsx`
   - Usage: Automatically used by ListPageLayout when loading

2. **SkeletonCard** - Skeleton loader for card/detail content
   - Location: `src/components/common/SkeletonCard.tsx`
   - Usage: Automatically used by DetailPageLayout when loading

3. **SkeletonForm** - Skeleton loader for form content
   - Location: `src/components/common/SkeletonForm.tsx`
   - Usage: Use when loading form default values (e.g., in edit pages)

### New Error Handling

1. **ErrorBoundaryWithRetry** - Error boundary with retry functionality
   - Location: `src/components/common/ErrorBoundaryWithRetry.tsx`
   - Usage: Wraps entire App to catch React errors gracefully
   - Features: Retry button, Go Home button, error logging

### New Hooks

1. **useToast** - Toast notification hook
   - Location: `src/hooks/useToast.ts`
   - Usage: Success, error, info, warning notifications
   - Integration: ToastContainer already configured in main.tsx

### Enhanced Patterns

1. **Optimistic UI Updates** - Update UI immediately, rollback on error
   - Pattern: Use React Query mutations with `onMutate` and `onError`
   - Example: Location status updates, product creation

2. **Network Error Recovery** - Automatic retry with exponential backoff
   - Implementation: Built into `apiClient.ts`
   - Features: 5xx errors retry (max 3), 429 rate limit handling, 401 token refresh

3. **Enhanced Async Validation** - Debounced validation with visual feedback
   - Pattern: Use `useDebounce` hook with CircularProgress indicator
   - Example: Product code uniqueness check

4. **Mobile Responsiveness** - Comprehensive responsive patterns
   - FormActions: Stacks vertically on mobile
   - ButtonGroups: Responsive orientation
   - FilterBar: Responsive direction
   - All action buttons: Full-width on mobile

5. **Capacity Visualization** - Color-coded progress bars
   - Pattern: LinearProgress with dynamic color based on utilization
   - Thresholds: <50% success, 50-80% warning, ≥80% error

6. **Expiry Alerts** - Dashboard alert cards
   - Pattern: Card component with conditional styling
   - Integration: ExpiryAlertCard component in UserDashboard

### Updated Components

1. **FormActions** - Now mobile-responsive (stacks on mobile)
2. **Pagination** - Custom component replaces MUI Pagination
3. **ListPageLayout** - Uses SkeletonTable instead of LoadingSpinner
4. **DetailPageLayout** - Uses SkeletonCard instead of LoadingSpinner
5. **FormPageLayout** - Enhanced ARIA attributes
6. **ActionDialog** - Enhanced ARIA attributes
7. **StatusBadge** - Uses MUI Chip (WCAG AA compliant colors)

### Accessibility Enhancements

1. **ARIA Labels** - Added to all form fields, buttons, dialogs
2. **Focus Indicators** - Custom `:focus-visible` styles in theme
3. **Live Regions** - ARIA live regions for dynamic content
4. **Keyboard Navigation** - Auto-focus, ESC handlers, tab order
5. **Color Contrast** - Verified WCAG AA compliance for all status colors

### Production Hardening Features

1. **Error Boundary** - Graceful error recovery with retry
2. **Optimistic Updates** - Better UX with immediate feedback
3. **Network Retry** - Automatic retry for transient failures
4. **Toast Notifications** - Non-intrusive user feedback
5. **Skeleton Loaders** - Content-aware loading states

---

**End of Document**

