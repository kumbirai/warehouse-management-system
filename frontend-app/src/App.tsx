import { Navigate, Route, Routes } from 'react-router-dom';
import { LandingPage } from './features/home/LandingPage';
import { LoginPage } from './features/auth/LoginPage';
import { EmailVerificationPage } from './features/auth/pages/EmailVerificationPage';
import { PasswordSetupPage } from './features/auth/pages/PasswordSetupPage';
import { DashboardRouter } from './components/DashboardRouter';
import { ProtectedRoute } from './components/auth/ProtectedRoute';
import { UnauthorizedPage } from './components/auth/UnauthorizedPage';
import { TenantListPage } from './features/tenant-management/pages/TenantListPage';
import { TenantCreatePage } from './features/tenant-management/pages/TenantCreatePage';
import { TenantDetailPage } from './features/tenant-management/pages/TenantDetailPage';
import { UserListPage } from './features/user-management/pages/UserListPage';
import { UserCreatePage } from './features/user-management/pages/UserCreatePage';
import { UserDetailPage } from './features/user-management/pages/UserDetailPage';
import { CreateConsignmentPage } from './features/stock-management/pages/CreateConsignmentPage';
import { ConsignmentCsvUploadPage } from './features/stock-management/pages/ConsignmentCsvUploadPage';
import { ConsignmentDetailPage } from './features/stock-management/pages/ConsignmentDetailPage';
import { LocationListPage } from './features/location-management/pages/LocationListPage';
import { LocationCreatePage } from './features/location-management/pages/LocationCreatePage';
import { LocationDetailPage } from './features/location-management/pages/LocationDetailPage';
import { ProductListPage } from './features/product-management/pages/ProductListPage';
import { ProductCreatePage } from './features/product-management/pages/ProductCreatePage';
import { ProductDetailPage } from './features/product-management/pages/ProductDetailPage';
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
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
