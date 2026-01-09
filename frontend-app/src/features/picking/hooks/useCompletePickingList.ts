import { useCallback, useState } from 'react';
import { useAuth } from '../../../hooks/useAuth';
import { pickingApiService } from '../services/pickingApiService';
import { CompletePickingListResponse, PickingListStatus } from '../types/pickingTypes';
import { logger } from '../../../utils/logger';

interface UseCompletePickingListResult {
  completePickingList: (pickingListId: string) => Promise<CompletePickingListResponse | null>;
  isLoading: boolean;
  error: Error | null;
}

export const useCompletePickingList = (): UseCompletePickingListResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const { user } = useAuth();

  const completePickingList = useCallback(
    async (pickingListId: string): Promise<CompletePickingListResponse | null> => {
      if (!user?.tenantId) {
        setError(new Error('Tenant ID is required'));
        return null;
      }

      setIsLoading(true);
      setError(null);

      try {
        const response = await pickingApiService.completePickingList(pickingListId, user.tenantId);

        if (response.error) {
          throw new Error(response.error.message || 'Failed to complete picking list');
        }

        if (!response.data) {
          throw new Error('Invalid response from server');
        }

        // Cast string status to enum type
        return {
          ...response.data,
          status: response.data.status as PickingListStatus,
        };
      } catch (err) {
        const error = err instanceof Error ? err : new Error('Failed to complete picking list');
        logger.error('Error completing picking list:', error);
        setError(error);
        return null;
      } finally {
        setIsLoading(false);
      }
    },
    [user?.tenantId]
  );

  return {
    completePickingList,
    isLoading,
    error,
  };
};
