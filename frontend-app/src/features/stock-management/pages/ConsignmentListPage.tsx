import { Button, FormControl, Grid, InputLabel, MenuItem, Select } from '@mui/material';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Add as AddIcon,
  Search as SearchIcon,
  UploadFile as UploadFileIcon,
} from '@mui/icons-material';

import { ListPageLayout } from '../../../components/layouts';
import { BarcodeInput, EmptyState, FilterBar, Pagination } from '../../../components/common';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';
import { ConsignmentList } from '../components/ConsignmentList';
import { useConsignments } from '../hooks/useConsignments';
import { useAuth } from '../../../hooks/useAuth';
import { ConsignmentListFilters, ConsignmentStatus } from '../types/stockManagement';

export const ConsignmentListPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  // Filter state
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [statusFilter, setStatusFilter] = useState<ConsignmentStatus | ''>('');
  const [searchQuery, setSearchQuery] = useState('');

  // Read expiringWithinDays from URL query parameter on mount
  const [expiringWithinDays, setExpiringWithinDays] = useState<number | undefined>(() => {
    const expiringParam = searchParams.get('expiringWithinDays');
    return expiringParam ? parseInt(expiringParam, 10) : undefined;
  });

  // Update expiringWithinDays when URL query parameter changes
  useEffect(() => {
    const expiringParam = searchParams.get('expiringWithinDays');
    const newValue = expiringParam ? parseInt(expiringParam, 10) : undefined;
    setExpiringWithinDays(newValue);
  }, [searchParams]);

  // Memoize filters object to prevent infinite loop in useConsignments hook
  const filters: ConsignmentListFilters = useMemo(
    () => ({
      page,
      size,
      status: statusFilter || undefined,
      search: searchQuery || undefined,
      expiringWithinDays,
    }),
    [page, size, statusFilter, searchQuery, expiringWithinDays]
  );

  const { consignments, isLoading, error, pagination } = useConsignments(
    filters,
    user?.tenantId ?? undefined
  );

  // Filter handlers
  const handleStatusChange = useCallback((newStatus: ConsignmentStatus | '') => {
    setStatusFilter(newStatus);
    setPage(0);
  }, []);

  const handleSearchChange = useCallback((query: string) => {
    setSearchQuery(query);
    setPage(0);
  }, []);

  const handleClearFilters = useCallback(() => {
    setStatusFilter('');
    setSearchQuery('');
    setExpiringWithinDays(undefined);
    setPage(0);
    // Clear URL query parameters
    setSearchParams({});
  }, [setSearchParams]);

  const handlePageChange = useCallback((newPage: number) => {
    setPage(newPage - 1); // Convert to 0-based index
  }, []);

  const hasActiveFilters = Boolean(statusFilter || searchQuery || expiringWithinDays);

  return (
    <ListPageLayout
      breadcrumbs={getBreadcrumbs.consignmentList()}
      title="Consignments"
      description="Manage stock consignments and track incoming inventory"
      actions={
        <>
          <Button
            variant="outlined"
            startIcon={<UploadFileIcon />}
            onClick={() => navigate(Routes.consignmentUploadCsv)}
            sx={{ mr: 1 }}
          >
            Upload CSV
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => navigate(Routes.consignmentCreate)}
          >
            Create Consignment
          </Button>
        </>
      }
      isLoading={isLoading}
      error={error?.message || null}
    >
      <FilterBar onClearFilters={handleClearFilters} hasActiveFilters={hasActiveFilters}>
        <Grid container spacing={2} sx={{ flex: 1 }}>
          <Grid item xs={12} sm={6}>
            <BarcodeInput
              fullWidth
              label="Search"
              placeholder="Search by reference, warehouse..."
              value={searchQuery}
              onChange={handleSearchChange}
              onScan={barcode => {
                handleSearchChange(barcode);
              }}
              autoSubmitOnEnter={true}
              InputProps={{
                startAdornment: <SearchIcon sx={{ mr: 1, color: 'text.secondary' }} />,
              }}
            />
          </Grid>
          <Grid item xs={12} sm={3}>
            <FormControl fullWidth>
              <InputLabel>Status</InputLabel>
              <Select
                value={statusFilter}
                label="Status"
                onChange={e => handleStatusChange(e.target.value as ConsignmentStatus | '')}
              >
                <MenuItem value="">All</MenuItem>
                <MenuItem value="RECEIVED">Received</MenuItem>
                <MenuItem value="CONFIRMED">Confirmed</MenuItem>
                <MenuItem value="CANCELLED">Cancelled</MenuItem>
              </Select>
            </FormControl>
          </Grid>
        </Grid>
      </FilterBar>

      {!isLoading && consignments.length === 0 ? (
        <EmptyState
          title="No consignments found"
          description={
            hasActiveFilters
              ? 'Try adjusting your filters to find consignments'
              : 'Create your first consignment to start managing stock'
          }
          action={
            !hasActiveFilters
              ? {
                  label: 'Create Consignment',
                  onClick: () => navigate(Routes.consignmentCreate),
                }
              : undefined
          }
        />
      ) : (
        <>
          <ConsignmentList consignments={consignments} error={error} />
          {pagination && pagination.totalPages > 1 && (
            <Pagination
              currentPage={pagination.page + 1} // Convert to 1-based for display
              totalPages={pagination.totalPages}
              totalItems={pagination.totalItems}
              itemsPerPage={pagination.size}
              onPageChange={handlePageChange}
            />
          )}
        </>
      )}
    </ListPageLayout>
  );
};
