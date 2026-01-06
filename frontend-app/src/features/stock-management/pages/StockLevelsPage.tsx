import { useState } from 'react';
import { FormControl, InputLabel, MenuItem, Select, Box, Typography } from '@mui/material';
import { useStockLevels } from '../hooks/useStockLevels';
import { StockLevelList } from '../components/StockLevelList';
import { ListPageLayout } from '../../../components/layouts/ListPageLayout';
import { FilterBar } from '../../../components/common/FilterBar';
import { getBreadcrumbs } from '../../../utils/navigationUtils';
import { useProducts } from '../../product-management/hooks/useProducts';
import { useAuth } from '../../../hooks/useAuth';

export const StockLevelsPage = () => {
  const { user } = useAuth();
  const [selectedProductId, setSelectedProductId] = useState<string>('');

  const { products } = useProducts({
    page: 0,
    size: 1000,
    tenantId: user?.tenantId || '',
  });

  const { data: stockLevelsResponse, isLoading, error } = useStockLevels({
    productId: selectedProductId,
  });

  const stockLevels = stockLevelsResponse?.data || [];

  const hasActiveFilters = !!selectedProductId;

  const handleClearFilters = () => {
    setSelectedProductId('');
  };

  return (
    <ListPageLayout
      breadcrumbs={getBreadcrumbs.stockLevels()}
      title="Stock Levels Monitoring"
      description="Monitor stock levels with min/max threshold alerts"
      isLoading={isLoading}
      error={error?.message || null}
    >
      <FilterBar onClearFilters={handleClearFilters} hasActiveFilters={hasActiveFilters}>
        <FormControl size="small" sx={{ minWidth: 250 }}>
          <InputLabel id="product-filter-label">Product</InputLabel>
          <Select
            labelId="product-filter-label"
            value={selectedProductId}
            label="Product"
            onChange={(e) => setSelectedProductId(e.target.value)}
            aria-label="Filter stock levels by product"
            required
          >
            <MenuItem value="">
              <em>Select a product</em>
            </MenuItem>
            {products.map((product) => (
              <MenuItem key={product.productId} value={product.productId}>
                {product.productCode} - {product.description}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </FilterBar>

      {!selectedProductId ? (
        <Box sx={{ textAlign: 'center', py: 4 }}>
          <Typography variant="body1" color="text.secondary">
            Please select a product to view stock levels
          </Typography>
        </Box>
      ) : (
        <StockLevelList stockLevels={stockLevels} />
      )}
    </ListPageLayout>
  );
};
