import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { pickingApiService } from '../services/pickingApiService';
import { ListPickingListsFilters, ListPickingListsQueryResult } from '../types/pickingTypes';
import { useAuth } from '../../../hooks/useAuth';
import { logger } from '../../../utils/logger';

export interface UsePickingListsResult {
  pickingLists: ListPickingListsQueryResult | null;
  isLoading: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
}

/**
 * Creates a stable string representation of filters for comparison.
 * This prevents unnecessary re-renders when filter objects are recreated with the same values.
 */
const getFiltersKey = (filters: ListPickingListsFilters): string => {
  return JSON.stringify({
    page: filters.page ?? 0,
    size: filters.size ?? 20,
    status: filters.status ?? '',
  });
};

export const usePickingLists = (filters: ListPickingListsFilters = {}): UsePickingListsResult => {
  const [pickingLists, setPickingLists] = useState<ListPickingListsQueryResult | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const { user } = useAuth();

  // Track if a request is in progress to prevent duplicate requests
  const isRequestInProgress = useRef(false);
  // Track abort controller for request cancellation
  const abortControllerRef = useRef<AbortController | null>(null);
  // Track the last filters key to detect actual changes
  const lastFiltersKeyRef = useRef<string>('');
  // Track the last tenant ID to detect changes
  const lastTenantIdRef = useRef<string | undefined>(undefined);
  // Track if we've ever successfully fetched (to ensure initial fetch happens)
  const hasFetchedRef = useRef(false);
  // Store current filters in a ref to avoid dependency issues
  const filtersRef = useRef<ListPickingListsFilters>(filters);
  // Normalize tenantId: convert null to undefined for consistency
  const tenantIdRef = useRef<string | undefined>(user?.tenantId ?? undefined);

  // Update refs when values change
  filtersRef.current = filters;
  tenantIdRef.current = user?.tenantId ?? undefined;

  // Create stable filters key for dependency tracking
  const filtersKey = useMemo(() => getFiltersKey(filters), [filters]);

  const fetchPickingLists = useCallback(async () => {
    // Prevent duplicate requests
    if (isRequestInProgress.current) {
      logger.debug('Request already in progress, skipping duplicate request');
      return;
    }

    const currentTenantId = tenantIdRef.current;
    if (!currentTenantId) {
      setError(new Error('Tenant ID is required'));
      setIsLoading(false);
      return;
    }

    // Cancel any pending request
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }

    // Create new abort controller for this request
    const abortController = new AbortController();
    abortControllerRef.current = abortController;

    isRequestInProgress.current = true;
    setIsLoading(true);
    setError(null);

    try {
      // Use current filters from ref
      const response = await pickingApiService.listPickingLists(
        filtersRef.current,
        currentTenantId
      );

      // Check if request was aborted
      if (abortController.signal.aborted) {
        logger.debug('Request was aborted, ignoring response');
        return;
      }

      if (response.error) {
        throw new Error(response.error.message || 'Failed to fetch picking lists');
      }

      // Validate response structure
      if (!response.data) {
        logger.error('Invalid response from server: missing data field', {
          response,
          responseType: typeof response,
          responseKeys: response ? Object.keys(response) : [],
        });
        throw new Error('Invalid response from server: missing data field');
      }

      // Additional validation: ensure data has expected structure
      if (typeof response.data !== 'object' || response.data === null) {
        logger.error('Invalid response data type: expected object', {
          dataType: typeof response.data,
          dataValue: response.data,
        });
        throw new Error('Invalid response from server: data is not an object');
      }

      // Validate that data has the expected pickingLists property
      if (!response.data.pickingLists) {
        logger.error('Invalid response structure: missing pickingLists property', {
          dataKeys: Object.keys(response.data),
          data: response.data,
        });
        throw new Error('Invalid response from server: missing pickingLists property');
      }

      // Log response structure for debugging
      if (import.meta.env.DEV) {
        logger.debug('Parsed picking lists from response', {
          pickingListsCount: Array.isArray(response.data.pickingLists)
            ? response.data.pickingLists.length
            : 'N/A',
          totalElements: response.data.totalElements ?? 'N/A',
          page: response.data.page ?? 'N/A',
          size: response.data.size ?? 'N/A',
          pickingListsIsArray: Array.isArray(response.data.pickingLists),
          pickingListsType: typeof response.data.pickingLists,
          firstPickingList:
            Array.isArray(response.data.pickingLists) && response.data.pickingLists.length > 0
              ? response.data.pickingLists[0]
              : null,
          responseDataStructure: response.data ? Object.keys(response.data) : [],
        });
      }

      setPickingLists(response.data);
      setError(null); // Clear any previous errors on success
      hasFetchedRef.current = true; // Mark that we've successfully fetched
    } catch (err) {
      // Don't set error if request was aborted
      if (abortController.signal.aborted) {
        logger.debug('Request was aborted, not setting error');
        return;
      }

      const error = err instanceof Error ? err : new Error('Failed to fetch picking lists');
      logger.error('Error fetching picking lists:', error);
      setError(error);
    } finally {
      isRequestInProgress.current = false;
      setIsLoading(false);
      // Clear abort controller reference if this is the current request
      if (abortControllerRef.current === abortController) {
        abortControllerRef.current = null;
      }
    }
  }, []); // No dependencies - uses refs for current values

  useEffect(() => {
    // Check if this is the initial load (we haven't fetched yet)
    const isInitialLoad = !hasFetchedRef.current;

    // Only fetch if filters or tenantId actually changed (avoid infinite loops from object reference changes)
    // OR if this is the initial load
    const filtersChanged = lastFiltersKeyRef.current !== filtersKey;
    const tenantIdChanged = lastTenantIdRef.current !== user?.tenantId;

    if (import.meta.env.DEV) {
      logger.debug('usePickingLists useEffect triggered', {
        isInitialLoad,
        hasFetched: hasFetchedRef.current,
        filtersChanged,
        tenantIdChanged,
        lastFiltersKey: lastFiltersKeyRef.current,
        currentFiltersKey: filtersKey,
        lastTenantId: lastTenantIdRef.current,
        currentTenantId: user?.tenantId,
      });
    }

    // Skip if nothing changed and we've already fetched
    if (!isInitialLoad && !filtersChanged && !tenantIdChanged) {
      logger.debug('Filters and tenantId unchanged, skipping fetch');
      return;
    }

    // On initial load or when tenantId becomes available, we need a tenantId to fetch
    if (!user?.tenantId) {
      logger.debug('Waiting for tenantId before fetching', {
        isInitialLoad,
        hasTenantId: !!user?.tenantId,
      });
      // Update tenantId ref even if we're not fetching, so we can detect when it becomes available
      lastTenantIdRef.current = undefined;
      return;
    }

    // Update refs to track current state BEFORE fetching
    lastFiltersKeyRef.current = filtersKey;
    lastTenantIdRef.current = user?.tenantId ?? undefined;

    // Cancel any pending request
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }

    // Reset request in progress flag
    isRequestInProgress.current = false;

    // Fetch picking lists
    fetchPickingLists();

    // Cleanup function
    return () => {
      // Cancel any pending request on unmount or dependency change
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
        abortControllerRef.current = null;
      }
      isRequestInProgress.current = false;
    };
  }, [filtersKey, user?.tenantId, fetchPickingLists]);

  // Debug: Track when pickingLists state changes
  useEffect(() => {
    if (import.meta.env.DEV) {
      logger.debug('pickingLists state changed', {
        pickingLists,
        pickingListsIsNull: pickingLists === null,
        pickingListsArray: pickingLists?.pickingLists,
        pickingListsCount: pickingLists?.pickingLists?.length,
      });
    }
  }, [pickingLists]);

  return {
    pickingLists,
    isLoading,
    error,
    refetch: fetchPickingLists,
  };
};
