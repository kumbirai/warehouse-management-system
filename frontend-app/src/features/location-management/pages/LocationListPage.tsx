import { Button, TextField, MenuItem, FormControl, InputLabel, Select, Grid } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useState, useCallback, useMemo } from 'react';
import { Add as AddIcon, Search as SearchIcon } from '@mui/icons-material';

import { ListPageLayout } from '../../../components/layouts';
import { FilterBar, EmptyState } from '../../../components/common';
import { Routes, getBreadcrumbs } from '../../../utils/navigationUtils';
import { LocationList } from '../components/LocationList';
import { useLocations } from '../hooks/useLocations';
import { useAuth } from '../../../hooks/useAuth';
import { LocationStatus, LocationListFilters } from '../types/location';

export const LocationListPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  // Filter state
  const [page, setPage] = useState(0);
  const [size] = useState(100);
  const [statusFilter, setStatusFilter] = useState<LocationStatus | ''>('');
  const [zoneFilter, setZoneFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // Memoize filters object to prevent infinite loop in useLocations hook
  const filters: LocationListFilters = useMemo(
    () => ({
      tenantId: user?.tenantId ?? undefined,
      page,
      size,
      status: statusFilter || undefined,
      zone: zoneFilter || undefined,
      search: searchQuery || undefined,
    }),
    [user?.tenantId, page, size, statusFilter, zoneFilter, searchQuery]
  );

  const { locations, isLoading, error } = useLocations(filters);

  // Filter handlers
  const handleStatusChange = useCallback((newStatus: LocationStatus | '') => {
    setStatusFilter(newStatus);
    setPage(0);
  }, []);

  const handleZoneChange = useCallback((newZone: string) => {
    setZoneFilter(newZone);
    setPage(0);
  }, []);

  const handleSearchChange = useCallback((query: string) => {
    setSearchQuery(query);
    setPage(0);
  }, []);

  const handleClearFilters = useCallback(() => {
    setStatusFilter('');
    setZoneFilter('');
    setSearchQuery('');
    setPage(0);
  }, []);

  const hasActiveFilters = Boolean(statusFilter || zoneFilter || searchQuery);

  return (
    <ListPageLayout
      breadcrumbs={getBreadcrumbs.locationList()}
      title="Locations"
      description="Manage warehouse locations and storage areas"
      actions={
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => navigate(Routes.locationCreate)}
        >
          Create Location
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
              label="Search"
              placeholder="Search by code, barcode..."
              value={searchQuery}
              onChange={(e) => handleSearchChange(e.target.value)}
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
                onChange={(e) => handleStatusChange(e.target.value as LocationStatus | '')}
              >
                <MenuItem value="">All</MenuItem>
                <MenuItem value="AVAILABLE">Available</MenuItem>
                <MenuItem value="OCCUPIED">Occupied</MenuItem>
                <MenuItem value="RESERVED">Reserved</MenuItem>
                <MenuItem value="BLOCKED">Blocked</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={3}>
            <TextField
              fullWidth
              label="Zone"
              placeholder="Filter by zone"
              value={zoneFilter}
              onChange={(e) => handleZoneChange(e.target.value)}
            />
          </Grid>
        </Grid>
      </FilterBar>

      {!isLoading && locations.length === 0 ? (
        <EmptyState
          title="No locations found"
          description={
            hasActiveFilters
              ? 'Try adjusting your filters to find locations'
              : 'Create your first location to start managing warehouse storage'
          }
          action={
            !hasActiveFilters
              ? {
                  label: 'Create Location',
                  onClick: () => navigate(Routes.locationCreate),
                }
              : undefined
          }
        />
      ) : (
        <LocationList locations={locations} isLoading={isLoading} error={error} />
      )}
    </ListPageLayout>
  );
};
