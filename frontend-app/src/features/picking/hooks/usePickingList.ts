import { useCallback, useEffect, useState } from 'react';
import { pickingApiService } from '../services/pickingApiService';
import { PickingListQueryResult } from '../types/pickingTypes';
import { useAuth } from '../../../hooks/useAuth';
import { logger } from '../../../utils/logger';

export interface UsePickingListResult {
  pickingList: PickingListQueryResult | null;
  isLoading: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
}

export const usePickingList = (pickingListId: string): UsePickingListResult => {
  const [pickingList, setPickingList] = useState<PickingListQueryResult | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const { user } = useAuth();

  const fetchPickingList = useCallback(async () => {
    if (!user?.tenantId) {
      setError(new Error('Tenant ID is required'));
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await pickingApiService.getPickingList(pickingListId, user.tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to fetch picking list');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      setPickingList(response.data);
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to fetch picking list');
      logger.error('Error fetching picking list:', error);
      setError(error);
    } finally {
      setIsLoading(false);
    }
  }, [pickingListId, user?.tenantId]);

  useEffect(() => {
    if (pickingListId) {
      fetchPickingList();
    }
  }, [pickingListId, fetchPickingList]);

  return {
    pickingList,
    isLoading,
    error,
    refetch: fetchPickingList,
  };
};
