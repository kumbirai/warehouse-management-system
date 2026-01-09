import { FormControl, InputLabel, MenuItem, Select, SelectChangeEvent } from '@mui/material';
import { useEffect, useMemo } from 'react';
import { useLocations } from '../../location-management/hooks/useLocations';
import { useAppSelector } from '../../../store/hooks';
import { selectUser } from '../../../store/authSlice';
import { logger } from '../../../utils/logger';

interface WarehouseSelectorProps {
  value?: string;
  onChange: (warehouseId?: string) => void;
  label?: string;
  required?: boolean;
  error?: boolean;
  helperText?: string;
}

export const WarehouseSelector = ({
  value,
  onChange,
  label = 'Warehouse ID',
  required = false,
  error = false,
  helperText,
}: WarehouseSelectorProps) => {
  const user = useAppSelector(selectUser);
  const tenantId = user?.tenantId;

  // Memoize filters to prevent infinite re-renders
  const filters = useMemo(
    () => ({
      tenantId: tenantId || undefined,
      page: 1,
      size: 1000, // Fetch a large number to get all locations
      // Don't filter by status - we want all warehouses regardless of status
    }),
    [tenantId]
  );

  // Fetch all locations - warehouses are locations with type='WAREHOUSE'
  const { locations, isLoading, error: fetchError } = useLocations(filters);

  // Debug logging
  useEffect(() => {
    if (import.meta.env.DEV) {
      logger.debug('WarehouseSelector state:', {
        tenantId,
        locationsCount: locations.length,
        isLoading,
        error: fetchError?.message,
        locations: locations.map(loc => ({
          id: loc.locationId,
          type: loc.type,
          description: loc.description,
          name: loc.name,
          code: loc.code,
        })),
      });
    }
  }, [tenantId, locations, isLoading, fetchError]);

  // Filter to only warehouse locations (type='WAREHOUSE') or use all locations if no warehouses found
  const warehouseOptions = useMemo(() => {
    // First, try to get only warehouses (type='WAREHOUSE')
    const warehouses = locations.filter(
      location => location.type?.toUpperCase() === 'WAREHOUSE' && location.locationId
    );

    if (import.meta.env.DEV) {
      logger.debug('WarehouseSelector filtering:', {
        totalLocations: locations.length,
        warehousesFound: warehouses.length,
        warehouseTypes: warehouses.map(w => w.type),
      });
    }

    // If we have warehouses, use them; otherwise use all locations as fallback
    const locationsToUse = warehouses.length > 0 ? warehouses : locations;

    // Extract unique location IDs
    const uniqueIds = new Set<string>();
    locationsToUse.forEach(location => {
      if (location.locationId) {
        uniqueIds.add(location.locationId);
      }
    });
    return Array.from(uniqueIds).sort();
  }, [locations]);

  const handleChange = (event: SelectChangeEvent<string>) => {
    const selectedValue = event.target.value;
    onChange(selectedValue || undefined);
  };

  return (
    <FormControl fullWidth required={required} error={error}>
      <InputLabel id="warehouse-select-label">{label}</InputLabel>
      <Select
        labelId="warehouse-select-label"
        id="warehouse-select"
        value={value || ''}
        label={label}
        onChange={handleChange}
        disabled={isLoading || !tenantId}
      >
        {!tenantId ? (
          <MenuItem value="" disabled>
            Tenant ID required
          </MenuItem>
        ) : fetchError ? (
          <MenuItem value="" disabled>
            Error loading warehouses: {fetchError.message}
          </MenuItem>
        ) : warehouseOptions.length === 0 && !isLoading ? (
          <MenuItem value="" disabled>
            No warehouses available
          </MenuItem>
        ) : isLoading ? (
          <MenuItem value="" disabled>
            Loading warehouses...
          </MenuItem>
        ) : (
          warehouseOptions.map(warehouseId => {
            // Find location details for display
            const location = locations.find(loc => loc.locationId === warehouseId);
            // Prefer description, then name, then code, then barcode, finally locationId
            const displayText = location
              ? location.description ||
                location.name ||
                location.code ||
                location.barcode ||
                warehouseId
              : warehouseId;
            return (
              <MenuItem key={warehouseId} value={warehouseId}>
                {displayText}
              </MenuItem>
            );
          })
        )}
      </Select>
      {helperText && (
        <span
          style={{
            fontSize: '0.75rem',
            marginTop: '3px',
            marginLeft: '14px',
            color: error ? '#d32f2f' : 'rgba(0, 0, 0, 0.6)',
          }}
        >
          {helperText}
        </span>
      )}
    </FormControl>
  );
};
