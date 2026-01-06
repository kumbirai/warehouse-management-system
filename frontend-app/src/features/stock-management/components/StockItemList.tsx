import { Box, Button, Chip, Collapse, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useState, useMemo } from 'react';
import { EmptyState } from '../../../components/common';
import { StockItem } from '../types/stockManagement';
import { Routes } from '../../../utils/navigationUtils';
import { formatDate } from '../../../utils/dateUtils';
import { StockClassificationBadge } from './StockClassificationBadge';
import { LocationOn as LocationIcon, ExpandMore as ExpandMoreIcon, ExpandLess as ExpandLessIcon } from '@mui/icons-material';

interface StockItemListProps {
  stockItems: StockItem[];
  error: Error | null;
  isLoading?: boolean;
}

interface GroupedStockItem {
  productId: string;
  productCode: string | undefined;
  productDescription: string | undefined;
  totalQuantity: number;
  locations: Array<{
    locationId: string | undefined;
    locationCode: string | undefined;
    locationName: string | undefined;
    quantity: number;
    stockItemId: string;
    classification: StockItem['classification'];
    expirationDate?: string;
  }>;
}

/**
 * StockItemList Component
 * <p>
 * Displays a grouped list of stock items with classification badges and location information.
 * Groups items by product and shows grand totals for each product across different locations.
 */
export const StockItemList = ({ stockItems, error, isLoading }: StockItemListProps) => {
  const navigate = useNavigate();
  const [expandedProducts, setExpandedProducts] = useState<Set<string>>(new Set());

  // Group stock items by product
  // Must be called before any early returns to follow React Hooks rules
  const groupedItems = useMemo(() => {
    const grouped = new Map<string, GroupedStockItem>();

    stockItems.forEach(item => {
      const productId = item.productId;
      if (!grouped.has(productId)) {
        grouped.set(productId, {
          productId,
          productCode: item.productCode,
          productDescription: item.productDescription,
          totalQuantity: 0,
          locations: [],
        });
      }

      const group = grouped.get(productId)!;
      group.totalQuantity += item.quantity;
      group.locations.push({
        locationId: item.locationId,
        locationCode: item.locationCode,
        locationName: item.locationName,
        quantity: item.quantity,
        stockItemId: item.stockItemId,
        classification: item.classification,
        expirationDate: item.expirationDate,
      });
    });

    return Array.from(grouped.values());
  }, [stockItems]);

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

  const toggleProduct = (productId: string) => {
    const newExpanded = new Set(expandedProducts);
    if (newExpanded.has(productId)) {
      newExpanded.delete(productId);
    } else {
      newExpanded.add(productId);
    }
    setExpandedProducts(newExpanded);
  };

  const getProductDisplayName = (item: GroupedStockItem): string => {
    if (item.productCode) {
      return item.productDescription ? `${item.productCode} - ${item.productDescription}` : item.productCode;
    }
    if (item.productDescription) {
      return item.productDescription;
    }
    return item.productId.substring(0, 8) + '...';
  };

  const getLocationDisplayName = (location: GroupedStockItem['locations'][0]): string => {
    if (location.locationName) {
      return location.locationCode ? `${location.locationCode} - ${location.locationName}` : location.locationName;
    }
    if (location.locationCode) {
      return location.locationCode;
    }
    if (location.locationId) {
      return location.locationId.substring(0, 8) + '...';
    }
    return 'Unassigned';
  };

  return (
    <Table>
      <TableHead>
        <TableRow>
          <TableCell sx={{ width: '50px' }}></TableCell>
          <TableCell>Product</TableCell>
          <TableCell>Classification</TableCell>
          <TableCell align="right">Total Quantity</TableCell>
          <TableCell>Locations</TableCell>
          <TableCell sx={{ display: { xs: 'none', md: 'table-cell' } }}>Expiration Date</TableCell>
          <TableCell sx={{ display: { xs: 'none', md: 'table-cell' } }}>Actions</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {groupedItems.map(group => {
          const isExpanded = expandedProducts.has(group.productId);
          return (
            <>
              <TableRow
                key={group.productId}
                hover
                sx={{ cursor: 'pointer', backgroundColor: isExpanded ? 'action.hover' : 'inherit' }}
                onClick={() => toggleProduct(group.productId)}
              >
                <TableCell>
                  {isExpanded ? <ExpandLessIcon /> : <ExpandMoreIcon />}
                </TableCell>
                <TableCell>
                  <Typography variant="body2" fontWeight="medium">
                    {getProductDisplayName(group)}
                  </Typography>
                  <Typography variant="caption" color="text.secondary" sx={{ fontFamily: 'monospace' }}>
                    ID: {group.productId.substring(0, 8)}...
                  </Typography>
                </TableCell>
                <TableCell>
                  <StockClassificationBadge classification={group.locations[0]?.classification || 'NORMAL'} />
                </TableCell>
                <TableCell align="right">
                  <Typography variant="body2" fontWeight="bold">
                    {group.totalQuantity}
                  </Typography>
                </TableCell>
                <TableCell>
                  <Typography variant="body2" color="text.secondary">
                    {group.locations.length} location{group.locations.length !== 1 ? 's' : ''}
                  </Typography>
                </TableCell>
                <TableCell sx={{ display: { xs: 'none', md: 'table-cell' } }}>
                  <Typography variant="body2" color="text.secondary">
                    {group.locations[0]?.expirationDate ? formatDate(group.locations[0].expirationDate) : 'N/A'}
                  </Typography>
                </TableCell>
                <TableCell sx={{ display: { xs: 'none', md: 'table-cell' } }}>
                  <Button
                    size="small"
                    variant="outlined"
                    onClick={e => {
                      e.stopPropagation();
                      navigate(Routes.stockItemDetail(group.locations[0]?.stockItemId || ''));
                    }}
                  >
                    View
                  </Button>
                </TableCell>
              </TableRow>
              <TableRow>
                <TableCell colSpan={7} sx={{ py: 0, border: 0 }}>
                  <Collapse in={isExpanded} timeout="auto" unmountOnExit>
                    <Box sx={{ margin: 2 }}>
                      <Typography variant="subtitle2" gutterBottom>
                        Location Details
                      </Typography>
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell>Location</TableCell>
                            <TableCell>Classification</TableCell>
                            <TableCell align="right">Quantity</TableCell>
                            <TableCell>Expiration Date</TableCell>
                            <TableCell>Actions</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {group.locations.map(location => (
                            <TableRow key={location.stockItemId} hover>
                              <TableCell>
                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                                  {location.locationId ? (
                                    <>
                                      <LocationIcon fontSize="small" color="action" />
                                      <Typography variant="body2">
                                        {getLocationDisplayName(location)}
                                      </Typography>
                                    </>
                                  ) : (
                                    <Chip label="Unassigned" size="small" variant="outlined" color="warning" />
                                  )}
                                </Box>
                              </TableCell>
                              <TableCell>
                                <StockClassificationBadge classification={location.classification} />
                              </TableCell>
                              <TableCell align="right">
                                <Typography variant="body2">{location.quantity}</Typography>
                              </TableCell>
                              <TableCell>
                                <Typography variant="body2">
                                  {location.expirationDate ? formatDate(location.expirationDate) : 'N/A'}
                                </Typography>
                              </TableCell>
                              <TableCell>
                                <Button
                                  size="small"
                                  variant="outlined"
                                  onClick={() => navigate(Routes.stockItemDetail(location.stockItemId))}
                                >
                                  View
                                </Button>
                              </TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </Box>
                  </Collapse>
                </TableCell>
              </TableRow>
            </>
          );
        })}
      </TableBody>
    </Table>
  );
};
