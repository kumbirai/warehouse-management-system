import { useCallback, useEffect, useRef, useState } from 'react';
import { useAuth } from '../../../hooks/useAuth';
import { pickingApiService } from '../services/pickingApiService';
import { ListPickingTasksQueryResult, PickingTaskStatus } from '../types/pickingTypes';
import { logger } from '../../../utils/logger';

interface UsePickingTasksResult {
  pickingTasks: ListPickingTasksQueryResult | null;
  isLoading: boolean;
  error: Error | null;
  refetch: () => void;
}

interface ListPickingTasksFilters {
  status?: PickingTaskStatus;
  page?: number;
  size?: number;
}

export const usePickingTasks = (filters: ListPickingTasksFilters = {}): UsePickingTasksResult => {
  const [pickingTasks, setPickingTasks] = useState<ListPickingTasksQueryResult | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const { user } = useAuth();
  const isRequestInProgress = useRef(false);
  const filtersRef = useRef<ListPickingTasksFilters>(filters);

  filtersRef.current = filters;

  const fetchPickingTasks = useCallback(async () => {
    if (isRequestInProgress.current) {
      return;
    }

    if (!user?.tenantId) {
      setError(new Error('Tenant ID is required'));
      setIsLoading(false);
      return;
    }

    isRequestInProgress.current = true;
    setIsLoading(true);
    setError(null);

    try {
      const response = await pickingApiService.listPickingTasks(filtersRef.current, user.tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to fetch picking tasks');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      // Cast string status values to enum types
      setPickingTasks({
        ...response.data,
        pickingTasks: response.data.pickingTasks.map(task => ({
          ...task,
          status: task.status as PickingTaskStatus,
        })),
      });
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to fetch picking tasks');
      logger.error('Error fetching picking tasks:', error);
      setError(error);
    } finally {
      setIsLoading(false);
      isRequestInProgress.current = false;
    }
  }, [user?.tenantId]);

  useEffect(() => {
    if (user?.tenantId) {
      fetchPickingTasks();
    }
  }, [user?.tenantId, fetchPickingTasks]);

  return {
    pickingTasks,
    isLoading,
    error,
    refetch: fetchPickingTasks,
  };
};
