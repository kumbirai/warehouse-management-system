import {
  Box,
  Card,
  Chip,
  Divider,
  Grid,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { LoadDetailQueryResult, LoadStatus } from '../types/pickingTypes';
import { formatDateTime } from '../../../utils/dateUtils';

interface LoadDetailProps {
  load: LoadDetailQueryResult | null;
}

const getStatusColor = (
  status: LoadStatus
): 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning' => {
  switch (status) {
    case 'CREATED':
      return 'info';
    case 'PLANNED':
      return 'primary';
    case 'IN_PROGRESS':
      return 'warning';
    case 'COMPLETED':
      return 'success';
    default:
      return 'default';
  }
};

export const LoadDetail = ({ load }: LoadDetailProps) => {
  if (!load) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography>Load not found</Typography>
      </Paper>
    );
  }

  return (
    <Grid container spacing={3}>
      {/* Basic Information */}
      <Grid item xs={12} md={6}>
        <Paper elevation={1} sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Basic Information
          </Typography>
          <Divider sx={{ mb: 2 }} />

          <Stack spacing={2}>
            <Box>
              <Typography variant="caption" color="text.secondary">
                Load ID
              </Typography>
              <Typography variant="body1" sx={{ fontFamily: 'monospace', fontWeight: 'medium' }}>
                {load.id}
              </Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Load Number
              </Typography>
              <Typography variant="body1" sx={{ fontFamily: 'monospace' }}>
                {load.loadNumber}
              </Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Status
              </Typography>
              <Box mt={0.5}>
                <Chip label={load.status} color={getStatusColor(load.status)} size="small" />
              </Box>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Created At
              </Typography>
              <Typography variant="body1">
                {load.createdAt ? formatDateTime(load.createdAt) : 'N/A'}
              </Typography>
            </Box>

            {load.plannedAt && (
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Planned At
                </Typography>
                <Typography variant="body1">{formatDateTime(load.plannedAt)}</Typography>
              </Box>
            )}

            <Box>
              <Typography variant="caption" color="text.secondary">
                Order Count
              </Typography>
              <Typography variant="body1">{load.orders?.length || 0}</Typography>
            </Box>
          </Stack>
        </Paper>
      </Grid>

      {/* Orders */}
      <Grid item xs={12}>
        <Paper elevation={1} sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Orders
          </Typography>
          <Divider sx={{ mb: 2 }} />

          {load.orders && load.orders.length > 0 ? (
            <Stack spacing={2}>
              {load.orders.map((order, orderIndex) => (
                <Card key={order.orderId} variant="outlined" sx={{ p: 2, bgcolor: 'grey.50' }}>
                  <Box
                    sx={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      mb: 1,
                    }}
                  >
                    <Typography variant="subtitle2">
                      Order {orderIndex + 1}: {order.orderNumber}
                    </Typography>
                    <Box sx={{ display: 'flex', gap: 1 }}>
                      <Chip
                        label={order.priority}
                        size="small"
                        color={
                          order.priority === 'HIGH'
                            ? 'error'
                            : order.priority === 'NORMAL'
                              ? 'default'
                              : 'info'
                        }
                      />
                      <Chip
                        label={order.status}
                        size="small"
                        color={order.status === 'COMPLETED' ? 'success' : 'default'}
                      />
                    </Box>
                  </Box>

                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                    Customer: {order.customerCode} {order.customerName && `(${order.customerName})`}
                  </Typography>

                  {order.lineItems && order.lineItems.length > 0 && (
                    <TableContainer>
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell>Product Code</TableCell>
                            <TableCell>Description</TableCell>
                            <TableCell align="right">Quantity</TableCell>
                            {order.lineItems.some(item => item.notes) && (
                              <TableCell>Notes</TableCell>
                            )}
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {order.lineItems.map((lineItem, lineItemIndex) => (
                            <TableRow key={lineItem.lineItemId || lineItemIndex}>
                              <TableCell>
                                <Typography
                                  variant="body2"
                                  sx={{ fontFamily: 'monospace', fontWeight: 'medium' }}
                                >
                                  {lineItem.productCode}
                                </Typography>
                              </TableCell>
                              <TableCell>
                                <Typography variant="body2">
                                  {lineItem.productDescription || '-'}
                                </Typography>
                              </TableCell>
                              <TableCell align="right">{lineItem.quantity}</TableCell>
                              {order.lineItems.some(item => item.notes) && (
                                <TableCell>{lineItem.notes || '-'}</TableCell>
                              )}
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  )}
                </Card>
              ))}
            </Stack>
          ) : (
            <Typography variant="body2" color="text.secondary">
              No orders in this load
            </Typography>
          )}
        </Paper>
      </Grid>
    </Grid>
  );
};
