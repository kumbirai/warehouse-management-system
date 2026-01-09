import { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  FormControlLabel,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { StockItem } from '../../stock-management/types/stockManagement';
import { AssignLocationsFEFORequest, StockItemAssignmentRequest } from '../types/location';
import { formatDate } from '../../../utils/dateUtils';
import { StockClassificationBadge } from '../../stock-management/components/StockClassificationBadge';

interface FEFOAssignmentFormProps {
  stockItems: StockItem[];
  onSubmit: (request: AssignLocationsFEFORequest) => Promise<void>;
  isLoading?: boolean;
}

export const FEFOAssignmentForm = ({
  stockItems,
  onSubmit,
  isLoading = false,
}: FEFOAssignmentFormProps) => {
  const [selectedStockItemIds, setSelectedStockItemIds] = useState<Set<string>>(new Set());

  // Filter to only unassigned stock items (no locationId)
  const unassignedStockItems = stockItems.filter(item => !item.locationId);

  const handleToggleSelection = (stockItemId: string) => {
    const newSelection = new Set(selectedStockItemIds);
    if (newSelection.has(stockItemId)) {
      newSelection.delete(stockItemId);
    } else {
      newSelection.add(stockItemId);
    }
    setSelectedStockItemIds(newSelection);
  };

  const handleSelectAll = () => {
    if (selectedStockItemIds.size === unassignedStockItems.length) {
      setSelectedStockItemIds(new Set());
    } else {
      setSelectedStockItemIds(new Set(unassignedStockItems.map(item => item.stockItemId)));
    }
  };

  const handleSubmit = async () => {
    if (selectedStockItemIds.size === 0) {
      return;
    }

    const stockItemRequests: StockItemAssignmentRequest[] = unassignedStockItems
      .filter(item => selectedStockItemIds.has(item.stockItemId))
      .map(item => ({
        stockItemId: item.stockItemId,
        quantity: item.quantity,
        expirationDate: item.expirationDate,
        classification: item.classification,
      }));

    await onSubmit({ stockItems: stockItemRequests });
  };

  if (unassignedStockItems.length === 0) {
    return (
      <Alert severity="info">
        No unassigned stock items found. All stock items already have locations assigned.
      </Alert>
    );
  }

  const allSelected = selectedStockItemIds.size === unassignedStockItems.length;
  const someSelected =
    selectedStockItemIds.size > 0 && selectedStockItemIds.size < unassignedStockItems.length;

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        Select Stock Items for FEFO Assignment
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Select unassigned stock items to automatically assign locations using the
        First-Expiring-First-Out (FEFO) algorithm. The system will prioritize items with earlier
        expiration dates.
      </Typography>

      <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <FormControlLabel
          control={
            <Checkbox
              checked={allSelected}
              indeterminate={someSelected}
              onChange={handleSelectAll}
              aria-label="Select all stock items"
            />
          }
          label={`${selectedStockItemIds.size} of ${unassignedStockItems.length} selected`}
        />
        <Button
          variant="contained"
          onClick={handleSubmit}
          disabled={selectedStockItemIds.size === 0 || isLoading}
          aria-label="Assign locations using FEFO"
        >
          {isLoading ? 'Assigning...' : `Assign Locations (${selectedStockItemIds.size})`}
        </Button>
      </Box>

      <TableContainer component={Paper} variant="outlined">
        <Table>
          <TableHead>
            <TableRow>
              <TableCell padding="checkbox" />
              <TableCell>Product Code</TableCell>
              <TableCell>Product Description</TableCell>
              <TableCell>Quantity</TableCell>
              <TableCell>Expiration Date</TableCell>
              <TableCell>Classification</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {unassignedStockItems.map(item => (
              <TableRow
                key={item.stockItemId}
                hover
                selected={selectedStockItemIds.has(item.stockItemId)}
                onClick={() => handleToggleSelection(item.stockItemId)}
                sx={{ cursor: 'pointer' }}
              >
                <TableCell padding="checkbox">
                  <Checkbox
                    checked={selectedStockItemIds.has(item.stockItemId)}
                    onChange={() => handleToggleSelection(item.stockItemId)}
                    onClick={e => e.stopPropagation()}
                    aria-label={`Select stock item ${item.stockItemId}`}
                  />
                </TableCell>
                <TableCell>
                  <Typography sx={{ fontFamily: 'monospace', fontWeight: 'medium' }}>
                    {item.productCode || 'N/A'}
                  </Typography>
                </TableCell>
                <TableCell>{item.productDescription || 'N/A'}</TableCell>
                <TableCell>{item.quantity}</TableCell>
                <TableCell>
                  {item.expirationDate ? formatDate(item.expirationDate) : 'N/A'}
                </TableCell>
                <TableCell>
                  <StockClassificationBadge classification={item.classification} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Paper>
  );
};
