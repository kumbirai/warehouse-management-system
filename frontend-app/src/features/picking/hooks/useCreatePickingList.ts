import { useState } from 'react';
import { pickingApiService } from '../services/pickingApiService';
import { CreatePickingListRequest, CreatePickingListResponse } from '../types/pickingTypes';
import { useAuth } from '../../../hooks/useAuth';
import { logger } from '../../../utils/logger';

export interface UseCreatePickingListResult {
  createPickingList: (request: CreatePickingListRequest) => Promise<CreatePickingListResponse>;
  isLoading: boolean;
  error: Error | null;
}

export const useCreatePickingList = (): UseCreatePickingListResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const { user } = useAuth();

  const createPickingList = async (
    request: CreatePickingListRequest
  ): Promise<CreatePickingListResponse> => {
    setIsLoading(true);
    setError(null);

    try {
      if (!user?.tenantId) {
        throw new Error('Tenant ID is required');
      }

      const response = await pickingApiService.createPickingList(request, user.tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to create picking list');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      logger.info('Picking list created successfully', {
        pickingListId: response.data.pickingListId,
        status: response.data.status,
      });

      return response.data;
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to create picking list');
      logger.error('Error creating picking list:', error);
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { createPickingList, isLoading, error };
};
