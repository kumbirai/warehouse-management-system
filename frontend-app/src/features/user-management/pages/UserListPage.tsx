import { Box, Button, MenuItem, Pagination, Stack, TextField } from '@mui/material';
import { ChangeEvent, useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useUsers } from '../hooks/useUsers';
import { UserStatus } from '../types/user';
import { UserList } from '../components/UserList';
import { TenantSelector } from '../components/TenantSelector';
import { useAuth } from '../../../hooks/useAuth';
import { ListPageLayout } from '../../../components/layouts';
import { FilterBar } from '../../../components/common';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';

const statusOptions: (UserStatus | 'ALL')[] = ['ALL', 'ACTIVE', 'INACTIVE', 'SUSPENDED'];

export const UserListPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { isSystemAdmin, user: currentUser } = useAuth();
  const currentTenantId = currentUser?.tenantId || undefined;
  const {
    users,
    pagination,
    isLoading,
    error,
    updatePage,
    updateSearch,
    updateStatus,
    updateTenantId,
    refetch,
  } = useUsers({ page: 1, size: 10, tenantId: isSystemAdmin() ? undefined : currentTenantId });
  const [searchValue, setSearchValue] = useState('');
  const [statusValue, setStatusValue] = useState<'ALL' | UserStatus>('ALL');
  const [tenantValue, setTenantValue] = useState<string | undefined>(
    isSystemAdmin() ? undefined : currentTenantId
  );

  // Refresh the list when navigating to this page
  useEffect(() => {
    if (location.pathname === '/admin/users') {
      refetch();
    }
  }, [location.pathname, refetch]);

  // Also refresh when the page becomes visible
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible' && location.pathname === '/admin/users') {
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
    const next = event.target.value as 'ALL' | UserStatus;
    setStatusValue(next);
    updateStatus(next === 'ALL' ? undefined : next);
  };

  const handleTenantChange = (tenantId?: string) => {
    const normalizedTenantId = tenantId || undefined;
    setTenantValue(normalizedTenantId);
    updateTenantId(normalizedTenantId);
  };

  const hasActiveFilters = Boolean(
    searchValue || (statusValue && statusValue !== 'ALL') || (isSystemAdmin() && tenantValue)
  );

  const handleClearFilters = () => {
    setSearchValue('');
    setStatusValue('ALL');
    const defaultTenantId = isSystemAdmin() ? undefined : currentTenantId;
    setTenantValue(defaultTenantId);
    updateSearch(undefined);
    updateStatus(undefined);
    updateTenantId(defaultTenantId);
  };

  return (
    <ListPageLayout
      breadcrumbs={getBreadcrumbs.userList()}
      title="User Management"
      description="Manage user accounts, roles, and permissions across tenants."
      actions={
        <Button variant="contained" onClick={() => navigate(Routes.admin.userCreate)}>
          Create User
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
            placeholder="Search by username or email"
            value={searchValue}
            onChange={handleSearchChange}
            fullWidth
          />
          {isSystemAdmin() && (
            <TenantSelector
              value={tenantValue}
              onChange={handleTenantChange}
              label="Filter by Tenant"
            />
          )}
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

      <UserList
        users={users}
        isLoading={isLoading}
        onOpenUser={id => navigate(Routes.admin.userDetail(id))}
        onActionCompleted={refetch}
      />

      {pagination && pagination.totalPages > 1 && (
        <Box display="flex" justifyContent="center" mt={3}>
          <Pagination
            count={pagination.totalPages}
            page={pagination.page}
            onChange={(_, page) => updatePage(page)}
            color="primary"
          />
        </Box>
      )}
    </ListPageLayout>
  );
};
