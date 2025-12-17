import {Box, Button, Card, CardContent, Container, Grid, Paper, Typography} from '@mui/material';
import {Link as RouterLink} from 'react-router-dom';
import {useAuth} from '../../hooks/useAuth';
import {Header} from '../../components/layout/Header';

/**
 * Tenant User dashboard component.
 * Accessible to users with USER role.
 */
export const UserDashboard = () => {
  const { user, isTenantAdmin } = useAuth();

  return (
    <>
      <Header />
      <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Warehouse Operations Dashboard
        </Typography>
        <Typography variant="body1" color="text.secondary" gutterBottom>
          Welcome, {user?.firstName || user?.username}!
        </Typography>

        <Grid container spacing={3} sx={{ mt: 2 }}>
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Stock Count
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Perform daily stock counts and reconcile inventory.
                </Typography>
                <Button sx={{ mt: 2 }} size="small" variant="outlined" disabled>
                  Coming Soon
                </Button>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Picking Tasks
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  View and execute picking lists for orders.
                </Typography>
                <Button sx={{ mt: 2 }} size="small" variant="outlined" disabled>
                  Coming Soon
                </Button>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Consignments
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Receive and confirm incoming stock consignments.
                </Typography>
                <Box sx={{ mt: 2, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                  <Button
                    size="small"
                    variant="contained"
                    component={RouterLink}
                    to="/stock-management/consignments/create"
                  >
                    Create Consignment
                  </Button>
                  <Button
                    size="small"
                    variant="outlined"
                    component={RouterLink}
                    to="/stock-management/consignments/upload-csv"
                  >
                    Upload CSV
                  </Button>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Returns
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Process returns and manage return documentation.
                </Typography>
                <Button sx={{ mt: 2 }} size="small" variant="outlined" disabled>
                  Coming Soon
                </Button>
              </CardContent>
            </Card>
          </Grid>
          {isTenantAdmin() && (
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    User Management
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Manage user accounts, roles, and permissions within your tenant.
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
          )}
        </Grid>

        <Paper sx={{ p: 3, mt: 4 }}>
          <Typography variant="h6" gutterBottom>
            Your Information
          </Typography>
          <Box sx={{ mt: 2 }}>
            <Typography variant="body2">
              <strong>Username:</strong> {user?.username}
            </Typography>
            <Typography variant="body2">
              <strong>Email:</strong> {user?.email || 'N/A'}
            </Typography>
            <Typography variant="body2">
              <strong>Tenant:</strong> {user?.tenantId || 'N/A'}
            </Typography>
          </Box>
        </Paper>
      </Container>
    </>
  );
};
