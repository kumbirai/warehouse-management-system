import { Button, FormControl, Grid, InputLabel, MenuItem, Select, TextField } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useCallback, useMemo, useState } from 'react';
import { Add as AddIcon } from '@mui/icons-material';

import { ListPageLayout } from '../../../components/layouts';
import { EmptyState, FilterBar } from '../../../components/common';
import { StockMovementList } from '../components/StockMovementList';
import { useStockMovements } from '../hooks/useStockMovements';
import { useAuth } from '../../../hooks/useAuth';
import { StockMovementListFilters } from '../services/stockMovementService';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';

export const StockMovementListPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [page, setPage] = useState(0);
  const [size] = useState(100);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [sourceLocationFilter, setSourceLocationFilter] = useState('');

  const filters: StockMovementListFilters = useMemo(
    () => ({
      page,
      size,
      status: statusFilter || undefined,
      sourceLocationId: sourceLocationFilter || undefined,
    }),
    [page, size, statusFilter, sourceLocationFilter]
  );

  const { movements, isLoading, error } = useStockMovements(filters, user?.tenantId || '');

  const handleStatusChange = useCallback((newStatus: string) => {
    setStatusFilter(newStatus);
    setPage(0);
  }, []);

  const handleSourceLocationChange = useCallback((newLocation: string) => {
    setSourceLocationFilter(newLocation);
    setPage(0);
  }, []);

  const handleClearFilters = useCallback(() => {
    setStatusFilter('');
    setSourceLocationFilter('');
    setPage(0);
  }, []);

  const hasActiveFilters = Boolean(statusFilter || sourceLocationFilter);

  return (
    <ListPageLayout
      breadcrumbs={getBreadcrumbs.stockMovementList()}
      title="Stock Movements"
      description="Track and manage stock movements between locations"
      actions={
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => navigate(Routes.stockMovementCreate)}
        >
          Create Movement
        </Button>
      }
      isLoading={isLoading}
      error={error?.message || null}
    >
      <FilterBar onClearFilters={handleClearFilters} hasActiveFilters={hasActiveFilters}>
        <Grid container spacing={2} sx={{ flex: 1 }}>
          <Grid item xs={12} sm={4}>
            <TextField
              fullWidth
              label="Source Location ID"
              placeholder="Filter by source location"
              value={sourceLocationFilter}
              onChange={e => handleSourceLocationChange(e.target.value)}
            />
          </Grid>
          <Grid item xs={12} sm={3}>
            <FormControl fullWidth>
              <InputLabel>Status</InputLabel>
              <Select
                value={statusFilter}
                label="Status"
                onChange={e => handleStatusChange(e.target.value)}
              >
                <MenuItem value="">All</MenuItem>
                <MenuItem value="INITIATED">Initiated</MenuItem>
                <MenuItem value="COMPLETED">Completed</MenuItem>
                <MenuItem value="CANCELLED">Cancelled</MenuItem>
              </Select>
            </FormControl>
          </Grid>
        </Grid>
      </FilterBar>

      {!isLoading && movements.length === 0 ? (
        <EmptyState
          title="No stock movements found"
          description={
            hasActiveFilters
              ? 'Try adjusting your filters to find movements'
              : 'Create your first stock movement to start tracking'
          }
          action={
            !hasActiveFilters
              ? {
                  label: 'Create Movement',
                  onClick: () => navigate(Routes.stockMovementCreate),
                }
              : undefined
          }
        />
      ) : (
        <StockMovementList movements={movements} isLoading={isLoading} error={error} />
      )}
    </ListPageLayout>
  );
};
