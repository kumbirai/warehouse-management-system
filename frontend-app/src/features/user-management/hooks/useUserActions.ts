import { useState } from 'react';
import { userService } from '../services/userService';

export const useUserActions = (onActionCompleted?: () => void) => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const activateUser = async (userId: string) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await userService.activateUser(userId);
      if (response.error) {
        throw new Error(response.error.message);
      }
      onActionCompleted?.();
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to activate user');
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  const deactivateUser = async (userId: string) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await userService.deactivateUser(userId);
      if (response.error) {
        throw new Error(response.error.message);
      }
      onActionCompleted?.();
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to deactivate user');
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  const suspendUser = async (userId: string) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await userService.suspendUser(userId);
      if (response.error) {
        throw new Error(response.error.message);
      }
      onActionCompleted?.();
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to suspend user');
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return {
    activateUser,
    deactivateUser,
    suspendUser,
    isLoading,
    error,
  };
};
