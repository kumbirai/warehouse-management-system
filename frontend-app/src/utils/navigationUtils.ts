/**
 * Navigation utilities for consistent routing across the application.
 */

/**
 * Application routes
 */
export const Routes = {
  // Public routes
  login: '/login',
  unauthorized: '/unauthorized',

  // Dashboard
  dashboard: '/dashboard',

  // Admin routes (System Admin only)
  admin: {
    tenants: '/admin/tenants',
    tenantDetail: (id: string) => `/admin/tenants/${id}`,
    tenantCreate: '/admin/tenants/create',
    tenantEdit: (id: string) => `/admin/tenants/${id}/edit`,

    users: '/admin/users',
    userDetail: (id: string) => `/admin/users/${id}`,
    userCreate: '/admin/users/create',
    userEdit: (id: string) => `/admin/users/${id}/edit`,
  },

  // Tenant routes
  locations: '/locations',
  locationDetail: (id: string) => `/locations/${id}`,
  locationCreate: '/locations/create',
  locationEdit: (id: string) => `/locations/${id}/edit`,

  products: '/products',
  productDetail: (id: string) => `/products/${id}`,
  productCreate: '/products/create',
  productEdit: (id: string) => `/products/${id}/edit`,

  consignments: '/stock-management/consignments',
  consignmentDetail: (id: string) => `/stock-management/consignments/${id}`,
  consignmentCreate: '/stock-management/consignments/create',
  consignmentUploadCsv: '/stock-management/consignments/upload-csv',

  // Stock Items
  stockItems: '/stock-management/stock-items',
  stockItemDetail: (id: string) => `/stock-management/stock-items/${id}`,

  // Stock Movements
  stockMovements: '/stock-movements',
  stockMovementDetail: (id: string) => `/stock-movements/${id}`,
  stockMovementCreate: '/stock-movements/create',

  // Stock Allocations
  stockAllocations: '/stock-management/allocations',
  stockAllocationDetail: (id: string) => `/stock-management/allocations/${id}`,
  stockAllocationCreate: '/stock-management/allocations/create',

  // Stock Adjustments
  stockAdjustments: '/stock-management/adjustments',
  stockAdjustmentDetail: (id: string) => `/stock-management/adjustments/${id}`,
  stockAdjustmentCreate: '/stock-management/adjustments/create',

  // Future routes (placeholder)
  inventory: '/inventory',
  picking: '/picking',
  returns: '/returns',
  reconciliation: '/reconciliation',
  reports: '/reports',
};

/**
 * Breadcrumb configurations for different pages
 */
export const getBreadcrumbs = {
  // Tenant Management
  tenantList: () => [{ label: 'Dashboard', href: Routes.dashboard }, { label: 'Tenants' }],
  tenantDetail: (tenantName: string) => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Tenants', href: Routes.admin.tenants },
    { label: tenantName },
  ],
  tenantCreate: () => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Tenants', href: Routes.admin.tenants },
    { label: 'Create Tenant' },
  ],

  // User Management
  userList: () => [{ label: 'Dashboard', href: Routes.dashboard }, { label: 'Users' }],
  userDetail: (userName: string) => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Users', href: Routes.admin.users },
    { label: userName },
  ],
  userCreate: () => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Users', href: Routes.admin.users },
    { label: 'Create User' },
  ],

  // Location Management
  locationList: () => [{ label: 'Dashboard', href: Routes.dashboard }, { label: 'Locations' }],
  locationDetail: (locationCode: string) => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Locations', href: Routes.locations },
    { label: locationCode },
  ],
  locationCreate: () => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Locations', href: Routes.locations },
    { label: 'Create Location' },
  ],
  locationEdit: () => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Locations', href: Routes.locations },
    { label: 'Edit Location' },
  ],

  // Product Management
  productList: () => [{ label: 'Dashboard', href: Routes.dashboard }, { label: 'Products' }],
  productDetail: (productCode: string) => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Products', href: Routes.products },
    { label: productCode },
  ],
  productCreate: () => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Products', href: Routes.products },
    { label: 'Create Product' },
  ],
  productEdit: () => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Products', href: Routes.products },
    { label: 'Edit Product' },
  ],

  // Consignment Management
  consignmentList: () => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Consignments' },
  ],
  consignmentDetail: (reference: string) => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Consignments', href: Routes.consignments },
    { label: reference },
  ],
  consignmentCreate: () => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Consignments', href: Routes.consignments },
    { label: 'Create Consignment' },
  ],
  consignmentUploadCsv: () => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Consignments', href: Routes.consignments },
    { label: 'Upload CSV' },
  ],

  // Stock Item Management
  stockItemList: () => [{ label: 'Dashboard', href: Routes.dashboard }, { label: 'Stock Items' }],
  stockItemDetail: (stockItemId: string) => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Stock Items', href: Routes.stockItems },
    { label: stockItemId.substring(0, 8) + '...' },
  ],

  // Stock Movement Management
  stockMovementList: () => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Stock Movements' },
  ],
  stockMovementDetail: (movementId: string) => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Stock Movements', href: Routes.stockMovements },
    { label: movementId.substring(0, 8) + '...' },
  ],
  stockMovementCreate: () => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Stock Movements', href: Routes.stockMovements },
    { label: 'Create Movement' },
  ],

  // Stock Allocation Management
  stockAllocationList: () => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Stock Allocations' },
  ],
  stockAllocationDetail: (allocationId: string) => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Stock Allocations', href: Routes.stockAllocations },
    { label: allocationId.substring(0, 8) + '...' },
  ],
  stockAllocationCreate: () => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Stock Allocations', href: Routes.stockAllocations },
    { label: 'Allocate Stock' },
  ],

  // Stock Adjustment Management
  stockAdjustmentList: () => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Stock Adjustments' },
  ],
  stockAdjustmentDetail: (adjustmentId: string) => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Stock Adjustments', href: Routes.stockAdjustments },
    { label: adjustmentId.substring(0, 8) + '...' },
  ],
  stockAdjustmentCreate: () => [
    { label: 'Dashboard', href: Routes.dashboard },
    { label: 'Stock Adjustments', href: Routes.stockAdjustments },
    { label: 'Adjust Stock' },
  ],
};

/**
 * Check if a route requires admin privileges
 */
export const isAdminRoute = (path: string): boolean => {
  return path.startsWith('/admin');
};

/**
 * Check if a route is public (no authentication required)
 */
export const isPublicRoute = (path: string): boolean => {
  const publicRoutes = [Routes.login, Routes.unauthorized];
  return publicRoutes.includes(path);
};

/**
 * Get the list page route for a given detail/edit route
 */
export const getListPageRoute = (currentPath: string): string => {
  if (currentPath.includes('/admin/tenants')) return Routes.admin.tenants;
  if (currentPath.includes('/admin/users')) return Routes.admin.users;
  if (currentPath.includes('/locations')) return Routes.locations;
  if (currentPath.includes('/products')) return Routes.products;
  if (
    currentPath.includes('/stock-management/consignments') ||
    currentPath.includes('/consignments')
  )
    return Routes.consignments;
  if (currentPath.includes('/stock-management/stock-items') || currentPath.includes('/stock-items'))
    return Routes.stockItems;
  return Routes.dashboard;
};
