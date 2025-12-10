import { useCallback, useEffect, useRef, useState } from 'react';
import axios from 'axios';
import { PaginationMeta } from '../../../types/api';
import { userService } from '../services/userService';
import { User, UserListFilters, UserStatus } from '../types/user';
import { logger } from '../../../utils/logger';

const DEFAULT_FILTERS: UserListFilters = {
  page: 1,
  size: 10,
};

/**
 * Custom hook for managing user list.
 *
 * Production-grade features:
 * - Prevents race conditions with abort controller
 * - Proper error handling with structured logging
 * - Memory leak prevention with cleanup
 */
export const useUsers = (initialFilters: UserListFilters = DEFAULT_FILTERS) => {
  const [users, setUsers] = useState<User[]>([]);
  const [filters, setFilters] = useState<UserListFilters>(initialFilters);
  const [pagination, setPagination] = useState<PaginationMeta | undefined>(undefined);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  const fetchUsers = useCallback(async () => {
    // Cancel any pending requests
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }

    // Create new abort controller for this request
    abortControllerRef.current = new AbortController();
    const signal = abortControllerRef.current.signal;

    setIsLoading(true);
    setError(null);
    try {
      const response = await userService.listUsers(filters);

      // Debug logging to see what we're receiving
      if (import.meta.env.DEV) {
        logger.debug('useUsers received response:', {
          hasData: !!response.data,
          dataType: typeof response.data,
          isArray: Array.isArray(response.data),
          dataLength: Array.isArray(response.data) ? response.data.length : 'N/A',
          hasError: !!response.error,
          hasMeta: !!response.meta,
          fullResponse: JSON.stringify(response, null, 2),
        });
      }

      // Check if request was aborted
      if (signal.aborted) {
        return;
      }

      if (response.error) {
        // Check if it's an authentication/authorization error
        const isAuthError =
          response.error.code === 'UNAUTHORIZED' ||
          response.error.code === 'ACCESS_DENIED' ||
          response.error.message?.toLowerCase().includes('authentication required') ||
          response.error.message?.toLowerCase().includes('access denied');

        if (isAuthError) {
          logger.warn('Authentication error received in user list', {
            code: response.error.code,
            message: response.error.message,
            pathname: window.location.pathname,
          });

          setError(null);
          return;
        }
        throw new Error(response.error.message);
      }

      // Handle the response data - ensure it's an array
      if (!response.data) {
        setUsers([]);
        setPagination(response.meta?.pagination);
        return;
      }

      // Ensure we have an array (defensive programming)
      const usersArray = Array.isArray(response.data) ? response.data : [];

      // Check if request was aborted before updating state
      if (signal.aborted) {
        return;
      }

      setUsers(usersArray);
      setPagination(response.meta?.pagination);
    } catch (err) {
      // Don't update state if request was aborted
      if (signal.aborted) {
        return;
      }
      // Don't display 401/403 errors - apiClient interceptor handles token refresh
      const isHttpAuthError =
        axios.isAxiosError(err) &&
        (err.response?.status === 401 ||
          err.response?.status === 403 ||
          err.response?.data?.error?.code === 'UNAUTHORIZED' ||
          err.response?.data?.error?.code === 'ACCESS_DENIED' ||
          err.response?.data?.error?.message?.toLowerCase().includes('authentication required') ||
          err.response?.data?.error?.message?.toLowerCase().includes('access denied'));
      if (isHttpAuthError) {
        logger.debug('Authentication error in user list - handled by apiClient interceptor');
        setError(null);
      } else {
        const message = err instanceof Error ? err.message : 'Failed to load users';
        setError(message);
      }
    } finally {
      // Only update loading state if request wasn't aborted
      if (!signal.aborted) {
        setIsLoading(false);
      }
    }
  }, [filters]);

  useEffect(() => {
    fetchUsers();

    // Cleanup: abort pending requests on unmount or filter change
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, [fetchUsers]);

  const updatePage = (page: number) => {
    setFilters(prev => ({
      ...prev,
      page,
    }));
  };

  const updateStatus = (status?: UserStatus) => {
    setFilters(prev => ({
      ...prev,
      page: 1,
      status,
    }));
  };

  const updateSearch = (search?: string) => {
    setFilters(prev => ({
      ...prev,
      page: 1,
      search,
    }));
  };

  const updateTenantId = (tenantId?: string) => {
    setFilters(prev => ({
      ...prev,
      page: 1,
      tenantId,
    }));
  };

  return {
    users,
    filters,
    pagination,
    isLoading,
    error,
    refetch: fetchUsers,
    updatePage,
    updateStatus,
    updateSearch,
    updateTenantId,
    setPageSize: (size: number) =>
      setFilters(prev => ({
        ...prev,
        size,
        page: 1,
      })),
  };
};
