import {
  Box,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Grid,
  Paper,
  Typography,
} from '@mui/material';
import { Location } from '../types/location';

interface LocationDetailProps {
  location: Location | null;
  isLoading: boolean;
  error: Error | null;
}

export const LocationDetail = ({ location, isLoading, error }: LocationDetailProps) => {
  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography color="error">Error loading location: {error.message}</Typography>
      </Paper>
    );
  }

  if (!location) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography>Location not found</Typography>
      </Paper>
    );
  }

  return (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Card>
          <CardContent>
            <Typography variant="h5" gutterBottom>
              Location Details
            </Typography>
            <Grid container spacing={2} sx={{ mt: 1 }}>
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">
                  Location ID
                </Typography>
                <Typography variant="body1">{location.locationId}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">
                  Barcode
                </Typography>
                <Typography variant="body1" sx={{ fontFamily: 'monospace' }}>
                  {location.barcode}
                </Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">
                  Zone
                </Typography>
                <Typography variant="body1">{location.coordinates.zone}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">
                  Aisle
                </Typography>
                <Typography variant="body1">{location.coordinates.aisle}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">
                  Rack
                </Typography>
                <Typography variant="body1">{location.coordinates.rack}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">
                  Level
                </Typography>
                <Typography variant="body1">{location.coordinates.level}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">
                  Status
                </Typography>
                <Chip
                  label={location.status}
                  color={location.status === 'AVAILABLE' ? 'success' : 'default'}
                  size="small"
                />
              </Grid>
              {location.capacity && (
                <>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="body2" color="text.secondary">
                      Current Quantity
                    </Typography>
                    <Typography variant="body1">{location.capacity.currentQuantity}</Typography>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="body2" color="text.secondary">
                      Maximum Quantity
                    </Typography>
                    <Typography variant="body1">{location.capacity.maximumQuantity}</Typography>
                  </Grid>
                </>
              )}
              {location.description && (
                <Grid item xs={12}>
                  <Typography variant="body2" color="text.secondary">
                    Description
                  </Typography>
                  <Typography variant="body1">{location.description}</Typography>
                </Grid>
              )}
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">
                  Created At
                </Typography>
                <Typography variant="body1">
                  {new Date(location.createdAt).toLocaleString()}
                </Typography>
              </Grid>
              {location.lastModifiedAt && (
                <Grid item xs={12} sm={6}>
                  <Typography variant="body2" color="text.secondary">
                    Last Modified
                  </Typography>
                  <Typography variant="body1">
                    {new Date(location.lastModifiedAt).toLocaleString()}
                  </Typography>
                </Grid>
              )}
            </Grid>
          </CardContent>
        </Card>
      </Grid>
    </Grid>
  );
};
