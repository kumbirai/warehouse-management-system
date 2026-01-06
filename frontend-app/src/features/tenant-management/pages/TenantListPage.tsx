import { Box, Button, MenuItem, Stack, TextField } from '@mui/material';
import { ChangeEvent, useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useTenants } from '../hooks/useTenants';
import { Tenant } from '../types/tenant';
import { TenantList } from '../components/TenantList';
import { ListPageLayout } from '../../../components/layouts';
import { FilterBar, Pagination } from '../../../components/common';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';

const statusOptions: (Tenant['status'] | 'ALL')[] = [
  'ALL',
  'PENDING',
  'ACTIVE',
  'INACTIVE',
  'SUSPENDED',
];

export const TenantListPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { tenants, pagination, isLoading, error, updatePage, updateSearch, updateStatus, refetch } =
    useTenants({ page: 1, size: 10 });
  const [searchValue, setSearchValue] = useState('');
  const [statusValue, setStatusValue] = useState<'ALL' | Tenant['status']>('ALL');

  // Refresh the list when navigating to this page (e.g., from create/detail pages)
  useEffect(() => {
    // Only refetch if we're actually on the tenants list page
    if (location.pathname === '/admin/tenants') {
      refetch();
    }
  }, [location.pathname, refetch]);

  // Also refresh when the page becomes visible (user switches back to the tab)
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible' && location.pathname === '/admin/tenants') {
        refetch();
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [location.pathname, refetch]);

  const handleSearchChange = (event: ChangeEvent<HTMLInputElement>) => {
    setSearchValue(event.target.value);
    updateSearch(event.target.value || undefined);
  };

  const handleStatusChange = (event: ChangeEvent<HTMLInputElement>) => {
    const next = event.target.value as 'ALL' | Tenant['status'];
    setStatusValue(next);
    updateStatus(next === 'ALL' ? undefined : next);
  };

  const hasActiveFilters = Boolean(searchValue || (statusValue && statusValue !== 'ALL'));

  const handleClearFilters = () => {
    setSearchValue('');
    setStatusValue('ALL');
    updateSearch(undefined);
    updateStatus(undefined);
  };

  return (
    <ListPageLayout
      breadcrumbs={getBreadcrumbs.tenantList()}
      title="Tenant Management"
      description="Create, activate, and manage Local Distribution Partner tenants."
      actions={
        <Button variant="contained" onClick={() => navigate(Routes.admin.tenantCreate)}>
          Create Tenant
        </Button>
      }
      isLoading={isLoading}
      error={error}
      maxWidth="lg"
    >
      <FilterBar onClearFilters={handleClearFilters} hasActiveFilters={hasActiveFilters}>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ flex: 1 }}>
          <TextField
            label="Search"
            placeholder="Search by ID or name"
            value={searchValue}
            onChange={handleSearchChange}
            fullWidth
          />
          <TextField
            select
            label="Status"
            value={statusValue}
            onChange={handleStatusChange}
            sx={{ minWidth: 180 }}
          >
            {statusOptions.map(status => (
              <MenuItem key={status} value={status}>
                {status}
              </MenuItem>
            ))}
          </TextField>
        </Stack>
      </FilterBar>

      <TenantList
        tenants={tenants}
        isLoading={isLoading}
        onOpenTenant={id => navigate(Routes.admin.tenantDetail(id))}
        onActionCompleted={refetch}
      />

      {pagination && pagination.totalPages > 1 && (
        <Box sx={{ mt: 3 }}>
          <Pagination
            currentPage={pagination.page}
            totalPages={pagination.totalPages}
            totalItems={pagination.totalElements || 0}
            itemsPerPage={pagination.size || 10}
            onPageChange={(page) => updatePage(page)}
          />
        </Box>
      )}
    </ListPageLayout>
  );
};
