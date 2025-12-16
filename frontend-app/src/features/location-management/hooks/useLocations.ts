import { useState, useEffect } from 'react';
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
      
      if (response.success && response.data) {
        setLocations(response.data);
      } else {
        throw new Error(response.error?.message || 'Failed to fetch locations');
      }
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to fetch locations');
      logger.error('Error fetching locations:', error);
      setError(error);
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

