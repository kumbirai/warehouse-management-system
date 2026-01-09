import { useCallback, useEffect, useState } from 'react';
import { pickingApiService } from '../services/pickingApiService';
import { LoadDetailQueryResult } from '../types/pickingTypes';
import { useAuth } from '../../../hooks/useAuth';
import { logger } from '../../../utils/logger';

export interface UseLoadResult {
  load: LoadDetailQueryResult | null;
  isLoading: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
}

export const useLoad = (loadId: string | null): UseLoadResult => {
  const [load, setLoad] = useState<LoadDetailQueryResult | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const { user } = useAuth();

  const fetchLoad = useCallback(async () => {
    if (!loadId || !user?.tenantId) {
      setError(new Error('Load ID and Tenant ID are required'));
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await pickingApiService.getLoad(loadId, user.tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to fetch load');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      setLoad(response.data);
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to fetch load');
      logger.error('Error fetching load:', error);
      setError(error);
    } finally {
      setIsLoading(false);
    }
  }, [loadId, user?.tenantId]);

  useEffect(() => {
    if (loadId) {
      fetchLoad();
    }
  }, [loadId, fetchLoad]);

  return {
    load,
    isLoading,
    error,
    refetch: fetchLoad,
  };
};
