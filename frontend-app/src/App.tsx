import { Navigate, Route, Routes } from 'react-router-dom';
import { LandingPage } from './features/home/LandingPage';
import { LoginPage } from './features/auth/LoginPage';
import { EmailVerificationPage } from './features/auth/pages/EmailVerificationPage';
import { PasswordSetupPage } from './features/auth/pages/PasswordSetupPage';
import { DashboardRouter } from './components/DashboardRouter';
import { ProtectedRoute } from './components/auth/ProtectedRoute';
import { UnauthorizedPage } from './components/auth/UnauthorizedPage';
import { ErrorBoundaryWithRetry } from './components/common/ErrorBoundaryWithRetry';
import { TenantListPage } from './features/tenant-management/pages/TenantListPage';
import { TenantCreatePage } from './features/tenant-management/pages/TenantCreatePage';
import { TenantDetailPage } from './features/tenant-management/pages/TenantDetailPage';
import { UserListPage } from './features/user-management/pages/UserListPage';
import { UserCreatePage } from './features/user-management/pages/UserCreatePage';
import { UserDetailPage } from './features/user-management/pages/UserDetailPage';
import { ConsignmentListPage } from './features/stock-management/pages/ConsignmentListPage';
import { CreateConsignmentPage } from './features/stock-management/pages/CreateConsignmentPage';
import { ConsignmentCsvUploadPage } from './features/stock-management/pages/ConsignmentCsvUploadPage';
import { ConsignmentDetailPage } from './features/stock-management/pages/ConsignmentDetailPage';
import { StockItemListPage } from './features/stock-management/pages/StockItemListPage';
import { StockItemDetailPage } from './features/stock-management/pages/StockItemDetailPage';
import { LocationListPage } from './features/location-management/pages/LocationListPage';
import { LocationCreatePage } from './features/location-management/pages/LocationCreatePage';
import { LocationDetailPage } from './features/location-management/pages/LocationDetailPage';
import { LocationEditPage } from './features/location-management/pages/LocationEditPage';
import { StockMovementListPage } from './features/location-management/pages/StockMovementListPage';
import { StockMovementCreatePage } from './features/location-management/pages/StockMovementCreatePage';
import { StockMovementDetailPage } from './features/location-management/pages/StockMovementDetailPage';
import { StockAllocationCreatePage } from './features/stock-management/pages/StockAllocationCreatePage';
import { StockAllocationListPage } from './features/stock-management/pages/StockAllocationListPage';
import { StockAllocationDetailPage } from './features/stock-management/pages/StockAllocationDetailPage';
import { StockAdjustmentCreatePage } from './features/stock-management/pages/StockAdjustmentCreatePage';
import { StockAdjustmentListPage } from './features/stock-management/pages/StockAdjustmentListPage';
import { StockAdjustmentDetailPage } from './features/stock-management/pages/StockAdjustmentDetailPage';
import { ProductListPage } from './features/product-management/pages/ProductListPage';
import { ProductCreatePage } from './features/product-management/pages/ProductCreatePage';
import { ProductDetailPage } from './features/product-management/pages/ProductDetailPage';
import { ProductEditPage } from './features/product-management/pages/ProductEditPage';
import { ProductCsvUploadPage } from './features/product-management/pages/ProductCsvUploadPage';
import { NotificationListPage } from './features/notification-management/pages/NotificationListPage';
import { NotificationDetailPage } from './features/notification-management/pages/NotificationDetailPage';
import { StockLevelsPage } from './features/stock-management/pages/StockLevelsPage';
import { FEFOAssignmentPage } from './features/location-management/pages/FEFOAssignmentPage';
import {
  LOCATION_MANAGER,
  OPERATOR,
  STOCK_CLERK,
  STOCK_MANAGER,
  SYSTEM_ADMIN,
  TENANT_ADMIN,
  USER,
  VIEWER,
  WAREHOUSE_MANAGER,
} from './constants/roles';

function App() {
  return (
    <ErrorBoundaryWithRetry>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/verify-email" element={<EmailVerificationPage />} />
        <Route path="/setup-password" element={<PasswordSetupPage />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <DashboardRouter />
            </ProtectedRoute>
          }
        />
        <Route path="/unauthorized" element={<UnauthorizedPage />} />
        <Route
          path="/admin/tenants"
          element={
            <ProtectedRoute requiredRole={SYSTEM_ADMIN}>
              <TenantListPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/tenants/create"
          element={
            <ProtectedRoute requiredRole={SYSTEM_ADMIN}>
              <TenantCreatePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/tenants/:tenantId"
          element={
            <ProtectedRoute requiredRole={SYSTEM_ADMIN}>
              <TenantDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/users"
          element={
            <ProtectedRoute requiredRoles={[SYSTEM_ADMIN, TENANT_ADMIN]}>
              <UserListPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/users/create"
          element={
            <ProtectedRoute requiredRoles={[SYSTEM_ADMIN, TENANT_ADMIN]}>
              <UserCreatePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/users/:userId"
          element={
            <ProtectedRoute requiredRoles={[SYSTEM_ADMIN, TENANT_ADMIN, USER]}>
              <UserDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/stock-management/consignments"
          element={
            <ProtectedRoute
              requiredRoles={[
                SYSTEM_ADMIN,
                TENANT_ADMIN,
                WAREHOUSE_MANAGER,
                STOCK_MANAGER,
                OPERATOR,
                STOCK_CLERK,
                VIEWER,
              ]}
            >
              <ConsignmentListPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/stock-management/consignments/create"
          element={
            <ProtectedRoute
              requiredRoles={[
                SYSTEM_ADMIN,
                TENANT_ADMIN,
                WAREHOUSE_MANAGER,
                STOCK_MANAGER,
                OPERATOR,
                STOCK_CLERK,
              ]}
            >
              <CreateConsignmentPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/stock-management/consignments/upload-csv"
          element={
            <ProtectedRoute
              requiredRoles={[
                SYSTEM_ADMIN,
                TENANT_ADMIN,
                WAREHOUSE_MANAGER,
                STOCK_MANAGER,
                OPERATOR,
                STOCK_CLERK,
              ]}
            >
              <ConsignmentCsvUploadPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/stock-management/consignments/:consignmentId"
          element={
            <ProtectedRoute
              requiredRoles={[
                SYSTEM_ADMIN,
                TENANT_ADMIN,
                WAREHOUSE_MANAGER,
                STOCK_MANAGER,
                OPERATOR,
                STOCK_CLERK,
              ]}
            >
              <ConsignmentDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/locations"
          element={
            <ProtectedRoute
              requiredRoles={[
                SYSTEM_ADMIN,
                TENANT_ADMIN,
                WAREHOUSE_MANAGER,
                LOCATION_MANAGER,
                OPERATOR,
                STOCK_CLERK,
                VIEWER,
              ]}
            >
              <LocationListPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/locations/create"
          element={
            <ProtectedRoute
              requiredRoles={[SYSTEM_ADMIN, TENANT_ADMIN, WAREHOUSE_MANAGER, LOCATION_MANAGER]}
            >
              <LocationCreatePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/locations/:locationId/edit"
          element={
            <ProtectedRoute
              requiredRoles={[SYSTEM_ADMIN, TENANT_ADMIN, WAREHOUSE_MANAGER, LOCATION_MANAGER]}
            >
              <LocationEditPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/locations/:locationId"
          element={
            <ProtectedRoute
              requiredRoles={[
                SYSTEM_ADMIN,
                TENANT_ADMIN,
                WAREHOUSE_MANAGER,
                LOCATION_MANAGER,
                OPERATOR,
                STOCK_CLERK,
                VIEWER,
              ]}
            >
              <LocationDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/products"
          element={
            <ProtectedRoute
              requiredRoles={[
                SYSTEM_ADMIN,
                TENANT_ADMIN,
                WAREHOUSE_MANAGER,
                STOCK_MANAGER,
                LOCATION_MANAGER,
                OPERATOR,
                STOCK_CLERK,
                VIEWER,
              ]}
            >
              <ProductListPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/products/create"
          element={
            <ProtectedRoute requiredRoles={[SYSTEM_ADMIN, TENANT_ADMIN]}>
              <ProductCreatePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/products/:productId"
          element={
            <ProtectedRoute
              requiredRoles={[
                SYSTEM_ADMIN,
                TENANT_ADMIN,
                WAREHOUSE_MANAGER,
                STOCK_MANAGER,
                LOCATION_MANAGER,
                OPERATOR,
                STOCK_CLERK,
                VIEWER,
              ]}
            >
              <ProductDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/products/:productId/edit"
          element={
            <ProtectedRoute requiredRoles={[SYSTEM_ADMIN, TENANT_ADMIN, WAREHOUSE_MANAGER]}>
              <ProductEditPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/products/upload-csv"
          element={
            <ProtectedRoute requiredRoles={[SYSTEM_ADMIN, TENANT_ADMIN, WAREHOUSE_MANAGER]}>
              <ProductCsvUploadPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/stock-management/stock-items"
          element={
            <ProtectedRoute
              requiredRoles={[
                SYSTEM_ADMIN,
                TENANT_ADMIN,
                WAREHOUSE_MANAGER,
                STOCK_MANAGER,
                LOCATION_MANAGER,
                OPERATOR,
                STOCK_CLERK,
                VIEWER,
              ]}
            >
              <StockItemListPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/stock-management/stock-items/:stockItemId"
          element={
            <ProtectedRoute
              requiredRoles={[
                SYSTEM_ADMIN,
                TENANT_ADMIN,
                WAREHOUSE_MANAGER,
                STOCK_MANAGER,
                LOCATION_MANAGER,
                OPERATOR,
                STOCK_CLERK,
                VIEWER,
              ]}
            >
              <StockItemDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/stock-movements"
          element={
            <ProtectedRoute
              requiredRoles={[
                SYSTEM_ADMIN,
                TENANT_ADMIN,
                WAREHOUSE_MANAGER,
                LOCATION_MANAGER,
                STOCK_MANAGER,
                OPERATOR,
                STOCK_CLERK,
                VIEWER,
              ]}
            >
              <StockMovementListPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/stock-movements/create"
          element={
            <ProtectedRoute
              requiredRoles={[
                SYSTEM_ADMIN,
                TENANT_ADMIN,
                WAREHOUSE_MANAGER,
                LOCATION_MANAGER,
                STOCK_MANAGER,
                OPERATOR,
                STOCK_CLERK,
              ]}
            >
              <StockMovementCreatePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/stock-movements/:movementId"
          element={
            <ProtectedRoute
              requiredRoles={[
                SYSTEM_ADMIN,
                TENANT_ADMIN,
                WAREHOUSE_MANAGER,
                LOCATION_MANAGER,
                STOCK_MANAGER,
                OPERATOR,
                STOCK_CLERK,
                VIEWER,
              ]}
            >
              <StockMovementDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/stock-management/allocations/create"
          element={
            <ProtectedRoute
              requiredRoles={[
                SYSTEM_ADMIN,
                TENANT_ADMIN,
                WAREHOUSE_MANAGER,
                STOCK_MANAGER,
                OPERATOR,
                STOCK_CLERK,
              ]}
            >
              <StockAllocationCreatePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/stock-management/adjustments/create"
          element={
            <ProtectedRoute
              requiredRoles={[
                SYSTEM_ADMIN,
                TENANT_ADMIN,
                WAREHOUSE_MANAGER,
                STOCK_MANAGER,
                OPERATOR,
              ]}
            >
              <StockAdjustmentCreatePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/stock-management/allocations"
          element={
            <ProtectedRoute
              requiredRoles={[
                SYSTEM_ADMIN,
                TENANT_ADMIN,
                WAREHOUSE_MANAGER,
                STOCK_MANAGER,
                OPERATOR,
                STOCK_CLERK,
                VIEWER,
              ]}
            >
              <StockAllocationListPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/stock-management/allocations/:allocationId"
          element={
            <ProtectedRoute
              requiredRoles={[
                SYSTEM_ADMIN,
                TENANT_ADMIN,
                WAREHOUSE_MANAGER,
                STOCK_MANAGER,
                OPERATOR,
                STOCK_CLERK,
                VIEWER,
              ]}
            >
              <StockAllocationDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/stock-management/adjustments"
          element={
            <ProtectedRoute
              requiredRoles={[
                SYSTEM_ADMIN,
                TENANT_ADMIN,
                WAREHOUSE_MANAGER,
                STOCK_MANAGER,
                OPERATOR,
                VIEWER,
              ]}
            >
              <StockAdjustmentListPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/stock-management/adjustments/:adjustmentId"
          element={
            <ProtectedRoute
              requiredRoles={[
                SYSTEM_ADMIN,
                TENANT_ADMIN,
                WAREHOUSE_MANAGER,
                STOCK_MANAGER,
                OPERATOR,
                VIEWER,
              ]}
            >
              <StockAdjustmentDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/notifications"
          element={
            <ProtectedRoute requiredRoles={[SYSTEM_ADMIN, TENANT_ADMIN, USER]}>
              <NotificationListPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/notifications/:id"
          element={
            <ProtectedRoute requiredRoles={[SYSTEM_ADMIN, TENANT_ADMIN, USER]}>
              <NotificationDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/stock-management/stock-levels"
          element={
            <ProtectedRoute
              requiredRoles={[
                SYSTEM_ADMIN,
                TENANT_ADMIN,
                WAREHOUSE_MANAGER,
                STOCK_MANAGER,
                OPERATOR,
                STOCK_CLERK,
                VIEWER,
              ]}
            >
              <StockLevelsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/locations/assign-fefo"
          element={
            <ProtectedRoute
              requiredRoles={[
                SYSTEM_ADMIN,
                TENANT_ADMIN,
                WAREHOUSE_MANAGER,
                LOCATION_MANAGER,
                STOCK_MANAGER,
              ]}
            >
              <FEFOAssignmentPage />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </ErrorBoundaryWithRetry>
  );
}

export default App;
