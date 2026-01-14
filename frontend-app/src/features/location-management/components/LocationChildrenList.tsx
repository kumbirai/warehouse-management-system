import { Box, Chip, Link, Paper, Stack, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { LocationHierarchyItem } from '../types/location';
import { Routes } from '../../../utils/navigationUtils';
import { StatusBadge } from '../../../components/common';
import { getStatusVariant } from '../../../utils/statusUtils';

interface LocationChildrenListProps {
  items: LocationHierarchyItem[];
  isLoading: boolean;
  error: Error | null;
}

export const LocationChildrenList = ({
  items,
  isLoading,
  error,
}: LocationChildrenListProps) => {
  const navigate = useNavigate();

  if (isLoading) {
    return (
      <Paper elevation={1} sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Child Locations
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Loading...
        </Typography>
      </Paper>
    );
  }

  if (error) {
    return (
      <Paper elevation={1} sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Child Locations
        </Typography>
        <Typography variant="body2" color="error">
          Error loading child locations: {error.message}
        </Typography>
      </Paper>
    );
  }

  if (!items || items.length === 0) {
    return (
      <Paper elevation={1} sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Child Locations
        </Typography>
        <Typography variant="body2" color="text.secondary">
          No child locations found
        </Typography>
      </Paper>
    );
  }

  return (
    <Paper elevation={1} sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        Child Locations ({items.length})
      </Typography>
      <Stack spacing={1} sx={{ mt: 2 }}>
        {items.map((item) => {
          const displayName =
            item.location.name ||
            item.location.code ||
            item.location.barcode ||
            'Unknown Location';

          return (
            <Box
              key={item.location.locationId}
              sx={{
                p: 2,
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 1,
                '&:hover': {
                  bgcolor: 'action.hover',
                },
              }}
            >
              <Stack direction="row" spacing={2} alignItems="center">
                <Box sx={{ flex: 1 }}>
                  <Link
                    component="button"
                    variant="body1"
                    onClick={() =>
                      navigate(Routes.locationDetail(item.location.locationId))
                    }
                    sx={{
                      cursor: 'pointer',
                      textDecoration: 'none',
                      fontWeight: 'medium',
                      '&:hover': {
                        textDecoration: 'underline',
                      },
                    }}
                  >
                    {displayName}
                  </Link>
                  {item.location.code && (
                    <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>
                      ({item.location.code})
                    </Typography>
                  )}
                </Box>
                <StatusBadge
                  label={item.location.status}
                  variant={getStatusVariant(item.location.status)}
                />
                {item.childCount > 0 && (
                  <Chip
                    label={`${item.childCount} child${item.childCount !== 1 ? 'ren' : ''}`}
                    size="small"
                    variant="outlined"
                  />
                )}
              </Stack>
            </Box>
          );
        })}
      </Stack>
    </Paper>
  );
};
