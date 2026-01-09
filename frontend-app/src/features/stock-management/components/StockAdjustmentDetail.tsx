import { Grid, Paper, Typography } from '@mui/material';
import { StockAdjustmentResponse } from '../types/stockManagement';
import { formatDateTime } from '../../../utils/dateUtils';

interface StockAdjustmentDetailProps {
  adjustment: StockAdjustmentResponse | null;
}

export const StockAdjustmentDetail = ({ adjustment }: StockAdjustmentDetailProps) => {
  if (!adjustment) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography>No stock adjustment data available</Typography>
      </Paper>
    );
  }

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        Stock Adjustment Details
      </Typography>
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6}>
          <Typography variant="body2" color="text.secondary">
            Adjustment ID
          </Typography>
          <Typography variant="body1" gutterBottom>
            {adjustment.adjustmentId}
          </Typography>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Typography variant="body2" color="text.secondary">
            Product ID
          </Typography>
          <Typography variant="body1" gutterBottom>
            {adjustment.productId}
          </Typography>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Typography variant="body2" color="text.secondary">
            Location ID
          </Typography>
          <Typography variant="body1" gutterBottom>
            {adjustment.locationId || '—'}
          </Typography>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Typography variant="body2" color="text.secondary">
            Stock Item ID
          </Typography>
          <Typography variant="body1" gutterBottom>
            {adjustment.stockItemId || '—'}
          </Typography>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Typography variant="body2" color="text.secondary">
            Adjustment Type
          </Typography>
          <Typography variant="body1" gutterBottom>
            {adjustment.adjustmentType}
          </Typography>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Typography variant="body2" color="text.secondary">
            Quantity
          </Typography>
          <Typography variant="body1" gutterBottom>
            {adjustment.quantity}
          </Typography>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Typography variant="body2" color="text.secondary">
            Quantity Before
          </Typography>
          <Typography variant="body1" gutterBottom>
            {adjustment.quantityBefore}
          </Typography>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Typography variant="body2" color="text.secondary">
            Quantity After
          </Typography>
          <Typography variant="body1" gutterBottom>
            {adjustment.quantityAfter}
          </Typography>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Typography variant="body2" color="text.secondary">
            Reason
          </Typography>
          <Typography variant="body1" gutterBottom>
            {adjustment.reason}
          </Typography>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Typography variant="body2" color="text.secondary">
            Adjusted At
          </Typography>
          <Typography variant="body1" gutterBottom>
            {formatDateTime(adjustment.adjustedAt)}
          </Typography>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Typography variant="body2" color="text.secondary">
            Adjusted By
          </Typography>
          <Typography variant="body1" gutterBottom>
            {adjustment.adjustedBy}
          </Typography>
        </Grid>
        {adjustment.authorizationCode && (
          <Grid item xs={12} sm={6}>
            <Typography variant="body2" color="text.secondary">
              Authorization Code
            </Typography>
            <Typography variant="body1" gutterBottom>
              {adjustment.authorizationCode}
            </Typography>
          </Grid>
        )}
        {adjustment.notes && (
          <Grid item xs={12}>
            <Typography variant="body2" color="text.secondary">
              Notes
            </Typography>
            <Typography variant="body1" gutterBottom>
              {adjustment.notes}
            </Typography>
          </Grid>
        )}
      </Grid>
    </Paper>
  );
};
