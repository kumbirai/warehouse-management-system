import { useState } from 'react';
import { locationStatusService } from '../services/locationStatusService';
import { logger } from '../../../utils/logger';

export interface UseBlockLocationResult {
  blockLocation: (locationId: string, reason: string, tenantId: string) => Promise<void>;
  isLoading: boolean;
  error: Error | null;
}

export const useBlockLocation = (): UseBlockLocationResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const blockLocation = async (locationId: string, reason: string, tenantId: string) => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await locationStatusService.blockLocation(locationId, reason, tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to block location');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      logger.info('Location blocked successfully', { locationId });
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to block location');
      logger.error('Error blocking location:', error);
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { blockLocation, isLoading, error };
};

