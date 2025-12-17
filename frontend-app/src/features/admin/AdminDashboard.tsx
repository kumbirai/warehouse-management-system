import {Box, Button, Card, CardContent, Container, Grid, Paper, Typography} from '@mui/material';
import {Link as RouterLink} from 'react-router-dom';
import {useAuth} from '../../hooks/useAuth';
import {Header} from '../../components/layout/Header';

/**
 * System Admin dashboard component.
 * Accessible only to users with SYSTEM_ADMIN role.
 */
export const AdminDashboard = () => {
  const { user } = useAuth();

  return (
    <>
      <Header />
      <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          System Administration Dashboard
        </Typography>
        <Typography variant="body1" color="text.secondary" gutterBottom>
          Welcome, {user?.firstName || user?.username}!
        </Typography>

        <Grid container spacing={3} sx={{ mt: 2 }}>
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Tenant Management
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Create, activate, and manage tenants (Local Distribution Partners).
                </Typography>
                <Button
                  sx={{ mt: 2 }}
                  size="small"
                  variant="contained"
                  component={RouterLink}
                  to="/admin/tenants"
                >
                  Manage Tenants
                </Button>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  User Management
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Manage user accounts, roles, and permissions across tenants.
                </Typography>
                <Button
                  sx={{ mt: 2 }}
                  size="small"
                  variant="contained"
                  component={RouterLink}
                  to="/admin/users"
                >
                  Manage Users
                </Button>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  System Configuration
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Configure system-wide settings and integrations.
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Monitoring & Analytics
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  View system health, performance metrics, and usage analytics.
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>

        <Paper sx={{ p: 3, mt: 4 }}>
          <Typography variant="h6" gutterBottom>
            User Information
          </Typography>
          <Box sx={{ mt: 2 }}>
            <Typography variant="body2">
              <strong>User ID:</strong> {user?.userId}
            </Typography>
            <Typography variant="body2">
              <strong>Username:</strong> {user?.username}
            </Typography>
            <Typography variant="body2">
              <strong>Email:</strong> {user?.email || 'N/A'}
            </Typography>
            <Typography variant="body2">
              <strong>Tenant ID:</strong> {user?.tenantId || 'N/A'}
            </Typography>
            <Typography variant="body2">
              <strong>Roles:</strong> {user?.roles?.join(', ') || 'N/A'}
            </Typography>
          </Box>
        </Paper>
      </Container>
    </>
  );
};
