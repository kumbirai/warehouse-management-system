import { useEffect, useState } from 'react';
import { locationService } from '../services/locationService';
import { Location, LocationListFilters } from '../types/location';
import { logger } from '../../../utils/logger';

export interface UseLocationsResult {
  locations: Location[];
  isLoading: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
}

export const useLocations = (filters: LocationListFilters): UseLocationsResult => {
  const [locations, setLocations] = useState<Location[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchLocations = async () => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await locationService.listLocations(filters);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to fetch locations');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      // Extract locations array from the nested response structure
      // Backend returns ApiResponse<LocationListQueryResult> where LocationListQueryResult has a 'locations' property
      const locationsArray = Array.isArray(response.data.locations) ? response.data.locations : [];

      setLocations(locationsArray);
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to fetch locations');
      logger.error('Error fetching locations:', error);
      setError(error);
      setLocations([]); // Reset locations on error
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (filters.tenantId) {
      fetchLocations();
    }
  }, [filters.tenantId, filters.page, filters.size, filters.zone, filters.status, filters.search]);

  return { locations, isLoading, error, refetch: fetchLocations };
};
