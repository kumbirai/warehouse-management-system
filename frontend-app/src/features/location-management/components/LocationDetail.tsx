import { Box, Divider, Grid, LinearProgress, Paper, Stack, Typography } from '@mui/material';

import { StatusBadge } from '../../../components/common';
import { getStatusVariant } from '../../../utils/statusUtils';
import { formatDateTime } from '../../../utils/dateUtils';
import { Location, LocationStatus } from '../types/location';
import { LocationActions } from './LocationActions';
import { useAuth } from '../../../hooks/useAuth';

interface LocationDetailProps {
  location: Location | null;
  onStatusUpdate?: () => void;
}

export const LocationDetail = ({ location, onStatusUpdate }: LocationDetailProps) => {
  const { user } = useAuth();

  if (!location) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography>Location not found</Typography>
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
                Location ID
              </Typography>
              <Typography variant="body1">{location.locationId}</Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Code
              </Typography>
              <Typography variant="body1" sx={{ fontFamily: 'monospace' }}>
                {location.code}
              </Typography>
            </Box>

            {location.barcode && (
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Barcode
                </Typography>
                <Typography variant="body1" sx={{ fontFamily: 'monospace' }}>
                  {location.barcode}
                </Typography>
              </Box>
            )}

            <Box>
              <Typography variant="caption" color="text.secondary">
                Status
              </Typography>
              <Box mt={0.5}>
                <StatusBadge label={location.status} variant={getStatusVariant(location.status)} />
              </Box>
            </Box>

            {location.description && (
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Description
                </Typography>
                <Typography variant="body1">{location.description}</Typography>
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
              <Typography variant="body1">{location.coordinates.zone || '—'}</Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Aisle
              </Typography>
              <Typography variant="body1">{location.coordinates.aisle || '—'}</Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Rack
              </Typography>
              <Typography variant="body1">{location.coordinates.rack || '—'}</Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Level
              </Typography>
              <Typography variant="body1">{location.coordinates.level || '—'}</Typography>
            </Box>
          </Stack>
        </Paper>
      </Grid>

      {/* Capacity Information */}
      {location.capacity && typeof location.capacity === 'object' && (
        <Grid item xs={12} md={6}>
          <Paper elevation={1} sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Capacity
            </Typography>
            <Divider sx={{ mb: 2 }} />

            <Stack spacing={2}>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Current Quantity
                </Typography>
                <Typography variant="body1">{location.capacity.currentQuantity || 0}</Typography>
              </Box>

              <Box>
                <Typography variant="caption" color="text.secondary">
                  Maximum Quantity
                </Typography>
                <Typography variant="body1">{location.capacity.maximumQuantity || 0}</Typography>
              </Box>

              {location.capacity.maximumQuantity > 0 && (
                <Box>
                  <Box
                    sx={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      mb: 1,
                    }}
                  >
                    <Typography variant="caption" color="text.secondary">
                      Capacity Utilization
                    </Typography>
                    <Typography variant="body2" fontWeight="medium">
                      {Math.round(
                        (location.capacity.currentQuantity / location.capacity.maximumQuantity) *
                          100
                      )}
                      %
                    </Typography>
                  </Box>
                  <LinearProgress
                    variant="determinate"
                    value={Math.min(
                      (location.capacity.currentQuantity / location.capacity.maximumQuantity) * 100,
                      100
                    )}
                    color={
                      (location.capacity.currentQuantity / location.capacity.maximumQuantity) *
                        100 >=
                      80
                        ? 'error'
                        : (location.capacity.currentQuantity / location.capacity.maximumQuantity) *
                              100 >=
                            50
                          ? 'warning'
                          : 'success'
                    }
                    sx={{ height: 8, borderRadius: 4 }}
                    aria-label="Location capacity utilization"
                    aria-valuenow={Math.round(
                      (location.capacity.currentQuantity / location.capacity.maximumQuantity) * 100
                    )}
                    aria-valuemin={0}
                    aria-valuemax={100}
                  />
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    sx={{ mt: 0.5, display: 'block' }}
                  >
                    {location.capacity.currentQuantity || 0} / {location.capacity.maximumQuantity}{' '}
                    units
                  </Typography>
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
              <Typography variant="body1">{formatDateTime(location.createdAt)}</Typography>
            </Box>

            {location.lastModifiedAt && (
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Last Modified
                </Typography>
                <Typography variant="body1">{formatDateTime(location.lastModifiedAt)}</Typography>
              </Box>
            )}
          </Stack>
        </Paper>
      </Grid>

      {/* Actions Section */}
      {user?.tenantId && (
        <Grid item xs={12}>
          <Paper elevation={1} sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Actions
            </Typography>
            <Divider sx={{ mb: 2 }} />

            <LocationActions
              locationId={location.locationId}
              status={location.status as LocationStatus}
              tenantId={user.tenantId}
              onCompleted={onStatusUpdate}
            />
          </Paper>
        </Grid>
      )}
    </Grid>
  );
};
