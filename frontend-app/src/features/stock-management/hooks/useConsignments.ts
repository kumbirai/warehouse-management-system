import { useCallback, useEffect, useRef, useState } from 'react';
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

  const fetchConsignments = useCallback(async () => {
    // Prevent concurrent fetches
    if (isFetchingRef.current) {
      return;
    }

    if (!tenantId) {
      if (import.meta.env.DEV) {
        logger.warn('Cannot fetch consignments: tenantId is missing', {
          filters,
          hasTenantId: !!tenantId,
        });
      }
      setIsLoading(false);
      setConsignments([]);
      return;
    }

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
          page: filters.page,
          size: filters.size,
          status: filters.status,
          search: filters.search,
        });
      }

      const response = await stockManagementService.listConsignments(filters, tenantId);

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
          page: response.data.page || filters.page || 0,
          size: response.data.size || filters.size || 20,
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

      const error = err instanceof Error ? err : new Error('Failed to fetch consignments');
      logger.error('Error fetching consignments:', {
        error,
        errorMessage: error.message,
        filters,
      });
      setError(error);
      setConsignments([]);
    } finally {
      isFetchingRef.current = false;
      setIsLoading(false);
    }
  }, [filters, tenantId]);

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
