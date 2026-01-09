import { Box, Button, Chip, Grid, Paper, Typography } from '@mui/material';
import { StockMovement } from '../services/stockMovementService';
import { getStatusVariant } from '../../../utils/statusUtils';

interface StockMovementDetailProps {
  movement: StockMovement | null;
  onComplete?: () => void;
  onCancel?: () => void;
  isCompleting?: boolean;
  isCancelling?: boolean;
}

export const StockMovementDetail = ({
  movement,
  onComplete,
  onCancel,
  isCompleting = false,
  isCancelling = false,
}: StockMovementDetailProps) => {
  if (!movement) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography>No stock movement data available</Typography>
      </Paper>
    );
  }

  const canComplete = movement.status === 'INITIATED';
  const canCancel = movement.status === 'INITIATED';

  return (
    <Box>
      <Paper sx={{ p: 3, mb: 2 }}>
        <Typography variant="h6" gutterBottom>
          Stock Movement Details
        </Typography>
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6}>
            <Typography variant="body2" color="text.secondary">
              Movement ID
            </Typography>
            <Typography variant="body1" gutterBottom>
              {movement.stockMovementId}
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="body2" color="text.secondary">
              Status
            </Typography>
            <Box sx={{ mt: 0.5 }}>
              <Chip
                label={movement.status}
                color={
                  getStatusVariant(movement.status) === 'success'
                    ? 'success'
                    : getStatusVariant(movement.status) === 'error'
                      ? 'error'
                      : getStatusVariant(movement.status) === 'warning'
                        ? 'warning'
                        : 'default'
                }
                size="small"
              />
            </Box>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="body2" color="text.secondary">
              Stock Item ID
            </Typography>
            <Typography variant="body1" gutterBottom>
              {movement.stockItemId}
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="body2" color="text.secondary">
              Product ID
            </Typography>
            <Typography variant="body1" gutterBottom>
              {movement.productId}
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="body2" color="text.secondary">
              Source Location
            </Typography>
            <Typography variant="body1" gutterBottom>
              {movement.sourceLocationId}
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="body2" color="text.secondary">
              Destination Location
            </Typography>
            <Typography variant="body1" gutterBottom>
              {movement.destinationLocationId}
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="body2" color="text.secondary">
              Quantity
            </Typography>
            <Typography variant="body1" gutterBottom>
              {movement.quantity}
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="body2" color="text.secondary">
              Movement Type
            </Typography>
            <Typography variant="body1" gutterBottom>
              {movement.movementType}
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="body2" color="text.secondary">
              Reason
            </Typography>
            <Typography variant="body1" gutterBottom>
              {movement.reason}
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="body2" color="text.secondary">
              Initiated At
            </Typography>
            <Typography variant="body1" gutterBottom>
              {new Date(movement.initiatedAt).toLocaleString()}
            </Typography>
          </Grid>
          {movement.completedAt && (
            <Grid item xs={12} sm={6}>
              <Typography variant="body2" color="text.secondary">
                Completed At
              </Typography>
              <Typography variant="body1" gutterBottom>
                {new Date(movement.completedAt).toLocaleString()}
              </Typography>
            </Grid>
          )}
          {movement.cancelledAt && (
            <>
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">
                  Cancelled At
                </Typography>
                <Typography variant="body1" gutterBottom>
                  {new Date(movement.cancelledAt).toLocaleString()}
                </Typography>
              </Grid>
              {movement.cancellationReason && (
                <Grid item xs={12}>
                  <Typography variant="body2" color="text.secondary">
                    Cancellation Reason
                  </Typography>
                  <Typography variant="body1" gutterBottom>
                    {movement.cancellationReason}
                  </Typography>
                </Grid>
              )}
            </>
          )}
        </Grid>
      </Paper>

      {(canComplete || canCancel) && (
        <Paper sx={{ p: 2 }}>
          <Box
            sx={{
              display: 'flex',
              flexDirection: { xs: 'column', sm: 'row' },
              gap: 2,
              justifyContent: 'flex-end',
            }}
          >
            {canComplete && onComplete && (
              <Button
                variant="contained"
                sx={{ width: { xs: '100%', sm: 'auto' } }}
                aria-label="Complete stock movement"
                color="success"
                onClick={onComplete}
                disabled={isCompleting}
              >
                {isCompleting ? 'Completing...' : 'Complete Movement'}
              </Button>
            )}
            {canCancel && onCancel && (
              <Button variant="outlined" color="error" onClick={onCancel} disabled={isCancelling}>
                {isCancelling ? 'Cancelling...' : 'Cancel Movement'}
              </Button>
            )}
          </Box>
        </Paper>
      )}
    </Box>
  );
};
