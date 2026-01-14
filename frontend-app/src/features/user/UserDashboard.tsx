import { Box, Button, Card, CardContent, Grid, Paper, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { DashboardPageLayout } from '../../components/layouts';
import { ExpiryAlertCard } from '../stock-management/components/ExpiryAlertCard';
import { Routes } from '../../utils/navigationUtils';

/**
 * Tenant User dashboard component.
 * Accessible to users with USER role.
 */
export const UserDashboard = () => {
  const { user, isTenantAdmin } = useAuth();

  return (
    <DashboardPageLayout
      title="Warehouse Operations Dashboard"
      subtitle={`Welcome, ${user?.firstName || user?.username}!`}
    >
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
              <Button
                sx={{ mt: 2 }}
                size="small"
                variant="contained"
                component={RouterLink}
                to={Routes.pickingLists}
              >
                View Picking Lists
              </Button>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Location Management
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Create and manage warehouse locations with barcode support.
              </Typography>
              <Box sx={{ mt: 2, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                <Button
                  size="small"
                  variant="contained"
                  component={RouterLink}
                  to={Routes.locationCreate}
                >
                  Create Location
                </Button>
                <Button
                  size="small"
                  variant="outlined"
                  component={RouterLink}
                  to={Routes.locations}
                >
                  View Locations
                </Button>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Product Management
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Create products manually or upload product master data via CSV file.
              </Typography>
              <Box sx={{ mt: 2, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                <Button
                  size="small"
                  variant="contained"
                  component={RouterLink}
                  to={Routes.productCreate}
                >
                  Create Product
                </Button>
                <Button size="small" variant="outlined" component={RouterLink} to={Routes.products}>
                  View Products
                </Button>
              </Box>
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
                  to={Routes.consignmentCreate}
                >
                  Create Consignment
                </Button>
                <Button
                  size="small"
                  variant="outlined"
                  component={RouterLink}
                  to={Routes.consignments}
                >
                  View Consignments
                </Button>
                <Button
                  size="small"
                  variant="outlined"
                  component={RouterLink}
                  to={Routes.consignmentUploadCsv}
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
                Stock Items
              </Typography>
              <Typography variant="body2" color="text.secondary">
                View stock items with classification and location information. Filter by
                classification to manage inventory effectively.
              </Typography>
              <Box sx={{ mt: 2, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                <Button
                  size="small"
                  variant="contained"
                  component={RouterLink}
                  to={Routes.stockItems}
                >
                  View Stock Items
                </Button>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={6}>
          <ExpiryAlertCard />
        </Grid>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Stock Levels
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Monitor stock levels with min/max threshold alerts and capacity utilization.
              </Typography>
              <Button
                sx={{ mt: 2 }}
                size="small"
                variant="contained"
                component={RouterLink}
                to={Routes.stockLevels}
              >
                View Stock Levels
              </Button>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Expiring Stock
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Monitor stock expiration dates and generate alerts for expiring inventory.
              </Typography>
              <Button
                sx={{ mt: 2 }}
                size="small"
                variant="contained"
                component={RouterLink}
                to={Routes.expiringStock}
              >
                View Expiring Stock
              </Button>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Restock Requests
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Monitor and manage automated restock requests when stock falls below minimum levels.
              </Typography>
              <Button
                sx={{ mt: 2 }}
                size="small"
                variant="contained"
                component={RouterLink}
                to={Routes.restockRequests}
              >
                View Restock Requests
              </Button>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Returns Management
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Process returns, record damage assessments, assign locations, and reconcile with
                D365.
              </Typography>
              <Box sx={{ mt: 2, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                <Button
                  size="small"
                  variant="contained"
                  component={RouterLink}
                  to={Routes.partialOrderAcceptance}
                >
                  Partial Acceptance
                </Button>
                <Button
                  size="small"
                  variant="contained"
                  component={RouterLink}
                  to={Routes.fullOrderReturn}
                >
                  Full Return
                </Button>
                <Button
                  size="small"
                  variant="outlined"
                  component={RouterLink}
                  to={Routes.damageAssessment}
                >
                  Damage Assessment
                </Button>
                <Button
                  size="small"
                  variant="outlined"
                  component={RouterLink}
                  to={Routes.returnLocationAssignment}
                >
                  Location Assignment
                </Button>
                <Button
                  size="small"
                  variant="outlined"
                  component={RouterLink}
                  to={Routes.returnsReconciliation}
                >
                  D365 Reconciliation
                </Button>
              </Box>
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
                  to={Routes.admin.users}
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
    </DashboardPageLayout>
  );
};
