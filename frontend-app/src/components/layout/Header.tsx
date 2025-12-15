import { AppBar, Box, Button, Toolbar, Typography } from '@mui/material';
import { Link as RouterLink, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

export function Header() {
  const { isAuthenticated, user, logout, isSystemAdmin } = useAuth();
  const location = useLocation();

  const handleLogout = () => {
    logout();
  };

  // Don't show dashboard link if already on dashboard
  const showDashboardLink = isAuthenticated && location.pathname !== '/dashboard';

  return (
    <AppBar position="static">
      <Toolbar>
        <Typography
          variant="h6"
          component={isAuthenticated ? RouterLink : 'div'}
          to={isAuthenticated ? '/dashboard' : undefined}
          sx={{
            flexGrow: 1,
            textDecoration: 'none',
            color: 'inherit',
            cursor: isAuthenticated ? 'pointer' : 'default',
            '&:hover': isAuthenticated ? { opacity: 0.8 } : {},
          }}
        >
          Warehouse Management System
        </Typography>
        {isAuthenticated && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            {showDashboardLink && (
              <Button color="inherit" component={RouterLink} to="/dashboard">
                Dashboard
              </Button>
            )}
            {isSystemAdmin() && (
              <Button color="inherit" component={RouterLink} to="/admin/tenants">
                Tenants
              </Button>
            )}
            <Typography variant="body2">{user?.firstName || user?.username}</Typography>
            <Button color="inherit" onClick={handleLogout}>
              Logout
            </Button>
          </Box>
        )}
      </Toolbar>
    </AppBar>
  );
}
