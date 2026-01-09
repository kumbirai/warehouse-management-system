import { useMemo, useState } from 'react';
import {
  Card,
  CardContent,
  Chip,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { DashboardPageLayout } from '../../../components/layouts';
import { useRestockRequests } from '../hooks/useRestockRequests';
import { RestockPriority, RestockRequestStatus } from '../types/stockManagement';
import { formatDateTime } from '../../../utils/dateUtils';

export const RestockRequestsDashboard = () => {
  const [statusFilter, setStatusFilter] = useState<RestockRequestStatus | ''>('');
  const [priorityFilter, setPriorityFilter] = useState<RestockPriority | ''>('');

  const filters = useMemo(
    () => ({
      status: statusFilter || undefined,
      priority: priorityFilter || undefined,
      page: 0,
      size: 100,
    }),
    [statusFilter, priorityFilter]
  );

  const { restockRequests, totalCount, isLoading, error } = useRestockRequests(filters);

  // Group by priority
  const groupedByPriority = useMemo(() => {
    const groups: Record<RestockPriority, typeof restockRequests> = {
      HIGH: [],
      MEDIUM: [],
      LOW: [],
    };

    restockRequests.forEach(request => {
      if (groups[request.priority]) {
        groups[request.priority].push(request);
      }
    });

    return groups;
  }, [restockRequests]);

  const totalHigh = groupedByPriority.HIGH.length;
  const totalMedium = groupedByPriority.MEDIUM.length;
  const totalLow = groupedByPriority.LOW.length;

  const getPriorityColor = (priority: RestockPriority): 'error' | 'warning' | 'info' => {
    switch (priority) {
      case 'HIGH':
        return 'error';
      case 'MEDIUM':
        return 'warning';
      case 'LOW':
        return 'info';
    }
  };

  const getStatusColor = (
    status: RestockRequestStatus
  ): 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning' => {
    switch (status) {
      case 'PENDING':
        return 'warning';
      case 'SENT_TO_D365':
        return 'info';
      case 'FULFILLED':
        return 'success';
      case 'CANCELLED':
        return 'error';
    }
  };

  return (
    <DashboardPageLayout
      title="Restock Requests Dashboard"
      subtitle="Monitor and manage automated restock requests"
    >
      <Grid container spacing={3}>
        {/* Summary Cards */}
        <Grid item xs={12} md={4}>
          <Card sx={{ bgcolor: 'error.light' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                High Priority
              </Typography>
              <Typography variant="h3" color="error.main" sx={{ fontWeight: 'bold' }}>
                {totalHigh}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Urgent restock requests
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={4}>
          <Card sx={{ bgcolor: 'warning.light' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Medium Priority
              </Typography>
              <Typography variant="h3" color="warning.main" sx={{ fontWeight: 'bold' }}>
                {totalMedium}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Standard restock requests
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={4}>
          <Card sx={{ bgcolor: 'info.light' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Low Priority
              </Typography>
              <Typography variant="h3" color="info.main" sx={{ fontWeight: 'bold' }}>
                {totalLow}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Low priority requests
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* Filters */}
        <Grid item xs={12}>
          <Paper elevation={1} sx={{ p: 2 }}>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} sm={6}>
                <FormControl fullWidth size="small">
                  <InputLabel>Status</InputLabel>
                  <Select
                    value={statusFilter}
                    label="Status"
                    onChange={e => setStatusFilter(e.target.value as RestockRequestStatus | '')}
                  >
                    <MenuItem value="">All</MenuItem>
                    <MenuItem value="PENDING">Pending</MenuItem>
                    <MenuItem value="SENT_TO_D365">Sent to D365</MenuItem>
                    <MenuItem value="FULFILLED">Fulfilled</MenuItem>
                    <MenuItem value="CANCELLED">Cancelled</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={6}>
                <FormControl fullWidth size="small">
                  <InputLabel>Priority</InputLabel>
                  <Select
                    value={priorityFilter}
                    label="Priority"
                    onChange={e => setPriorityFilter(e.target.value as RestockPriority | '')}
                  >
                    <MenuItem value="">All</MenuItem>
                    <MenuItem value="HIGH">High</MenuItem>
                    <MenuItem value="MEDIUM">Medium</MenuItem>
                    <MenuItem value="LOW">Low</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
            </Grid>
          </Paper>
        </Grid>

        {/* Restock Requests List */}
        <Grid item xs={12}>
          <Paper elevation={1} sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Restock Requests ({totalCount})
            </Typography>
            {isLoading ? (
              <Typography>Loading...</Typography>
            ) : error ? (
              <Typography color="error">{error.message}</Typography>
            ) : restockRequests.length === 0 ? (
              <Typography color="text.secondary">
                No restock requests found for the selected criteria.
              </Typography>
            ) : (
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Product ID</TableCell>
                      <TableCell>Location</TableCell>
                      <TableCell>Current Qty</TableCell>
                      <TableCell>Min Qty</TableCell>
                      <TableCell>Requested Qty</TableCell>
                      <TableCell>Priority</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Created At</TableCell>
                      <TableCell>D365 Reference</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {restockRequests.map(request => (
                      <TableRow key={request.restockRequestId}>
                        <TableCell>
                          <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                            {request.productId}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                            {request.locationId || 'All Locations'}
                          </Typography>
                        </TableCell>
                        <TableCell>{request.currentQuantity}</TableCell>
                        <TableCell>{request.minimumQuantity}</TableCell>
                        <TableCell>{request.requestedQuantity}</TableCell>
                        <TableCell>
                          <Chip
                            label={request.priority}
                            color={getPriorityColor(request.priority)}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={request.status}
                            color={getStatusColor(request.status)}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>{formatDateTime(request.createdAt)}</TableCell>
                        <TableCell>
                          <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                            {request.d365OrderReference || '-'}
                          </Typography>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </Paper>
        </Grid>
      </Grid>
    </DashboardPageLayout>
  );
};
