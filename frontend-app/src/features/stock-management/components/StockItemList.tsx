import { Box, Button, Chip, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { Column, EmptyState, ResponsiveTable } from '../../../components/common';
import { StockItem } from '../types/stockManagement';
import { Routes } from '../../../utils/navigationUtils';
import { formatDate } from '../../../utils/dateUtils';
import { StockClassificationBadge } from './StockClassificationBadge';
import { LocationOn as LocationIcon } from '@mui/icons-material';

interface StockItemListProps {
  stockItems: StockItem[];
  error: Error | null;
  isLoading?: boolean;
}

/**
 * StockItemList Component
 * <p>
 * Displays a list of stock items with classification badges and location information.
 */
export const StockItemList = ({ stockItems, error, isLoading }: StockItemListProps) => {
  const navigate = useNavigate();

  if (error) {
    return <Typography color="error">Error loading stock items: {error.message}</Typography>;
  }

  if (isLoading) {
    return null; // Loading handled by parent
  }

  if (!Array.isArray(stockItems) || stockItems.length === 0) {
    return (
      <EmptyState
        title="No stock items found"
        description="Stock items will appear here after consignments are confirmed"
      />
    );
  }

  const columns: Column<StockItem>[] = [
    {
      key: 'stockItemId',
      label: 'Stock Item ID',
      render: stockItem => (
        <Typography variant="body2" fontWeight="medium" sx={{ fontFamily: 'monospace' }}>
          {stockItem.stockItemId.substring(0, 8)}...
        </Typography>
      ),
    },
    {
      key: 'productId',
      label: 'Product ID',
      render: stockItem => (
        <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
          {stockItem.productId.substring(0, 8)}...
        </Typography>
      ),
      hideOnMobile: true,
    },
    {
      key: 'classification',
      label: 'Classification',
      render: stockItem => <StockClassificationBadge classification={stockItem.classification} />,
    },
    {
      key: 'quantity',
      label: 'Quantity',
      render: stockItem => <Typography variant="body2">{stockItem.quantity}</Typography>,
    },
    {
      key: 'locationId',
      label: 'Location',
      render: stockItem => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
          {stockItem.locationId ? (
            <>
              <LocationIcon fontSize="small" color="action" />
              <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                {stockItem.locationId.substring(0, 8)}...
              </Typography>
            </>
          ) : (
            <Chip label="Unassigned" size="small" variant="outlined" color="warning" />
          )}
        </Box>
      ),
    },
    {
      key: 'expirationDate',
      label: 'Expiration Date',
      render: stockItem => (
        <Typography variant="body2">
          {stockItem.expirationDate ? formatDate(stockItem.expirationDate) : 'N/A'}
        </Typography>
      ),
      hideOnMobile: true,
    },
    {
      key: 'actions',
      label: 'Actions',
      hideOnMobile: true,
      render: stockItem => (
        <Button
          size="small"
          variant="outlined"
          onClick={() => navigate(Routes.stockItemDetail(stockItem.stockItemId))}
        >
          View
        </Button>
      ),
    },
  ];

  return (
    <ResponsiveTable
      data={stockItems}
      columns={columns}
      getRowKey={stockItem => stockItem.stockItemId}
      onRowClick={stockItem => navigate(Routes.stockItemDetail(stockItem.stockItemId))}
      emptyMessage="No stock items found"
    />
  );
};
