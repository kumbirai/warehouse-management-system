import { useCallback, useEffect, useRef, useState } from 'react';
import axios from 'axios';
import { PaginationMeta } from '../../../types/api';
import { tenantService } from '../services/tenantService';
import { Tenant, TenantListFilters, TenantStatus } from '../types/tenant';
import { logger } from '../../../utils/logger';

const DEFAULT_FILTERS: TenantListFilters = {
  page: 1,
  size: 10,
};

/**
 * Custom hook for managing tenant list.
 *
 * Production-grade features:
 * - Prevents race conditions with abort controller
 * - Proper error handling with structured logging
 * - Memory leak prevention with cleanup
 */
export const useTenants = (initialFilters: TenantListFilters = DEFAULT_FILTERS) => {
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [filters, setFilters] = useState<TenantListFilters>(initialFilters);
  const [pagination, setPagination] = useState<PaginationMeta | undefined>(undefined);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  const fetchTenants = useCallback(async () => {
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
      const response = await tenantService.listTenants(filters);

      // Debug logging to see what we received
      if (import.meta.env.DEV) {
        logger.debug('useTenants received response:', {
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
          // Log the error for debugging (production-grade logging)
          // Note: We can't check token here without importing tokenStorage,
          // but the error itself indicates auth issues
          logger.warn('Authentication error received in tenant list', {
            code: response.error.code,
            message: response.error.message,
            pathname: window.location.pathname,
          });

          // Don't handle auth errors here - let apiClient interceptor handle token refresh
          // If refresh fails, apiClient will clear tokens and ProtectedRoute will redirect
          // Don't set error state for auth errors - user will be redirected to login
          setError(null);
          return;
        }
        throw new Error(response.error.message);
      }

      // Handle the response data - ensure it's an array
      // The backend returns ApiResponse<List<TenantSummaryResponse>>, so response.data should be an array
      if (!response.data) {
        // If data is null/undefined, set empty array
        setTenants([]);
        setPagination(response.meta?.pagination);
        return;
      }

      // Ensure we have an array (defensive programming)
      // response.data should be an array of TenantSummaryResponse objects
      const tenantsArray = Array.isArray(response.data) ? response.data : [];

      // Check if request was aborted before updating state
      if (signal.aborted) {
        return;
      }

      setTenants(tenantsArray);
      setPagination(response.meta?.pagination);
    } catch (err) {
      // Don't update state if request was aborted
      if (signal.aborted) {
        return;
      }
      // Don't display 401/403 errors - apiClient interceptor handles token refresh
      // If refresh fails, ProtectedRoute will redirect to login via React Router
      const isHttpAuthError =
        axios.isAxiosError(err) &&
        (err.response?.status === 401 ||
          err.response?.status === 403 ||
          err.response?.data?.error?.code === 'UNAUTHORIZED' ||
          err.response?.data?.error?.code === 'ACCESS_DENIED' ||
          err.response?.data?.error?.message?.toLowerCase().includes('authentication required') ||
          err.response?.data?.error?.message?.toLowerCase().includes('access denied'));
      if (isHttpAuthError) {
        // Auth error - apiClient interceptor will handle token refresh
        // If refresh fails, tokens will be cleared and ProtectedRoute will redirect
        // Don't display error or set state - let the auth flow handle it
        logger.debug('Authentication error in tenant list - handled by apiClient interceptor');
        setError(null);
      } else {
        // For other errors, display the error message
        const message = err instanceof Error ? err.message : 'Failed to load tenants';
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
    fetchTenants();

    // Cleanup: abort pending requests on unmount or filter change
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, [fetchTenants]);

  const updatePage = (page: number) => {
    setFilters(prev => ({
      ...prev,
      page,
    }));
  };

  const updateStatus = (status?: TenantStatus) => {
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

  return {
    tenants,
    filters,
    pagination,
    isLoading,
    error,
    refetch: fetchTenants,
    updatePage,
    updateStatus,
    updateSearch,
    setPageSize: (size: number) =>
      setFilters(prev => ({
        ...prev,
        size,
        page: 1,
      })),
  };
};
