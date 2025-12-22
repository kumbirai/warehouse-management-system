import { Alert, Box, Button, Divider, Grid, Paper, Stack, Typography } from '@mui/material';
import { StockItem } from '../types/stockManagement';
import { StockClassificationBadge } from './StockClassificationBadge';
import { formatDate, formatDateTime } from '../../../utils/dateUtils';
import { AssignLocationToStockForm } from './AssignLocationToStockForm';
import { useState } from 'react';
import { LocationOn as LocationIcon } from '@mui/icons-material';
import { useAssignLocationToStock } from '../hooks/useAssignLocationToStock';

interface StockItemDetailProps {
  stockItem: StockItem | null;
  onLocationAssigned?: () => void;
}

/**
 * StockItemDetail Component
 * <p>
 * Displays detailed information about a stock item including classification and location.
 */
export const StockItemDetail = ({ stockItem, onLocationAssigned }: StockItemDetailProps) => {
  const [assignLocationOpen, setAssignLocationOpen] = useState(false);
  const assignLocation = useAssignLocationToStock();

  if (!stockItem) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography>Stock item not found</Typography>
      </Paper>
    );
  }

  const handleAssignLocation = () => {
    setAssignLocationOpen(true);
  };

  const handleLocationAssigned = () => {
    setAssignLocationOpen(false);
    onLocationAssigned?.();
  };

  return (
    <>
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
                  Stock Item ID
                </Typography>
                <Typography variant="body1" sx={{ fontFamily: 'monospace' }}>
                  {stockItem.stockItemId}
                </Typography>
              </Box>

              <Box>
                <Typography variant="caption" color="text.secondary">
                  Product ID
                </Typography>
                <Typography variant="body1" sx={{ fontFamily: 'monospace' }}>
                  {stockItem.productId}
                </Typography>
              </Box>

              <Box>
                <Typography variant="caption" color="text.secondary">
                  Quantity
                </Typography>
                <Typography variant="body1">{stockItem.quantity}</Typography>
              </Box>

              <Box>
                <Typography variant="caption" color="text.secondary">
                  Classification
                </Typography>
                <Box mt={0.5}>
                  <StockClassificationBadge classification={stockItem.classification} />
                </Box>
              </Box>
            </Stack>
          </Paper>
        </Grid>

        {/* Location and Expiration */}
        <Grid item xs={12} md={6}>
          <Paper elevation={1} sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Location & Expiration
            </Typography>
            <Divider sx={{ mb: 2 }} />

            <Stack spacing={2}>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Location
                </Typography>
                {stockItem.locationId ? (
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 0.5 }}>
                    <LocationIcon fontSize="small" color="action" />
                    <Typography variant="body1" sx={{ fontFamily: 'monospace' }}>
                      {stockItem.locationId}
                    </Typography>
                  </Box>
                ) : (
                  <Alert severity="warning" sx={{ mt: 0.5 }}>
                    No location assigned
                  </Alert>
                )}
              </Box>

              <Box>
                <Typography variant="caption" color="text.secondary">
                  Expiration Date
                </Typography>
                <Typography variant="body1">
                  {stockItem.expirationDate
                    ? formatDate(stockItem.expirationDate)
                    : 'N/A (Non-perishable)'}
                </Typography>
              </Box>

              <Box>
                <Typography variant="caption" color="text.secondary">
                  Created At
                </Typography>
                <Typography variant="body1">{formatDateTime(stockItem.createdAt)}</Typography>
              </Box>

              <Box>
                <Typography variant="caption" color="text.secondary">
                  Last Modified
                </Typography>
                <Typography variant="body1">{formatDateTime(stockItem.lastModifiedAt)}</Typography>
              </Box>
            </Stack>
          </Paper>
        </Grid>

        {/* Actions */}
        {!stockItem.locationId && (
          <Grid item xs={12}>
            <Paper elevation={1} sx={{ p: 3 }}>
              <Typography variant="h6" gutterBottom>
                Actions
              </Typography>
              <Divider sx={{ mb: 2 }} />

              <Alert severity="info" sx={{ mb: 2 }}>
                This stock item does not have a location assigned. Assign a location to enable
                warehouse operations.
              </Alert>
              <Button
                variant="contained"
                color="primary"
                onClick={handleAssignLocation}
                disabled={assignLocation.isPending}
              >
                Assign Location
              </Button>
            </Paper>
          </Grid>
        )}
      </Grid>

      <AssignLocationToStockForm
        open={assignLocationOpen}
        onClose={() => setAssignLocationOpen(false)}
        stockItem={stockItem}
        onSuccess={handleLocationAssigned}
      />
    </>
  );
};
