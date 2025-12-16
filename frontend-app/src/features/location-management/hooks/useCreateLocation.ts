import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { locationService } from '../services/locationService';
import { CreateLocationRequest } from '../types/location';
import { logger } from '../../../utils/logger';

export interface UseCreateLocationResult {
  createLocation: (request: CreateLocationRequest, tenantId: string) => Promise<void>;
  isLoading: boolean;
  error: Error | null;
}

export const useCreateLocation = (): UseCreateLocationResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const navigate = useNavigate();

  const createLocation = async (request: CreateLocationRequest, tenantId: string) => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await locationService.createLocation(request, tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to create location');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      logger.info('Location created successfully', { locationId: response.data.locationId });
      navigate(`/locations/${response.data.locationId}`);
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to create location');
      logger.error('Error creating location:', error);
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { createLocation, isLoading, error };
};
