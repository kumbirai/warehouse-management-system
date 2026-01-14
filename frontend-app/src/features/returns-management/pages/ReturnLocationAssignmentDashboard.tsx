import { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Button, Card, CardContent, Grid, IconButton, Paper, Typography } from '@mui/material';
import { AutoMode as AutoModeIcon } from '@mui/icons-material';
import { DashboardPageLayout } from '../../../components/layouts';
import {
  ResponsiveTable,
  Column,
  StatusBadge,
  getStatusVariant,
  SkeletonTable,
  EmptyState,
  PageBreadcrumbs,
} from '../../../components/common';
import { useReturns } from '../hooks/useReturns';
import { useAutoAssignReturnLocations } from '../hooks/useAutoAssignReturnLocations';
import { Return, ReturnStatus } from '../types/returns';
import { Routes, getBreadcrumbs } from '../../../utils/navigationUtils';
import { formatDateTime } from '../../../utils/dateUtils';

export const ReturnLocationAssignmentDashboard = () => {
  const navigate = useNavigate();
  const filters = useMemo(
    () => ({
      status: ReturnStatus.PROCESSED,
      page: 0,
      size: 100,
    }),
    []
  );

  const { data: returns, isLoading, error } = useReturns(filters);
  const { mutate: assignLocations, isPending: isAssigning } = useAutoAssignReturnLocations();

  const pendingReturns = useMemo(
    () => returns?.filter(r => r.status === ReturnStatus.PROCESSED) || [],
    [returns]
  );

  const assignedReturns = useMemo(
    () => returns?.filter(r => r.status === ReturnStatus.LOCATION_ASSIGNED) || [],
    [returns]
  );

  const handleAutoAssign = (returnId: string) => {
    assignLocations(
      { returnId },
      {
        onSuccess: () => {
          // Toast notification handled in hook
        },
      }
    );
  };

  const handleAutoAssignAll = () => {
    pendingReturns.forEach(returnItem => {
      handleAutoAssign(returnItem.returnId);
    });
  };

  const handleRowClick = (returnItem: Return) => {
    navigate(Routes.returnDetail(returnItem.returnId));
  };

  const columns: Column<Return>[] = [
    {
      key: 'returnId',
      label: 'Return ID',
      render: returnItem => (
        <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
          {returnItem.returnId.substring(0, 8)}...
        </Typography>
      ),
    },
    {
      key: 'orderNumber',
      label: 'Order Number',
      render: returnItem => <Typography variant="body2">{returnItem.orderNumber}</Typography>,
    },
    {
      key: 'returnType',
      label: 'Type',
      render: returnItem => (
        <Typography variant="body2">{returnItem.returnType.replace(/_/g, ' ')}</Typography>
      ),
    },
    {
      key: 'status',
      label: 'Status',
      render: returnItem => (
        <StatusBadge label={returnItem.status} variant={getStatusVariant(returnItem.status)} />
      ),
    },
    {
      key: 'lineItems',
      label: 'Line Items',
      render: returnItem => <Typography variant="body2">{returnItem.lineItems.length}</Typography>,
    },
    {
      key: 'createdAt',
      label: 'Created At',
      render: returnItem => (
        <Typography variant="body2">{formatDateTime(returnItem.createdAt)}</Typography>
      ),
      hideOnMobile: true,
    },
    {
      key: 'actions',
      label: 'Actions',
      hideOnMobile: true,
      render: returnItem => (
        <IconButton
          size="small"
          onClick={e => {
            e.stopPropagation();
            handleAutoAssign(returnItem.returnId);
          }}
          disabled={isAssigning}
          aria-label="Auto-assign locations"
        >
          <AutoModeIcon fontSize="small" />
        </IconButton>
      ),
    },
  ];

  const mobileCardRender = (returnItem: Return) => (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={1}>
        <Box>
          <Typography variant="subtitle2" fontWeight="bold">
            Return {returnItem.returnId.substring(0, 8)}...
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Order: {returnItem.orderNumber}
          </Typography>
        </Box>
        <StatusBadge label={returnItem.status} variant={getStatusVariant(returnItem.status)} />
      </Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mt={1}>
        <Typography variant="body2" color="text.secondary">
          {returnItem.lineItems.length} line items
        </Typography>
        <Button
          size="small"
          variant="outlined"
          startIcon={<AutoModeIcon />}
          onClick={e => {
            e.stopPropagation();
            handleAutoAssign(returnItem.returnId);
          }}
          disabled={isAssigning}
        >
          Assign
        </Button>
      </Box>
    </Box>
  );

  return (
    <DashboardPageLayout
      title="Return Location Assignment Dashboard"
      subtitle="Manage location assignments for returns"
    >
      <PageBreadcrumbs items={getBreadcrumbs.returnLocationAssignment()} />
      <Grid container spacing={3}>
        <Grid item xs={12} md={4}>
          <Card sx={{ bgcolor: 'warning.light' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Pending Assignment
              </Typography>
              <Typography variant="h3" color="warning.main" sx={{ fontWeight: 'bold' }}>
                {pendingReturns.length}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Returns awaiting location assignment
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={4}>
          <Card sx={{ bgcolor: 'success.light' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Assigned
              </Typography>
              <Typography variant="h3" color="success.main" sx={{ fontWeight: 'bold' }}>
                {assignedReturns.length}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Returns with locations assigned
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={4}>
          <Card sx={{ bgcolor: 'info.light' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Total Returns
              </Typography>
              <Typography variant="h3" color="info.main" sx={{ fontWeight: 'bold' }}>
                {returns?.length || 0}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                All returns in system
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12}>
          <Paper elevation={1} sx={{ p: 2 }}>
            <Box display="flex" justifyContent="space-between" alignItems="center">
              <Typography variant="h6">Actions</Typography>
              <Button
                variant="contained"
                onClick={handleAutoAssignAll}
                disabled={isAssigning || pendingReturns.length === 0}
                startIcon={<AutoModeIcon />}
              >
                Auto-Assign All Pending
              </Button>
            </Box>
          </Paper>
        </Grid>

        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Pending Returns
              </Typography>
              {isLoading ? (
                <SkeletonTable rows={5} columns={7} />
              ) : error ? (
                <Box sx={{ p: 3, textAlign: 'center' }}>
                  <Typography color="error">{error.message}</Typography>
                </Box>
              ) : pendingReturns.length === 0 ? (
                <EmptyState
                  title="No pending returns"
                  description="No returns are currently awaiting location assignment"
                />
              ) : (
                <ResponsiveTable
                  data={pendingReturns}
                  columns={columns}
                  getRowKey={returnItem => returnItem.returnId}
                  onRowClick={handleRowClick}
                  emptyMessage="No pending returns found"
                  mobileCardRender={mobileCardRender}
                />
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </DashboardPageLayout>
  );
};
