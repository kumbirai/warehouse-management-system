import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  Typography,
} from '@mui/material';
import { useEffect, useState } from 'react';
import { useAssignLocationToStock } from '../hooks/useAssignLocationToStock';
import { StockItem } from '../types/stockManagement';
import { BarcodeInput } from '../../../components/common';
import { useLocations } from '../../location-management/hooks/useLocations';
import { useAuth } from '../../../hooks/useAuth';
import { logger } from '../../../utils/logger';

interface AssignLocationToStockFormProps {
  open: boolean;
  onClose: () => void;
  stockItem: StockItem | null;
  onSuccess?: () => void;
}

/**
 * AssignLocationToStockForm Component
 * <p>
 * Form for assigning a location to a stock item.
 * Supports barcode scanning for location selection.
 */
export const AssignLocationToStockForm = ({
  open,
  onClose,
  stockItem,
  onSuccess,
}: AssignLocationToStockFormProps) => {
  const [locationInput, setLocationInput] = useState('');
  const [quantity, setQuantity] = useState<number>(stockItem?.quantity || 0);
  const [barcodeError, setBarcodeError] = useState<string | null>(null);
  const [isSearching, setIsSearching] = useState(false);
  const assignLocation = useAssignLocationToStock();
  const { user } = useAuth();

  // Search for location by barcode when barcode is scanned
  const { locations: searchResults, refetch: searchLocations } = useLocations({
    tenantId: user?.tenantId || undefined,
    search: isSearching ? locationInput : undefined,
    size: 5,
  });

  // Reset form when dialog opens/closes
  useEffect(() => {
    if (open) {
      setLocationInput('');
      setQuantity(stockItem?.quantity || 0);
      setBarcodeError(null);
      setIsSearching(false);
    }
  }, [open, stockItem?.quantity]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!stockItem || !locationInput) return;

    // If we have a search result, use the locationId, otherwise use the input directly
    let locationIdToUse = locationInput;
    if (isSearching && searchResults && searchResults.length === 1) {
      locationIdToUse = searchResults[0].locationId;
    }

    try {
      await assignLocation.mutateAsync({
        stockItemId: stockItem.stockItemId,
        request: {
          locationId: locationIdToUse,
          quantity,
        },
      });
      onSuccess?.();
      onClose();
    } catch (error) {
      // Error is handled by the mutation
      logger.error(
        'Failed to assign location',
        error instanceof Error ? error : new Error(String(error)),
        {
          stockItemId: stockItem?.stockItemId,
          locationId: locationIdToUse,
        }
      );
    }
  };

  // Handle barcode scan - search for location by barcode
  const handleBarcodeScan = async (barcode: string) => {
    setLocationInput(barcode);
    setBarcodeError(null);
    setIsSearching(true);

    // Trigger search
    try {
      await searchLocations();
    } catch (error) {
      setBarcodeError('Failed to search for location');
      setIsSearching(false);
    }
  };

  // Check search results when they arrive
  useEffect(() => {
    if (isSearching && searchResults !== undefined) {
      if (searchResults.length === 0) {
        setBarcodeError(`Location with barcode "${locationInput}" not found`);
        setIsSearching(false);
      } else if (searchResults.length === 1) {
        // Single match - auto-select
        setLocationInput(searchResults[0].locationId);
        setBarcodeError(null);
        setIsSearching(false);
      } else {
        // Multiple matches - show error asking for more specific search
        setBarcodeError(`Multiple locations found. Please enter location ID directly.`);
        setIsSearching(false);
      }
    }
  }, [isSearching, searchResults, locationInput]);

  const handleClose = () => {
    if (!assignLocation.isPending && !isSearching) {
      onClose();
    }
  };

  if (!stockItem) {
    return null;
  }

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle>Assign Location to Stock Item</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 1 }}>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              Stock Item ID: {stockItem.stockItemId}
            </Typography>
            {stockItem.locationId && (
              <Alert severity="info" sx={{ mb: 2 }}>
                Current location: {stockItem.locationId}
              </Alert>
            )}
            <BarcodeInput
              label="Location Barcode or ID"
              value={locationInput}
              onChange={value => {
                setLocationInput(value);
                setBarcodeError(null);
                setIsSearching(false);
              }}
              onScan={handleBarcodeScan}
              fullWidth
              required
              margin="normal"
              disabled={assignLocation.isPending || isSearching}
              helperText="Scan location barcode or enter location ID"
              autoFocus
            />
            {isSearching && (
              <Alert severity="info" sx={{ mt: 1 }}>
                Searching for location...
              </Alert>
            )}
            {barcodeError && (
              <Alert severity="error" sx={{ mt: 1 }}>
                {barcodeError}
              </Alert>
            )}
            <TextField
              label="Quantity"
              type="number"
              value={quantity}
              onChange={e => setQuantity(Number(e.target.value))}
              fullWidth
              required
              margin="normal"
              disabled={assignLocation.isPending || isSearching}
              inputProps={{ min: 1, max: stockItem.quantity }}
              helperText={`Maximum quantity: ${stockItem.quantity}`}
            />
            {assignLocation.isError && (
              <Alert severity="error" sx={{ mt: 2 }}>
                {assignLocation.error instanceof Error
                  ? assignLocation.error.message
                  : 'Failed to assign location'}
              </Alert>
            )}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleClose} disabled={assignLocation.isPending || isSearching}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            color="primary"
            disabled={assignLocation.isPending || !locationInput || quantity <= 0 || isSearching}
            startIcon={assignLocation.isPending ? <CircularProgress size={16} /> : null}
          >
            {assignLocation.isPending ? 'Assigning...' : 'Assign Location'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};
