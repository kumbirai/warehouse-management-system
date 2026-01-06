import { Box, Card, CardContent, Chip, LinearProgress, Typography, Alert } from '@mui/material';
import { StockLevelResponse } from '../types/stockManagement';
import { useLocations } from '../../location-management/hooks/useLocations';

interface StockLevelListProps {
  stockLevels: StockLevelResponse[];
}

type StockLevelStatus = 'NORMAL' | 'LOW' | 'CRITICAL' | 'ABOVE_MAX' | 'APPROACHING_MIN' | 'APPROACHING_MAX';

const getStockLevelStatus = (
  stockLevel: StockLevelResponse
): StockLevelStatus => {
  const { availableQuantity, minimumQuantity, maximumQuantity } = stockLevel;

  if (minimumQuantity === undefined && maximumQuantity === undefined) {
    return 'NORMAL';
  }

  if (minimumQuantity !== undefined && availableQuantity < minimumQuantity) {
    const threshold = minimumQuantity * 0.2; // 20% buffer
    if (availableQuantity < minimumQuantity - threshold) {
      return 'CRITICAL';
    }
    return 'LOW';
  }

  if (maximumQuantity !== undefined && availableQuantity > maximumQuantity) {
    return 'ABOVE_MAX';
  }

  if (minimumQuantity !== undefined) {
    const threshold = minimumQuantity * 0.2;
    if (availableQuantity < minimumQuantity + threshold) {
      return 'APPROACHING_MIN';
    }
  }

  if (maximumQuantity !== undefined) {
    const threshold = maximumQuantity * 0.2;
    if (availableQuantity > maximumQuantity - threshold) {
      return 'APPROACHING_MAX';
    }
  }

  return 'NORMAL';
};

const getStatusColor = (status: StockLevelStatus): 'success' | 'warning' | 'error' | 'info' => {
  switch (status) {
    case 'CRITICAL':
    case 'ABOVE_MAX':
      return 'error';
    case 'LOW':
    case 'APPROACHING_MIN':
    case 'APPROACHING_MAX':
      return 'warning';
    case 'NORMAL':
    default:
      return 'success';
  }
};

const getStatusLabel = (status: StockLevelStatus): string => {
  switch (status) {
    case 'CRITICAL':
      return 'Critical - Below Minimum';
    case 'LOW':
      return 'Low Stock';
    case 'APPROACHING_MIN':
      return 'Approaching Minimum';
    case 'APPROACHING_MAX':
      return 'Approaching Maximum';
    case 'ABOVE_MAX':
      return 'Above Maximum';
    case 'NORMAL':
    default:
      return 'Normal';
  }
};

export const StockLevelList = ({ stockLevels }: StockLevelListProps) => {
  const { locations } = useLocations({ page: 0, size: 1000, tenantId: '' }); // tenantId will be set by hook

  const locationsMap = new Map(locations.map((loc) => [loc.locationId, loc]));

  if (stockLevels.length === 0) {
    return (
      <Box sx={{ textAlign: 'center', py: 4 }}>
        <Typography variant="body1" color="text.secondary">
          No stock levels found for this product
        </Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      {stockLevels.map((stockLevel) => {
        const status = getStockLevelStatus(stockLevel);
        const location = stockLevel.locationId
          ? locationsMap.get(stockLevel.locationId)
          : null;

        // Calculate capacity percentage if max is defined
        const capacityPercentage =
          stockLevel.maximumQuantity && stockLevel.maximumQuantity > 0
            ? (stockLevel.availableQuantity / stockLevel.maximumQuantity) * 100
            : null;

        return (
          <Card key={stockLevel.stockLevelId}>
            <CardContent>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
                <Box>
                  <Typography variant="h6" component="div">
                    {location ? location.barcode || `Location ${stockLevel.locationId}` : 'All Locations'}
                  </Typography>
                  {location && location.coordinates && (
                    <Typography variant="body2" color="text.secondary">
                      {location.coordinates.zone}-{location.coordinates.aisle}-{location.coordinates.rack}-{location.coordinates.level}
                    </Typography>
                  )}
                </Box>
                <Chip
                  label={getStatusLabel(status)}
                  color={getStatusColor(status)}
                  size="small"
                />
              </Box>

              <Box sx={{ mb: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="body2" color="text.secondary">
                    Available Quantity
                  </Typography>
                  <Typography variant="body2" fontWeight="bold">
                    {stockLevel.availableQuantity}
                  </Typography>
                </Box>
                {capacityPercentage !== null && (
                  <LinearProgress
                    variant="determinate"
                    value={Math.min(capacityPercentage, 100)}
                    color={getStatusColor(status)}
                    sx={{ height: 8, borderRadius: 4 }}
                  />
                )}
              </Box>

              <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 2 }}>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Total Quantity
                  </Typography>
                  <Typography variant="body2">{stockLevel.totalQuantity}</Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Allocated Quantity
                  </Typography>
                  <Typography variant="body2">{stockLevel.allocatedQuantity}</Typography>
                </Box>
                {stockLevel.minimumQuantity !== undefined && (
                  <Box>
                    <Typography variant="caption" color="text.secondary">
                      Minimum Threshold
                    </Typography>
                    <Typography variant="body2">{stockLevel.minimumQuantity}</Typography>
                  </Box>
                )}
                {stockLevel.maximumQuantity !== undefined && (
                  <Box>
                    <Typography variant="caption" color="text.secondary">
                      Maximum Threshold
                    </Typography>
                    <Typography variant="body2">{stockLevel.maximumQuantity}</Typography>
                  </Box>
                )}
              </Box>

              {(status === 'CRITICAL' || status === 'LOW' || status === 'ABOVE_MAX') && (
                <Alert severity={getStatusColor(status)} sx={{ mt: 2 }}>
                  {status === 'CRITICAL' &&
                    `Stock is critically low. Current: ${stockLevel.availableQuantity}, Minimum: ${stockLevel.minimumQuantity}`}
                  {status === 'LOW' &&
                    `Stock is below minimum threshold. Current: ${stockLevel.availableQuantity}, Minimum: ${stockLevel.minimumQuantity}`}
                  {status === 'ABOVE_MAX' &&
                    `Stock exceeds maximum threshold. Current: ${stockLevel.availableQuantity}, Maximum: ${stockLevel.maximumQuantity}`}
                </Alert>
              )}
            </CardContent>
          </Card>
        );
      })}
    </Box>
  );
};
