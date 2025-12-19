import {Navigate} from 'react-router-dom';
import {useAuth} from '../hooks/useAuth';
import {AdminDashboard} from '../features/admin/AdminDashboard';
import {UserDashboard} from '../features/user/UserDashboard';

/**
 * Dashboard router component.
 * Routes users to appropriate dashboard based on their role.
 */
export const DashboardRouter = () => {
  const { isSystemAdmin, isUser } = useAuth();

  if (isSystemAdmin()) {
    return <AdminDashboard />;
  }

  if (isUser()) {
    return <UserDashboard />;
  }

  // Default fallback - redirect to landing page if no matching role
  return <Navigate to="/" replace />;
};
