import { useState } from 'react';
import { locationService } from '../services/locationService';
import { UpdateLocationStatusRequest, LocationStatus } from '../types/location';
import { logger } from '../../../utils/logger';

export interface UseUpdateLocationStatusResult {
  updateStatus: (
    locationId: string,
    status: LocationStatus,
    reason?: string,
    tenantId?: string
  ) => Promise<void>;
  isLoading: boolean;
  error: Error | null;
}

export const useUpdateLocationStatus = (): UseUpdateLocationStatusResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const updateStatus = async (
    locationId: string,
    status: LocationStatus,
    reason?: string,
    tenantId?: string
  ) => {
    if (!tenantId) {
      throw new Error('Tenant ID is required');
    }

    setIsLoading(true);
    setError(null);

    try {
      const request: UpdateLocationStatusRequest = { status, reason };
      const response = await locationService.updateLocationStatus(locationId, request, tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to update location status');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      logger.info('Location status updated successfully', { locationId, status });
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to update location status');
      logger.error('Error updating location status:', error);
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { updateStatus, isLoading, error };
};

