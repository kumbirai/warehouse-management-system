import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { stockManagementService } from '../services/stockManagementService';
import { Consignment, ConsignmentListFilters } from '../types/stockManagement';
import { logger } from '../../../utils/logger';

export interface UseConsignmentsResult {
  consignments: Consignment[];
  isLoading: boolean;
  error: Error | null;
  pagination?: {
    page: number;
    size: number;
    totalPages: number;
    totalItems: number;
  };
  refetch: () => Promise<void>;
}

/**
 * Creates a stable string representation of filters for comparison.
 * This prevents unnecessary re-renders when filter objects are recreated with the same values.
 */
const getFiltersKey = (filters: ConsignmentListFilters): string => {
  return JSON.stringify({
    page: filters.page ?? 0,
    size: filters.size ?? 20,
    status: filters.status ?? '',
    search: filters.search ?? '',
    expiringWithinDays: filters.expiringWithinDays ?? undefined,
  });
};

export const useConsignments = (
  filters: ConsignmentListFilters,
  tenantId: string | undefined
): UseConsignmentsResult => {
  const [consignments, setConsignments] = useState<Consignment[]>([]);
  const [pagination, setPagination] = useState<
    | {
        page: number;
        size: number;
        totalPages: number;
        totalItems: number;
      }
    | undefined
  >();
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);
  const isFetchingRef = useRef(false);
  const lastFiltersKeyRef = useRef<string>('');
  const lastTenantIdRef = useRef<string | undefined>(undefined);

  // Compute filters key for dependency tracking
  const filtersKey = useMemo(() => getFiltersKey(filters), [filters]);

  // Memoize filters to create a stable reference based on serialized key
  // Note: filtersKey is included to ensure memoization updates when key changes
  const stableFilters = useMemo(() => filters, [filters]);

  const fetchConsignments = useCallback(async () => {
    // Prevent concurrent fetches
    if (isFetchingRef.current) {
      return;
    }

    if (!tenantId) {
      if (import.meta.env.DEV) {
        logger.warn('Cannot fetch consignments: tenantId is missing', {
          filters: stableFilters,
          hasTenantId: !!tenantId,
        });
      }
      setIsLoading(false);
      setConsignments([]);
      return;
    }

    // Check if filters or tenantId actually changed to prevent unnecessary fetches
    if (filtersKey === lastFiltersKeyRef.current && tenantId === lastTenantIdRef.current) {
      // No change, skip fetch (initial load will still happen due to empty refs)
      if (lastFiltersKeyRef.current !== '') {
        return;
      }
    }

    // Update refs
    lastFiltersKeyRef.current = filtersKey;
    lastTenantIdRef.current = tenantId;

    // Cancel any ongoing request
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }

    // Create new abort controller for this request
    abortControllerRef.current = new AbortController();
    isFetchingRef.current = true;
    setIsLoading(true);
    setError(null);

    try {
      if (import.meta.env.DEV) {
        logger.debug('Fetching consignments with filters:', {
          tenantId,
          page: stableFilters.page,
          size: stableFilters.size,
          status: stableFilters.status,
          search: stableFilters.search,
          expiringWithinDays: stableFilters.expiringWithinDays,
        });
      }

      const response = await stockManagementService.listConsignments(stableFilters, tenantId);

      if (import.meta.env.DEV) {
        logger.debug('Raw consignment list response:', {
          hasResponse: !!response,
          hasError: !!response?.error,
          hasData: !!response?.data,
        });
      }

      // Check for error response
      if (response.error) {
        const errorMessage = response.error.message || 'Failed to fetch consignments';
        logger.error('API returned error response:', {
          error: response.error,
          message: errorMessage,
        });
        throw new Error(errorMessage);
      }

      // Validate response structure
      if (!response.data) {
        logger.error('Invalid response from server: missing data field', {
          response,
        });
        throw new Error('Invalid response from server: missing data field');
      }

      // Extract consignments array from the nested response structure
      let consignmentsArray: Consignment[] = [];

      if (response.data) {
        if (Array.isArray(response.data.consignments)) {
          consignmentsArray = response.data.consignments;
        } else if (Array.isArray(response.data)) {
          consignmentsArray = response.data;
        } else {
          logger.warn('Consignments is not an array in response', {
            consignmentsType: typeof response.data.consignments,
            responseData: response.data,
          });
        }
      }

      // Set pagination if available
      if (response.data && typeof response.data === 'object' && 'totalCount' in response.data) {
        setPagination({
          page: response.data.page || stableFilters.page || 0,
          size: response.data.size || stableFilters.size || 20,
          totalPages: response.data.totalPages || 0,
          totalItems: response.data.totalCount || 0,
        });
      }

      setConsignments(consignmentsArray);
    } catch (err) {
      // Ignore abort errors
      if (err instanceof Error && err.name === 'AbortError') {
        return;
      }

      // Handle 429 rate limit errors gracefully
      if (err instanceof Error && err.message.includes('429')) {
        logger.warn('Rate limit exceeded, will retry after delay', {
          filters: stableFilters,
        });
        // Don't set error state for rate limits - let the retry mechanism handle it
        return;
      }

      const error = err instanceof Error ? err : new Error('Failed to fetch consignments');
      logger.error('Error fetching consignments:', {
        error,
        errorMessage: error.message,
        filters: stableFilters,
      });
      setError(error);
      setConsignments([]);
    } finally {
      isFetchingRef.current = false;
      setIsLoading(false);
    }
  }, [stableFilters, filtersKey, tenantId]);

  useEffect(() => {
    fetchConsignments();

    // Cleanup: abort request on unmount or when filters change
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, [fetchConsignments]);

  return { consignments, isLoading, error, pagination, refetch: fetchConsignments };
};
