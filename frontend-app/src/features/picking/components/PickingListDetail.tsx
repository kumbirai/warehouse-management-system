import {
  Box,
  Card,
  CardContent,
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
import { PickingListQueryResult, PickingListStatus } from '../types/pickingTypes';
import { formatDateTime } from '../../../utils/dateUtils';

interface PickingListDetailProps {
  pickingList: PickingListQueryResult | null;
}

const getStatusColor = (
  status: PickingListStatus
): 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning' => {
  switch (status) {
    case 'RECEIVED':
      return 'info';
    case 'PROCESSING':
      return 'warning';
    case 'PLANNED':
      return 'primary';
    case 'COMPLETED':
      return 'success';
    default:
      return 'default';
  }
};

export const PickingListDetail = ({ pickingList }: PickingListDetailProps) => {
  if (!pickingList) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography>Picking list not found</Typography>
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
                Picking List Reference
              </Typography>
              <Typography variant="body1" sx={{ fontFamily: 'monospace', fontWeight: 'medium' }}>
                {pickingList.pickingListReference || pickingList.id}
              </Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Status
              </Typography>
              <Box mt={0.5}>
                <Chip
                  label={pickingList.status}
                  color={getStatusColor(pickingList.status)}
                  size="small"
                />
              </Box>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Received At
              </Typography>
              <Typography variant="body1">
                {pickingList.receivedAt ? formatDateTime(pickingList.receivedAt) : 'N/A'}
              </Typography>
            </Box>

            {pickingList.processedAt && (
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Processed At
                </Typography>
                <Typography variant="body1">{formatDateTime(pickingList.processedAt)}</Typography>
              </Box>
            )}

            <Box>
              <Typography variant="caption" color="text.secondary">
                Load Count
              </Typography>
              <Typography variant="body1">{pickingList.loadCount}</Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Total Order Count
              </Typography>
              <Typography variant="body1">{pickingList.totalOrderCount}</Typography>
            </Box>

            {pickingList.notes && (
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Notes
                </Typography>
                <Typography variant="body1">{pickingList.notes}</Typography>
              </Box>
            )}
          </Stack>
        </Paper>
      </Grid>

      {/* Loads and Orders */}
      <Grid item xs={12}>
        <Paper elevation={1} sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Loads and Orders
          </Typography>
          <Divider sx={{ mb: 2 }} />

          {pickingList.loads && pickingList.loads.length > 0 ? (
            <Stack spacing={3}>
              {pickingList.loads.map((load, loadIndex) => (
                <Card key={load.loadId} variant="outlined">
                  <CardContent>
                    <Box
                      sx={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        mb: 2,
                      }}
                    >
                      <Typography variant="subtitle1">
                        Load {loadIndex + 1}: {load.loadNumber}
                      </Typography>
                      <Chip
                        label={load.status}
                        size="small"
                        color={load.status === 'COMPLETED' ? 'success' : 'default'}
                      />
                    </Box>

                    {load.orders && load.orders.length > 0 ? (
                      <Stack spacing={2}>
                        {load.orders.map((order, orderIndex) => (
                          <Card
                            key={order.orderId}
                            variant="outlined"
                            sx={{ p: 2, bgcolor: 'grey.50' }}
                          >
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
                              Customer: {order.customerCode}{' '}
                              {order.customerName && `(${order.customerName})`}
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
                  </CardContent>
                </Card>
              ))}
            </Stack>
          ) : (
            <Typography variant="body2" color="text.secondary">
              No loads in this picking list
            </Typography>
          )}
        </Paper>
      </Grid>
    </Grid>
  );
};
