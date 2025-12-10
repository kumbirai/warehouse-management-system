import { useEffect, useState } from 'react';
import { userService } from '../services/userService';

export const useUserRoles = (userId?: string) => {
  const [roles, setRoles] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetchRoles = async () => {
    if (!userId) {
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      const response = await userService.getUserRoles(userId);
      if (response.error) {
        throw new Error(response.error.message);
      }
      setRoles(response.data ?? []);
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to load user roles');
      setError(error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (userId) {
      fetchRoles();
    }
  }, [userId]);

  const assignRole = async (roleId: string) => {
    if (!userId) {
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      const response = await userService.assignRole(userId, { roleId });
      if (response.error) {
        throw new Error(response.error.message);
      }
      await fetchRoles(); // Refresh roles
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to assign role');
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  const removeRole = async (roleId: string) => {
    if (!userId) {
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      const response = await userService.removeRole(userId, roleId);
      if (response.error) {
        throw new Error(response.error.message);
      }
      await fetchRoles(); // Refresh roles
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to remove role');
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return {
    roles,
    isLoading,
    error,
    assignRole,
    removeRole,
    refetch: fetchRoles,
  };
};
