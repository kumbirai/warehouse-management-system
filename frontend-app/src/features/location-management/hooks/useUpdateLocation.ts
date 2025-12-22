import { useState } from 'react';
import { locationService } from '../services/locationService';
import { UpdateLocationRequest } from '../types/location';
import { logger } from '../../../utils/logger';

export interface UseUpdateLocationResult {
  updateLocation: (
    locationId: string,
    request: UpdateLocationRequest,
    tenantId: string
  ) => Promise<void>;
  isLoading: boolean;
  error: Error | null;
}

export const useUpdateLocation = (): UseUpdateLocationResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const updateLocation = async (
    locationId: string,
    request: UpdateLocationRequest,
    tenantId: string
  ) => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await locationService.updateLocation(locationId, request, tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to update location');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      logger.info('Location updated successfully', { locationId });
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to update location');
      logger.error('Error updating location:', error);
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { updateLocation, isLoading, error };
};
