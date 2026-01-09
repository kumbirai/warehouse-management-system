import { Box, Divider, Grid, Paper, Stack, Typography } from '@mui/material';

import { StatusBadge } from '../../../components/common';
import { getStatusVariant } from '../../../utils/statusUtils';
import { formatDateTime } from '../../../utils/dateUtils';
import { Location } from '../types/location';

interface WarehouseDetailProps {
  warehouse: Location | null;
}

export const WarehouseDetail = ({ warehouse }: WarehouseDetailProps) => {
  if (!warehouse) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography>Warehouse not found</Typography>
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
                Warehouse Code
              </Typography>
              <Typography variant="body1" sx={{ fontFamily: 'monospace', fontWeight: 'medium' }}>
                {warehouse.code}
              </Typography>
            </Box>

            {warehouse.name && (
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Warehouse Name
                </Typography>
                <Typography variant="body1" fontWeight="medium">
                  {warehouse.name}
                </Typography>
              </Box>
            )}

            {warehouse.barcode && (
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Barcode
                </Typography>
                <Typography variant="body1" sx={{ fontFamily: 'monospace' }}>
                  {warehouse.barcode}
                </Typography>
              </Box>
            )}

            <Box>
              <Typography variant="caption" color="text.secondary">
                Status
              </Typography>
              <Box mt={0.5}>
                <StatusBadge label={warehouse.status} variant={getStatusVariant(warehouse.status)} />
              </Box>
            </Box>

            {warehouse.description && (
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Description
                </Typography>
                <Typography variant="body1">{warehouse.description}</Typography>
              </Box>
            )}
          </Stack>
        </Paper>
      </Grid>

      {/* Location Coordinates */}
      <Grid item xs={12} md={6}>
        <Paper elevation={1} sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Location Coordinates
          </Typography>
          <Divider sx={{ mb: 2 }} />

          <Stack spacing={2}>
            <Box>
              <Typography variant="caption" color="text.secondary">
                Zone
              </Typography>
              <Typography variant="body1">{warehouse.coordinates?.zone || '—'}</Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Aisle
              </Typography>
              <Typography variant="body1">{warehouse.coordinates?.aisle || '—'}</Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Rack
              </Typography>
              <Typography variant="body1">{warehouse.coordinates?.rack || '—'}</Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Level
              </Typography>
              <Typography variant="body1">{warehouse.coordinates?.level || '—'}</Typography>
            </Box>

            {warehouse.coordinates && (
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Hierarchy
                </Typography>
                <Typography variant="body1" sx={{ fontFamily: 'monospace' }}>
                  {warehouse.coordinates.zone}-{warehouse.coordinates.aisle}-{warehouse.coordinates.rack}-{warehouse.coordinates.level}
                </Typography>
              </Box>
            )}
          </Stack>
        </Paper>
      </Grid>

      {/* Capacity Information */}
      {warehouse.capacity && typeof warehouse.capacity === 'object' && (
        <Grid item xs={12} md={6}>
          <Paper elevation={1} sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Capacity Information
            </Typography>
            <Divider sx={{ mb: 2 }} />

            <Stack spacing={2}>
              {warehouse.capacity.currentQuantity !== undefined && (
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Current Quantity
                  </Typography>
                  <Typography variant="body1">{warehouse.capacity.currentQuantity}</Typography>
                </Box>
              )}

              {warehouse.capacity.maximumQuantity !== undefined && (
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Maximum Quantity
                  </Typography>
                  <Typography variant="body1">{warehouse.capacity.maximumQuantity}</Typography>
                </Box>
              )}
            </Stack>
          </Paper>
        </Grid>
      )}

      {/* Audit Information */}
      <Grid item xs={12} md={6}>
        <Paper elevation={1} sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Audit Information
          </Typography>
          <Divider sx={{ mb: 2 }} />

          <Stack spacing={2}>
            <Box>
              <Typography variant="caption" color="text.secondary">
                Created At
              </Typography>
              <Typography variant="body1">
                {warehouse.createdAt ? formatDateTime(warehouse.createdAt) : '—'}
              </Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Last Modified
              </Typography>
              <Typography variant="body1">
                {warehouse.lastModifiedAt ? formatDateTime(warehouse.lastModifiedAt) : '—'}
              </Typography>
            </Box>
          </Stack>
        </Paper>
      </Grid>
    </Grid>
  );
};
