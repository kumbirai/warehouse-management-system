import { Box, Divider, Grid, LinearProgress, Link, Paper, Stack, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';

import { StatusBadge } from '../../../components/common';
import { getStatusVariant } from '../../../utils/statusUtils';
import { formatDateTime } from '../../../utils/dateUtils';
import { Location, LocationStatus } from '../types/location';
import { LocationActions } from './LocationActions';
import { LocationChildrenList } from './LocationChildrenList';
import { LocationStockList } from './LocationStockList';
import { useAuth } from '../../../hooks/useAuth';
import { useLocation } from '../hooks/useLocation';
import { useLocationChildren } from '../hooks/useLocationChildren';
import { useStockItemsByLocation } from '../hooks/useStockItemsByLocation';
import { Routes } from '../../../utils/navigationUtils';

interface LocationDetailProps {
  location: Location | null;
  onStatusUpdate?: () => void;
}

export const LocationDetail = ({ location, onStatusUpdate }: LocationDetailProps) => {
  const { user } = useAuth();
  const navigate = useNavigate();

  // Fetch parent location if parentLocationId is available
  const shouldFetchParent = !!location?.parentLocationId && !!user?.tenantId;
  const { location: parentLocation } = useLocation(
    shouldFetchParent && location?.parentLocationId ? location.parentLocationId : '',
    shouldFetchParent && user?.tenantId ? user.tenantId : ''
  );

  // Fetch child locations
  const {
    data: childrenData,
    isLoading: childrenLoading,
    error: childrenError,
  } = useLocationChildren(location, user?.tenantId);

  // Fetch stock items at this location
  const {
    data: stockData,
    isLoading: stockLoading,
    error: stockError,
  } = useStockItemsByLocation(location?.locationId, user?.tenantId);

  if (!location) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography>Location not found</Typography>
      </Paper>
    );
  }

  // Helper function to navigate to parent location
  const navigateToParent = () => {
    if (location.parentLocationId) {
      navigate(Routes.locationDetail(location.parentLocationId));
    }
  };

  // Helper function to determine if a coordinate should be clickable
  const getCoordinateLink = (coordinateType: 'zone' | 'aisle' | 'rack' | 'level') => {
    if (!location.parentLocationId) {
      return null;
    }

    // Determine which coordinate corresponds to parent based on location type
    const locationType = location.type?.toUpperCase();
    switch (locationType) {
      case 'BIN':
        // For bins, level corresponds to rack (parent)
        if (coordinateType === 'level' && location.coordinates.level && location.coordinates.level !== '00' && location.coordinates.level !== '—') {
          return navigateToParent;
        }
        break;
      case 'RACK':
        // For racks, rack corresponds to aisle (parent)
        if (coordinateType === 'rack' && location.coordinates.rack && location.coordinates.rack !== '00' && location.coordinates.rack !== '—') {
          return navigateToParent;
        }
        break;
      case 'AISLE':
        // For aisles, aisle corresponds to zone (parent)
        if (coordinateType === 'aisle' && location.coordinates.aisle && location.coordinates.aisle !== '00' && location.coordinates.aisle !== '—') {
          return navigateToParent;
        }
        break;
      case 'ZONE':
        // For zones, zone corresponds to warehouse (parent)
        if (coordinateType === 'zone' && location.coordinates.zone && location.coordinates.zone !== '00' && location.coordinates.zone !== '—') {
          return navigateToParent;
        }
        break;
    }
    return null;
  };

  // Render coordinate value as link or text
  const renderCoordinate = (value: string | undefined, coordinateType: 'zone' | 'aisle' | 'rack' | 'level') => {
    const displayValue = value || '—';
    const linkHandler = getCoordinateLink(coordinateType);

    if (linkHandler && displayValue !== '—' && displayValue !== '00') {
      return (
        <Link
          component="button"
          variant="body1"
          onClick={linkHandler}
          sx={{
            cursor: 'pointer',
            textDecoration: 'underline',
            '&:hover': {
              textDecoration: 'underline',
            },
          }}
        >
          {displayValue}
        </Link>
      );
    }

    return <Typography variant="body1">{displayValue}</Typography>;
  };

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
            {/* Parent Location Link */}
            {location.parentLocationId && (
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Parent Location
                </Typography>
                <Box mt={0.5}>
                  {parentLocation ? (
                    <Link
                      component="button"
                      variant="body1"
                      onClick={navigateToParent}
                      sx={{
                        cursor: 'pointer',
                        textDecoration: 'underline',
                        '&:hover': {
                          textDecoration: 'underline',
                        },
                      }}
                    >
                      {parentLocation.code || parentLocation.name || parentLocation.barcode}
                    </Link>
                  ) : (
                    <Link
                      component="button"
                      variant="body1"
                      onClick={navigateToParent}
                      sx={{
                        cursor: 'pointer',
                        textDecoration: 'underline',
                        '&:hover': {
                          textDecoration: 'underline',
                        },
                      }}
                    >
                      View Parent Location
                    </Link>
                  )}
                </Box>
              </Box>
            )}

            <Box>
              <Typography variant="caption" color="text.secondary">
                Zone
              </Typography>
              {renderCoordinate(location.coordinates.zone, 'zone')}
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Aisle
              </Typography>
              {renderCoordinate(location.coordinates.aisle, 'aisle')}
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Rack
              </Typography>
              {renderCoordinate(location.coordinates.rack, 'rack')}
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Level
              </Typography>
              {renderCoordinate(location.coordinates.level, 'level')}
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

      {/* Child Locations Section */}
      {location.type?.toUpperCase() !== 'BIN' && (
        <Grid item xs={12}>
          <LocationChildrenList
            items={childrenData?.items || []}
            isLoading={childrenLoading}
            error={childrenError}
          />
        </Grid>
      )}

      {/* Stock Details Section */}
      <Grid item xs={12}>
        <LocationStockList
          stockItems={stockData?.stockItems || []}
          isLoading={stockLoading}
          error={stockError}
        />
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
              location={location}
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
