import {
  Button,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Tab,
  Tabs,
  TextField,
} from '@mui/material';
import { useCallback, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Add as AddIcon, UploadFile as UploadFileIcon } from '@mui/icons-material';

import { ListPageLayout } from '../../../components/layouts';
import { BarcodeInput, EmptyState, FilterBar, Pagination } from '../../../components/common';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';
import { ProductList } from '../components/ProductList';
import { useProducts } from '../hooks/useProducts';
import { useAuth } from '../../../hooks/useAuth';
import { ProductCsvUploadForm } from '../components/ProductCsvUploadForm';
import { useUploadProductCsv } from '../hooks/useUploadProductCsv';
import { ProductListFilters } from '../types/product';

export const ProductListPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState(0);

  // Filter state
  const [page, setPage] = useState(0);
  const [size] = useState(100);
  const [categoryFilter, setCategoryFilter] = useState('');
  const [brandFilter, setBrandFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // Memoize filters object to prevent infinite loop in useProducts hook
  const filters: ProductListFilters = useMemo(
    () => ({
      tenantId: user?.tenantId ?? undefined,
      page,
      size,
      category: categoryFilter || undefined,
      brand: brandFilter || undefined,
      search: searchQuery || undefined,
    }),
    [user?.tenantId, page, size, categoryFilter, brandFilter, searchQuery]
  );

  const { products, isLoading, error, pagination } = useProducts(filters);
  const { uploadCsv, isLoading: isUploading } = useUploadProductCsv();

  // Filter handlers
  const handleCategoryChange = useCallback((newCategory: string) => {
    setCategoryFilter(newCategory);
    setPage(0);
  }, []);

  const handleBrandChange = useCallback((newBrand: string) => {
    setBrandFilter(newBrand);
    setPage(0);
  }, []);

  const handleSearchChange = useCallback((query: string) => {
    setSearchQuery(query);
    setPage(0);
  }, []);

  const handleClearFilters = useCallback(() => {
    setCategoryFilter('');
    setBrandFilter('');
    setSearchQuery('');
    setPage(0);
  }, []);

  const handlePageChange = useCallback((newPage: number) => {
    setPage(newPage - 1); // Convert 1-based to 0-based
  }, []);

  const hasActiveFilters = !!searchQuery || !!categoryFilter || !!brandFilter;

  const handleUpload = async (file: File) => {
    if (!user?.tenantId) {
      throw new Error('Tenant ID is required');
    }
    return await uploadCsv(file, user.tenantId);
  };

  const canCreate =
    user?.roles?.some(role =>
      ['SYSTEM_ADMIN', 'TENANT_ADMIN', 'WAREHOUSE_MANAGER'].includes(role)
    ) ?? false;

  return (
    <ListPageLayout
      breadcrumbs={getBreadcrumbs.productList()}
      title="Products"
      description="Manage product master data"
      actions={
        <>
          <Button variant="outlined" startIcon={<UploadFileIcon />} onClick={() => setActiveTab(1)}>
            Upload CSV
          </Button>
          {canCreate && (
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => navigate(Routes.productCreate)}
            >
              Create Product
            </Button>
          )}
        </>
      }
      isLoading={isLoading}
      error={error?.message || null}
      maxWidth="lg"
    >
      <Tabs value={activeTab} onChange={(_, newValue) => setActiveTab(newValue)} sx={{ mb: 3 }}>
        <Tab label="Product List" />
        <Tab label="CSV Upload" />
      </Tabs>

      {activeTab === 0 && (
        <>
          <FilterBar onClearFilters={handleClearFilters} hasActiveFilters={hasActiveFilters}>
            <BarcodeInput
              label="Search"
              placeholder="Search by product code, barcode, or description..."
              value={searchQuery}
              onChange={handleSearchChange}
              onScan={barcode => {
                handleSearchChange(barcode);
              }}
              fullWidth
              size="small"
              autoSubmitOnEnter={true}
            />
            <FormControl size="small" sx={{ minWidth: 180 }}>
              <InputLabel>Category</InputLabel>
              <Select
                value={categoryFilter}
                onChange={e => handleCategoryChange(e.target.value)}
                label="Category"
              >
                <MenuItem value="">All Categories</MenuItem>
                <MenuItem value="BEVERAGES">Beverages</MenuItem>
                <MenuItem value="FOOD">Food</MenuItem>
                <MenuItem value="SNACKS">Snacks</MenuItem>
              </Select>
            </FormControl>
            <TextField
              label="Brand"
              placeholder="Filter by brand..."
              value={brandFilter}
              onChange={e => handleBrandChange(e.target.value)}
              size="small"
              sx={{ minWidth: 180 }}
            />
          </FilterBar>

          {products.length === 0 && !isLoading ? (
            <EmptyState
              title="No products found"
              description={
                hasActiveFilters
                  ? 'Try adjusting your filters to see more results'
                  : 'Get started by creating your first product or uploading a CSV file'
              }
              action={
                canCreate
                  ? {
                      label: 'Create Product',
                      onClick: () => navigate(Routes.productCreate),
                    }
                  : undefined
              }
            />
          ) : (
            <>
              <ProductList products={products} error={error} />
              {pagination && pagination.totalPages > 1 && (
                <Pagination
                  currentPage={pagination.currentPage}
                  totalPages={pagination.totalPages}
                  totalItems={pagination.totalItems}
                  itemsPerPage={pagination.itemsPerPage}
                  onPageChange={handlePageChange}
                />
              )}
            </>
          )}
        </>
      )}

      {activeTab === 1 && <ProductCsvUploadForm onUpload={handleUpload} isLoading={isUploading} />}
    </ListPageLayout>
  );
};
