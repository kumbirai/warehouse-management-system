import {
  Box,
  Chip,
  CircularProgress,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { Location } from '../types/location';
import { useNavigate } from 'react-router-dom';

interface LocationListProps {
  locations: Location[];
  isLoading: boolean;
  error: Error | null;
}

export const LocationList = ({ locations, isLoading, error }: LocationListProps) => {
  const navigate = useNavigate();

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
        <Typography color="error">Error loading locations: {error.message}</Typography>
      </Paper>
    );
  }

  // Defensive check: ensure locations is an array
  if (!Array.isArray(locations)) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography color="error">Invalid data format: locations is not an array</Typography>
      </Paper>
    );
  }

  if (locations.length === 0) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography>No locations found</Typography>
      </Paper>
    );
  }

  return (
    <TableContainer component={Paper}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Barcode</TableCell>
            <TableCell>Zone</TableCell>
            <TableCell>Aisle</TableCell>
            <TableCell>Rack</TableCell>
            <TableCell>Level</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Description</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {locations.map(location => (
            <TableRow
              key={location.locationId}
              hover
              sx={{ cursor: 'pointer' }}
              onClick={() => navigate(`/locations/${location.locationId}`)}
            >
              <TableCell>{location.barcode}</TableCell>
              <TableCell>{location.coordinates.zone}</TableCell>
              <TableCell>{location.coordinates.aisle}</TableCell>
              <TableCell>{location.coordinates.rack}</TableCell>
              <TableCell>{location.coordinates.level}</TableCell>
              <TableCell>
                <Chip
                  label={location.status}
                  color={
                    location.status === 'AVAILABLE'
                      ? 'success'
                      : location.status === 'BLOCKED'
                        ? 'error'
                        : location.status === 'RESERVED'
                          ? 'warning'
                          : 'default'
                  }
                  size="small"
                />
              </TableCell>
              <TableCell>{location.description || '-'}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
};
