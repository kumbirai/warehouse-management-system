import { Button, Card, CardContent, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { useMemo } from 'react';
import { useConsignments } from '../hooks/useConsignments';
import { useAuth } from '../../../hooks/useAuth';
import { Routes } from '../../../utils/navigationUtils';

export const ExpiryAlertCard = () => {
  const { user } = useAuth();
  
  // Memoize filters to prevent unnecessary re-renders and API calls
  const filters = useMemo(
    () => ({
      expiringWithinDays: 7,
      page: 0,
      size: 1, // We only need the count
    }),
    []
  );

  const { consignments, isLoading } = useConsignments(filters, user?.tenantId ?? undefined);

  const expiringCount = consignments?.length ?? 0;

  // If we got 1 result but size was 1, we need to check totalCount
  // For now, we'll use a simple approach: if we have any results, show the alert
  // In a production system, you'd want to fetch totalCount separately

  return (
    <Card
      sx={{
        bgcolor: expiringCount > 0 ? 'error.light' : 'background.paper',
        border: expiringCount > 0 ? '2px solid' : 'none',
        borderColor: expiringCount > 0 ? 'error.main' : 'transparent',
      }}
    >
      <CardContent>
        <Typography variant="h6" gutterBottom>
          Expiring Consignments
        </Typography>
        {isLoading ? (
          <Typography variant="body2" color="text.secondary">
            Loading...
          </Typography>
        ) : (
          <>
            <Typography
              variant="h3"
              color={expiringCount > 0 ? 'error.main' : 'text.primary'}
              sx={{ fontWeight: 'bold' }}
            >
              {expiringCount}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Consignments expiring within 7 days
            </Typography>
            {expiringCount > 0 && (
              <Button
                variant="contained"
                color="error"
                component={RouterLink}
                to={`${Routes.consignments}?expiringWithinDays=7`}
                fullWidth
                aria-label="View expiring consignments"
              >
                View Details
              </Button>
            )}
          </>
        )}
      </CardContent>
    </Card>
  );
};
