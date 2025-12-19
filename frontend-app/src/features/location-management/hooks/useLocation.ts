import { useCallback, useEffect, useState } from 'react';
import { locationService } from '../services/locationService';
import { Location } from '../types/location';
import { logger } from '../../../utils/logger';

export interface UseLocationResult {
  location: Location | null;
  isLoading: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
}

export const useLocation = (locationId: string, tenantId: string): UseLocationResult => {
  const [location, setLocation] = useState<Location | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchLocation = useCallback(async () => {
    if (!locationId || !tenantId) {
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await locationService.getLocation(locationId, tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to fetch location');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      setLocation(response.data);
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to fetch location');
      logger.error('Error fetching location:', error);
      setError(error);
    } finally {
      setIsLoading(false);
    }
  }, [locationId, tenantId]);

  useEffect(() => {
    fetchLocation();
  }, [fetchLocation]);

  return { location, isLoading, error, refetch: fetchLocation };
};
