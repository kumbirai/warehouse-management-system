import { useState } from 'react';
import { Box, FormControl, InputLabel, MenuItem, Select } from '@mui/material';
import { useNotifications } from '../hooks/useNotifications';
import { NotificationList } from '../components/NotificationList';
import { ListPageLayout } from '../../../components/layouts';
import { FilterBar, Pagination } from '../../../components/common';
import { getBreadcrumbs } from '../../../utils/navigationUtils';
import { NotificationStatus, NotificationType } from '../types/notification';

export const NotificationListPage = () => {
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [statusFilter, setStatusFilter] = useState<NotificationStatus | 'ALL'>('ALL');
  const [typeFilter, setTypeFilter] = useState<NotificationType | 'ALL'>('ALL');

  const filters = {
    status: statusFilter !== 'ALL' ? statusFilter : undefined,
    type: typeFilter !== 'ALL' ? typeFilter : undefined,
    page,
    size,
  };

  const { data: notificationsResponse, isLoading, error } = useNotifications(filters);

  const notifications = notificationsResponse?.data || [];
  const totalElements = notificationsResponse?.totalElements || 0;
  const totalPages = notificationsResponse?.totalPages || 0;

  const hasActiveFilters = statusFilter !== 'ALL' || typeFilter !== 'ALL';

  const handleClearFilters = () => {
    setStatusFilter('ALL');
    setTypeFilter('ALL');
    setPage(0);
  };

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
  };

  return (
    <ListPageLayout
      breadcrumbs={getBreadcrumbs.notificationList()}
      title="Notifications"
      description="View and manage your notifications"
      isLoading={isLoading}
      error={error?.message || null}
    >
      <FilterBar onClearFilters={handleClearFilters} hasActiveFilters={hasActiveFilters}>
        <FormControl size="small" sx={{ minWidth: 150 }}>
          <InputLabel id="status-filter-label">Status</InputLabel>
          <Select
            labelId="status-filter-label"
            value={statusFilter}
            label="Status"
            onChange={e => {
              setStatusFilter(e.target.value as NotificationStatus | 'ALL');
              setPage(0);
            }}
            aria-label="Filter notifications by status"
          >
            <MenuItem value="ALL">All Status</MenuItem>
            <MenuItem value="PENDING">Pending</MenuItem>
            <MenuItem value="SENT">Sent</MenuItem>
            <MenuItem value="DELIVERED">Delivered</MenuItem>
            <MenuItem value="READ">Read</MenuItem>
            <MenuItem value="FAILED">Failed</MenuItem>
          </Select>
        </FormControl>

        <FormControl size="small" sx={{ minWidth: 200 }}>
          <InputLabel id="type-filter-label">Type</InputLabel>
          <Select
            labelId="type-filter-label"
            value={typeFilter}
            label="Type"
            onChange={e => {
              setTypeFilter(e.target.value as NotificationType | 'ALL');
              setPage(0);
            }}
            aria-label="Filter notifications by type"
          >
            <MenuItem value="ALL">All Types</MenuItem>
            <MenuItem value="USER_CREATED">User Created</MenuItem>
            <MenuItem value="USER_UPDATED">User Updated</MenuItem>
            <MenuItem value="TENANT_CREATED">Tenant Created</MenuItem>
            <MenuItem value="TENANT_ACTIVATED">Tenant Activated</MenuItem>
            <MenuItem value="WELCOME">Welcome</MenuItem>
            <MenuItem value="SYSTEM_ALERT">System Alert</MenuItem>
          </Select>
        </FormControl>
      </FilterBar>

      <NotificationList
        notifications={notifications}
        emptyMessage={
          hasActiveFilters
            ? 'No notifications match the selected filters'
            : 'You have no notifications'
        }
      />

      {totalPages > 1 && (
        <Box sx={{ mt: 3 }}>
          <Pagination
            currentPage={page + 1}
            totalPages={totalPages}
            totalItems={totalElements}
            itemsPerPage={size}
            onPageChange={newPage => handlePageChange(newPage - 1)}
          />
        </Box>
      )}
    </ListPageLayout>
  );
};
