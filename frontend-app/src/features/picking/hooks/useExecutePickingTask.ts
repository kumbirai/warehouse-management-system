import { useCallback, useState } from 'react';
import { useAuth } from '../../../hooks/useAuth';
import { pickingApiService } from '../services/pickingApiService';
import {
  ExecutePickingTaskRequest,
  ExecutePickingTaskResponse,
  PickingTaskStatus,
} from '../types/pickingTypes';
import { logger } from '../../../utils/logger';

interface UseExecutePickingTaskResult {
  executeTask: (
    taskId: string,
    request: ExecutePickingTaskRequest
  ) => Promise<ExecutePickingTaskResponse | null>;
  isLoading: boolean;
  error: Error | null;
}

export const useExecutePickingTask = (): UseExecutePickingTaskResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const { user } = useAuth();

  const executeTask = useCallback(
    async (
      taskId: string,
      request: ExecutePickingTaskRequest
    ): Promise<ExecutePickingTaskResponse | null> => {
      if (!user?.tenantId) {
        setError(new Error('Tenant ID is required'));
        return null;
      }

      setIsLoading(true);
      setError(null);

      try {
        const response = await pickingApiService.executePickingTask(taskId, request, user.tenantId);

        if (response.error) {
          throw new Error(response.error.message || 'Failed to execute picking task');
        }

        if (!response.data) {
          throw new Error('Invalid response from server');
        }

        // Cast string status to enum type
        return {
          ...response.data,
          status: response.data.status as PickingTaskStatus,
        };
      } catch (err) {
        const error = err instanceof Error ? err : new Error('Failed to execute picking task');
        logger.error('Error executing picking task:', error);
        setError(error);
        return null;
      } finally {
        setIsLoading(false);
      }
    },
    [user?.tenantId]
  );

  return {
    executeTask,
    isLoading,
    error,
  };
};
