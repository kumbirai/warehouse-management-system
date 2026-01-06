import { Box, Button, Chip, Grid, Paper, Typography } from '@mui/material';
import { StockAllocationResponse } from '../types/stockManagement';
import { getStatusVariant } from '../../../utils/statusUtils';
import { formatDateTime } from '../../../utils/dateUtils';

interface StockAllocationDetailProps {
  allocation: StockAllocationResponse | null;
  onRelease?: () => void;
  isReleasing?: boolean;
}

export const StockAllocationDetail = ({
  allocation,
  onRelease,
  isReleasing = false,
}: StockAllocationDetailProps) => {
  if (!allocation) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography>No stock allocation data available</Typography>
      </Paper>
    );
  }

  const canRelease = allocation.status === 'ALLOCATED';

  return (
    <Box>
      <Paper sx={{ p: 3, mb: 2 }}>
        <Typography variant="h6" gutterBottom>
          Stock Allocation Details
        </Typography>
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6}>
            <Typography variant="body2" color="text.secondary">
              Allocation ID
            </Typography>
            <Typography variant="body1" gutterBottom>
              {allocation.allocationId}
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="body2" color="text.secondary">
              Status
            </Typography>
            <Box sx={{ mt: 0.5 }}>
              <Chip
                label={allocation.status}
                color={
                  getStatusVariant(allocation.status) === 'success'
                    ? 'success'
                    : getStatusVariant(allocation.status) === 'error'
                      ? 'error'
                      : getStatusVariant(allocation.status) === 'warning'
                        ? 'warning'
                        : 'default'
                }
                size="small"
              />
            </Box>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="body2" color="text.secondary">
              Product ID
            </Typography>
            <Typography variant="body1" gutterBottom>
              {allocation.productId}
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="body2" color="text.secondary">
              Location ID
            </Typography>
            <Typography variant="body1" gutterBottom>
              {allocation.locationId || '—'}
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="body2" color="text.secondary">
              Stock Item ID
            </Typography>
            <Typography variant="body1" gutterBottom>
              {allocation.stockItemId}
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="body2" color="text.secondary">
              Quantity
            </Typography>
            <Typography variant="body1" gutterBottom>
              {allocation.quantity}
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="body2" color="text.secondary">
              Allocation Type
            </Typography>
            <Typography variant="body1" gutterBottom>
              {allocation.allocationType}
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="body2" color="text.secondary">
              Reference ID
            </Typography>
            <Typography variant="body1" gutterBottom>
              {allocation.referenceId || '—'}
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="body2" color="text.secondary">
              Allocated At
            </Typography>
            <Typography variant="body1" gutterBottom>
              {formatDateTime(allocation.allocatedAt)}
            </Typography>
          </Grid>
          {allocation.releasedAt && (
            <Grid item xs={12} sm={6}>
              <Typography variant="body2" color="text.secondary">
                Released At
              </Typography>
              <Typography variant="body1" gutterBottom>
                {formatDateTime(allocation.releasedAt)}
              </Typography>
            </Grid>
          )}
          <Grid item xs={12} sm={6}>
            <Typography variant="body2" color="text.secondary">
              Allocated By
            </Typography>
            <Typography variant="body1" gutterBottom>
              {allocation.allocatedBy}
            </Typography>
          </Grid>
          {allocation.notes && (
            <Grid item xs={12}>
              <Typography variant="body2" color="text.secondary">
                Notes
              </Typography>
              <Typography variant="body1" gutterBottom>
                {allocation.notes}
              </Typography>
            </Grid>
          )}
        </Grid>
      </Paper>

      {canRelease && onRelease && (
        <Paper sx={{ p: 2 }}>
          <Box
            sx={{
              display: 'flex',
              flexDirection: { xs: 'column', sm: 'row' },
              gap: 2,
              justifyContent: 'flex-end',
            }}
          >
            <Button
              variant="outlined"
              color="warning"
              onClick={onRelease}
              disabled={isReleasing}
              sx={{ width: { xs: '100%', sm: 'auto' } }}
              aria-label="Release stock allocation"
            >
              {isReleasing ? 'Releasing...' : 'Release Allocation'}
            </Button>
          </Box>
        </Paper>
      )}
    </Box>
  );
};

