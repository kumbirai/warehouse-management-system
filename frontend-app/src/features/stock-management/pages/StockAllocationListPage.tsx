import { Button } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useCallback, useMemo, useState } from 'react';
import { Add as AddIcon } from '@mui/icons-material';

import { ListPageLayout } from '../../../components/layouts';
import { EmptyState, FilterBar, Pagination } from '../../../components/common';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';
import { StockAllocationList } from '../components/StockAllocationList';
import { useStockAllocations } from '../hooks/useStockAllocations';
import { AllocationStatus, StockAllocationFilters } from '../types/stockManagement';

export const StockAllocationListPage = () => {
  const navigate = useNavigate();

  // Filter state
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [statusFilter, setStatusFilter] = useState<AllocationStatus | ''>('');
  const [productIdFilter, setProductIdFilter] = useState('');

  // Memoize filters object
  const filters: StockAllocationFilters = useMemo(
    () => ({
      page,
      size,
      status: statusFilter || undefined,
      productId: productIdFilter || undefined,
    }),
    [page, size, statusFilter, productIdFilter]
  );

  const { data: queryResponse, isLoading, error } = useStockAllocations(filters);
  const allocations = queryResponse?.data?.allocations || [];
  const totalCount = queryResponse?.data?.totalCount || 0;
  const totalPages = Math.ceil(totalCount / size);

  const handleClearFilters = useCallback(() => {
    setStatusFilter('');
    setProductIdFilter('');
    setPage(0);
  }, []);

  const handlePageChange = useCallback((newPage: number) => {
    setPage(newPage - 1); // Convert to 0-based index
  }, []);

  const hasActiveFilters = Boolean(statusFilter || productIdFilter);

  return (
    <ListPageLayout
      breadcrumbs={getBreadcrumbs.stockAllocationList()}
      title="Stock Allocations"
      description="View and manage stock allocations for picking orders and reservations"
      actions={
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => navigate(Routes.stockAllocationCreate)}
        >
          Allocate Stock
        </Button>
      }
      isLoading={isLoading}
      error={error?.message || null}
    >
      <FilterBar onClearFilters={handleClearFilters} hasActiveFilters={hasActiveFilters}>
        <div />
      </FilterBar>

      {allocations.length === 0 ? (
        <EmptyState
          title="No stock allocations found"
          description={
            hasActiveFilters
              ? 'Try adjusting your filters'
              : 'Create your first stock allocation to get started'
          }
          action={
            !hasActiveFilters
              ? {
                  label: 'Allocate Stock',
                  onClick: () => navigate(Routes.stockAllocationCreate),
                }
              : undefined
          }
        />
      ) : (
        <>
          <StockAllocationList allocations={allocations} error={error} />

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
