import { useState } from 'react';
import { locationStatusService } from '../services/locationStatusService';
import { logger } from '../../../utils/logger';

export interface UseUnblockLocationResult {
  unblockLocation: (locationId: string, tenantId: string) => Promise<void>;
  isLoading: boolean;
  error: Error | null;
}

export const useUnblockLocation = (): UseUnblockLocationResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const unblockLocation = async (locationId: string, tenantId: string) => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await locationStatusService.unblockLocation(locationId, tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to unblock location');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      logger.info('Location unblocked successfully', { locationId });
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to unblock location');
      logger.error('Error unblocking location:', error);
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { unblockLocation, isLoading, error };
};
