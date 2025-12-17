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
          <ProtectedRoute requiredRole="SYSTEM_ADMIN">
            <TenantListPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/tenants/create"
        element={
          <ProtectedRoute requiredRole="SYSTEM_ADMIN">
            <TenantCreatePage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/tenants/:tenantId"
        element={
          <ProtectedRoute requiredRole="SYSTEM_ADMIN">
            <TenantDetailPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/users"
        element={
          <ProtectedRoute requiredRoles={['SYSTEM_ADMIN', 'TENANT_ADMIN']}>
            <UserListPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/users/create"
        element={
          <ProtectedRoute requiredRoles={['SYSTEM_ADMIN', 'TENANT_ADMIN']}>
            <UserCreatePage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/users/:userId"
        element={
          <ProtectedRoute requiredRoles={['SYSTEM_ADMIN', 'TENANT_ADMIN', 'USER']}>
            <UserDetailPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/stock-management/consignments/create"
        element={
          <ProtectedRoute requiredRoles={['ADMIN', 'MANAGER', 'OPERATOR']}>
            <CreateConsignmentPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/stock-management/consignments/upload-csv"
        element={
          <ProtectedRoute requiredRoles={['ADMIN', 'MANAGER', 'OPERATOR']}>
            <ConsignmentCsvUploadPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/stock-management/consignments/:consignmentId"
        element={
          <ProtectedRoute requiredRoles={['ADMIN', 'MANAGER', 'OPERATOR']}>
            <ConsignmentDetailPage />
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
