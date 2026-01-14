import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardContent,
  FormControl,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Typography,
  Tooltip,
} from '@mui/material';
import {
  Refresh as RefreshIcon,
  History as HistoryIcon,
  Error as ErrorIcon,
} from '@mui/icons-material';
import { DashboardPageLayout } from '../../../components/layouts';
import {
  ResponsiveTable,
  Column,
  StatusBadge,
  getStatusVariant,
  SkeletonTable,
  EmptyState,
  FilterBar,
  Pagination,
  PageBreadcrumbs,
} from '../../../components/common';
import { useReconciliationRecords } from '../hooks/useReconciliationRecords';
import { useRetryD365Sync } from '../hooks/useRetryD365Sync';
import { ReconciliationRecord, D365ReconciliationStatus } from '../types/reconciliation';
import { Routes, getBreadcrumbs } from '../../../utils/navigationUtils';
import { formatDateTime } from '../../../utils/dateUtils';

export const ReturnsReconciliationDashboard = () => {
  const navigate = useNavigate();
  const [statusFilter, setStatusFilter] = useState<D365ReconciliationStatus | ''>('');
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 20;

  const filters = useMemo(
    () => ({
      status: statusFilter || undefined,
      page: currentPage - 1,
      size: pageSize,
    }),
    [statusFilter, currentPage]
  );

  const { data: records, isLoading, error } = useReconciliationRecords(filters);
  const { mutate: retrySync, isPending: isRetrying } = useRetryD365Sync();

  const summary = useMemo(() => {
    if (!records) {
      return {
        totalReturns: 0,
        pendingReconciliation: 0,
        inProgress: 0,
        successful: 0,
        failed: 0,
        retrying: 0,
      };
    }

    return {
      totalReturns: records.length,
      pendingReconciliation: records.filter(
        r => r.reconciliationStatus === D365ReconciliationStatus.PENDING
      ).length,
      inProgress: records.filter(
        r => r.reconciliationStatus === D365ReconciliationStatus.IN_PROGRESS
      ).length,
      successful: records.filter(r => r.reconciliationStatus === D365ReconciliationStatus.SUCCESS)
        .length,
      failed: records.filter(r => r.reconciliationStatus === D365ReconciliationStatus.FAILED)
        .length,
      retrying: records.filter(r => r.reconciliationStatus === D365ReconciliationStatus.RETRYING)
        .length,
    };
  }, [records]);

  const handleRetrySync = (returnId: string) => {
    retrySync(returnId, {
      onSuccess: () => {
        // Toast notification handled in hook
      },
    });
  };

  const handleClearFilters = () => {
    setStatusFilter('');
    setCurrentPage(1);
  };

  const handleRowClick = (record: ReconciliationRecord) => {
    navigate(Routes.returnDetail(record.returnId));
  };

  const columns: Column<ReconciliationRecord>[] = [
    {
      key: 'returnId',
      label: 'Return ID',
      render: record => (
        <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
          {record.returnId.substring(0, 8)}...
        </Typography>
      ),
    },
    {
      key: 'orderNumber',
      label: 'Order Number',
      render: record => <Typography variant="body2">{record.orderNumber}</Typography>,
    },
    {
      key: 'd365ReturnOrderId',
      label: 'D365 Return Order ID',
      render: record => (
        <Typography variant="body2" color="text.secondary">
          {record.d365ReturnOrderId || 'N/A'}
        </Typography>
      ),
      hideOnMobile: true,
    },
    {
      key: 'status',
      label: 'Status',
      render: record => (
        <StatusBadge
          label={record.reconciliationStatus}
          variant={getStatusVariant(record.reconciliationStatus)}
        />
      ),
    },
    {
      key: 'inventoryAdjusted',
      label: 'Inventory Adjusted',
      render: record => (
        <Typography variant="body2">{record.inventoryAdjusted ? 'Yes' : 'No'}</Typography>
      ),
      hideOnMobile: true,
    },
    {
      key: 'creditNoteId',
      label: 'Credit Note ID',
      render: record => (
        <Typography variant="body2" color="text.secondary">
          {record.creditNoteId || 'N/A'}
        </Typography>
      ),
      hideOnMobile: true,
    },
    {
      key: 'writeOffProcessed',
      label: 'Write-Off Processed',
      render: record => (
        <Typography variant="body2">{record.writeOffProcessed ? 'Yes' : 'No'}</Typography>
      ),
      hideOnMobile: true,
    },
    {
      key: 'lastSyncAttempt',
      label: 'Last Sync',
      render: record => (
        <Typography variant="body2">
          {record.lastSyncAttempt ? formatDateTime(record.lastSyncAttempt) : 'N/A'}
        </Typography>
      ),
      hideOnMobile: true,
    },
    {
      key: 'actions',
      label: 'Actions',
      hideOnMobile: true,
      render: record => (
        <Box display="flex" gap={1}>
          {record.reconciliationStatus === D365ReconciliationStatus.FAILED && (
            <Tooltip title="Retry D365 Sync">
              <IconButton
                size="small"
                color="primary"
                onClick={e => {
                  e.stopPropagation();
                  handleRetrySync(record.returnId);
                }}
                disabled={isRetrying}
                aria-label="Retry D365 sync"
              >
                <RefreshIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
          <Tooltip title="View Audit Trail">
            <IconButton
              size="small"
              onClick={e => {
                e.stopPropagation();
                navigate(`${Routes.returnDetail(record.returnId)}/audit-trail`);
              }}
              aria-label="View audit trail"
            >
              <HistoryIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          {record.lastSyncError && (
            <Tooltip title={record.lastSyncError}>
              <IconButton size="small" color="error" aria-label="Error details">
                <ErrorIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
        </Box>
      ),
    },
  ];

  const mobileCardRender = (record: ReconciliationRecord) => (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={1}>
        <Box>
          <Typography variant="subtitle2" fontWeight="bold">
            Return {record.returnId.substring(0, 8)}...
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Order: {record.orderNumber}
          </Typography>
        </Box>
        <StatusBadge
          label={record.reconciliationStatus}
          variant={getStatusVariant(record.reconciliationStatus)}
        />
      </Box>
      <Box mt={1}>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          D365 Order: {record.d365ReturnOrderId || 'N/A'}
        </Typography>
        <Box display="flex" justifyContent="space-between" alignItems="center" mt={1}>
          <Typography variant="body2" color="text.secondary">
            {record.lastSyncAttempt ? formatDateTime(record.lastSyncAttempt) : 'Never synced'}
          </Typography>
          {record.reconciliationStatus === D365ReconciliationStatus.FAILED && (
            <Button
              size="small"
              variant="outlined"
              startIcon={<RefreshIcon />}
              onClick={e => {
                e.stopPropagation();
                handleRetrySync(record.returnId);
              }}
              disabled={isRetrying}
            >
              Retry
            </Button>
          )}
        </Box>
      </Box>
    </Box>
  );

  const totalPages = records ? Math.ceil(records.length / pageSize) : 1;

  return (
    <DashboardPageLayout
      title="Returns Reconciliation Dashboard"
      subtitle="Monitor and manage D365 reconciliation for returns"
    >
      <PageBreadcrumbs items={getBreadcrumbs.returnsReconciliation()} />
      <Grid container spacing={3}>
        <Grid item xs={12} md={4}>
          <Card sx={{ bgcolor: 'info.light' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Pending
              </Typography>
              <Typography variant="h3" color="info.main" sx={{ fontWeight: 'bold' }}>
                {summary.pendingReconciliation}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Awaiting reconciliation
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={4}>
          <Card sx={{ bgcolor: 'success.light' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Successful
              </Typography>
              <Typography variant="h3" color="success.main" sx={{ fontWeight: 'bold' }}>
                {summary.successful}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Successfully reconciled
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={4}>
          <Card sx={{ bgcolor: 'error.light' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Failed
              </Typography>
              <Typography variant="h3" color="error.main" sx={{ fontWeight: 'bold' }}>
                {summary.failed}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Reconciliation failed
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12}>
          <FilterBar onClearFilters={handleClearFilters} hasActiveFilters={!!statusFilter}>
            <FormControl fullWidth sx={{ minWidth: 180 }}>
              <InputLabel>Status Filter</InputLabel>
              <Select
                value={statusFilter}
                onChange={e => {
                  setStatusFilter(e.target.value as D365ReconciliationStatus);
                  setCurrentPage(1);
                }}
                label="Status Filter"
              >
                <MenuItem value="">All</MenuItem>
                {Object.values(D365ReconciliationStatus).map(status => (
                  <MenuItem key={status} value={status}>
                    {status.replace(/_/g, ' ')}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </FilterBar>
        </Grid>

        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Reconciliation Records
              </Typography>
              {isLoading ? (
                <SkeletonTable rows={10} columns={9} />
              ) : error ? (
                <Box sx={{ p: 3, textAlign: 'center' }}>
                  <Typography color="error">{error.message}</Typography>
                </Box>
              ) : !records || records.length === 0 ? (
                <EmptyState
                  title="No reconciliation records found"
                  description="No reconciliation records match the current filters"
                  action={
                    statusFilter
                      ? {
                          label: 'Clear Filters',
                          onClick: handleClearFilters,
                        }
                      : undefined
                  }
                />
              ) : (
                <>
                  <ResponsiveTable
                    data={records}
                    columns={columns}
                    getRowKey={record => record.returnId}
                    onRowClick={handleRowClick}
                    emptyMessage="No reconciliation records found"
                    mobileCardRender={mobileCardRender}
                  />
                  {totalPages > 1 && (
                    <Pagination
                      currentPage={currentPage}
                      totalPages={totalPages}
                      totalItems={records.length}
                      itemsPerPage={pageSize}
                      onPageChange={setCurrentPage}
                    />
                  )}
                </>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </DashboardPageLayout>
  );
};
