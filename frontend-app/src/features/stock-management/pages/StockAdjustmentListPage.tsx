import { Button } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useCallback, useMemo, useState } from 'react';
import { Add as AddIcon } from '@mui/icons-material';

import { ListPageLayout } from '../../../components/layouts';
import { EmptyState, FilterBar, Pagination } from '../../../components/common';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';
import { StockAdjustmentList } from '../components/StockAdjustmentList';
import { useStockAdjustments } from '../hooks/useStockAdjustments';
import { StockAdjustmentFilters } from '../types/stockManagement';

export const StockAdjustmentListPage = () => {
  const navigate = useNavigate();

  // Filter state
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [productIdFilter, setProductIdFilter] = useState('');

  // Memoize filters object
  const filters: StockAdjustmentFilters = useMemo(
    () => ({
      page,
      size,
      productId: productIdFilter || undefined,
    }),
    [page, size, productIdFilter]
  );

  const { data: queryResponse, isLoading, error } = useStockAdjustments(filters);
  const adjustments = queryResponse?.data?.adjustments || [];
  const totalCount = queryResponse?.data?.totalCount || 0;
  const totalPages = Math.ceil(totalCount / size);

  const handleClearFilters = useCallback(() => {
    setProductIdFilter('');
    setPage(0);
  }, []);

  const handlePageChange = useCallback((newPage: number) => {
    setPage(newPage - 1); // Convert to 0-based index
  }, []);

  const hasActiveFilters = Boolean(productIdFilter);

  return (
    <ListPageLayout
      breadcrumbs={getBreadcrumbs.stockAdjustmentList()}
      title="Stock Adjustments"
      description="View and manage stock level adjustments"
      actions={
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => navigate(Routes.stockAdjustmentCreate)}
        >
          Adjust Stock
        </Button>
      }
      isLoading={isLoading}
      error={error?.message || null}
    >
      <FilterBar onClearFilters={handleClearFilters} hasActiveFilters={hasActiveFilters}>
        <div />
      </FilterBar>

      {adjustments.length === 0 ? (
        <EmptyState
          title="No stock adjustments found"
          description={
            hasActiveFilters
              ? 'Try adjusting your filters'
              : 'Create your first stock adjustment to get started'
          }
          action={
            !hasActiveFilters
              ? {
                  label: 'Adjust Stock',
                  onClick: () => navigate(Routes.stockAdjustmentCreate),
                }
              : undefined
          }
        />
      ) : (
        <>
          <StockAdjustmentList adjustments={adjustments} error={error} />

          {totalPages > 1 && (
            <Pagination
              currentPage={page + 1}
              totalPages={totalPages}
              totalItems={totalCount}
              itemsPerPage={size}
              onPageChange={handlePageChange}
            />
          )}
        </>
      )}
    </ListPageLayout>
  );
};

