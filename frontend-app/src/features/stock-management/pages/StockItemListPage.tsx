import {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {Box, Button, FormControl, InputLabel, MenuItem, Select} from '@mui/material';
import {FilterList as FilterIcon} from '@mui/icons-material';

import {ListPageLayout} from '../../../components/layouts';
import {getBreadcrumbs, Routes} from '../../../utils/navigationUtils';
import {StockItemList} from '../components/StockItemList';
import {useStockItemsByClassification} from '../hooks/useStockItemsByClassification';
import {StockClassification} from '../types/stockManagement';
import {EmptyState, FilterBar} from '../../../components/common';

/**
 * StockItemListPage
 * <p>
 * Page for listing stock items with classification filtering.
 * Follows mandated frontend templates and CQRS principles.
 */
export const StockItemListPage = () => {
  const navigate = useNavigate();
  const [selectedClassification, setSelectedClassification] = useState<StockClassification | null>(
    null
  );

  // Query stock items by classification (if filter is selected)
  const {
    data: queryResponse,
    isLoading,
    error,
  } = useStockItemsByClassification(selectedClassification);

  const stockItems = queryResponse?.data?.stockItems || [];
  const hasFilter = selectedClassification !== null;

  const handleClassificationChange = (classification: string) => {
    if (classification === 'ALL') {
      setSelectedClassification(null);
    } else {
      setSelectedClassification(classification as StockClassification);
    }
  };

  const handleClearFilters = () => {
    setSelectedClassification(null);
  };

  return (
    <ListPageLayout
      breadcrumbs={getBreadcrumbs.stockItemList()}
      title="Stock Items"
      description="View and manage stock items with classification and location information"
      actions={
        <Button
          variant="outlined"
          onClick={() => navigate(Routes.consignments)}
          startIcon={<FilterIcon />}
        >
          View Consignments
        </Button>
      }
      isLoading={isLoading}
      error={error?.message || null}
    >
      <FilterBar onClearFilters={handleClearFilters} hasActiveFilters={hasFilter}>
        <FormControl sx={{ minWidth: 200 }}>
          <InputLabel>Classification</InputLabel>
          <Select
            value={selectedClassification || 'ALL'}
            onChange={e => handleClassificationChange(e.target.value)}
            label="Classification"
          >
            <MenuItem value="ALL">All Classifications</MenuItem>
            <MenuItem value="EXPIRED">Expired</MenuItem>
            <MenuItem value="CRITICAL">Critical</MenuItem>
            <MenuItem value="NEAR_EXPIRY">Near Expiry</MenuItem>
            <MenuItem value="NORMAL">Normal</MenuItem>
            <MenuItem value="EXTENDED_SHELF_LIFE">Extended Shelf Life</MenuItem>
          </Select>
        </FormControl>
      </FilterBar>

      <Box sx={{ mt: 3 }}>
        {!hasFilter ? (
          <EmptyState
            title="Select a Classification"
            description="Please select a classification filter to view stock items. Stock items are automatically classified based on expiration dates."
          />
        ) : (
          <StockItemList stockItems={stockItems} error={error} isLoading={isLoading} />
        )}
      </Box>
    </ListPageLayout>
  );
};
