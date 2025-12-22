import { useCallback, useEffect, useRef, useState } from 'react';
import { locationService } from '../services/locationService';
import { Location, LocationListFilters } from '../types/location';
import { logger } from '../../../utils/logger';

export interface UseLocationsResult {
  locations: Location[];
  isLoading: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
}

export const useLocations = (filters: LocationListFilters): UseLocationsResult => {
  const [locations, setLocations] = useState<Location[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);
  const isFetchingRef = useRef(false);

  const fetchLocations = useCallback(async () => {
    // Prevent concurrent fetches
    if (isFetchingRef.current) {
      return;
    }

    if (!filters.tenantId) {
      if (import.meta.env.DEV) {
        logger.warn('Cannot fetch locations: tenantId is missing', {
          filters,
          hasTenantId: !!filters.tenantId,
        });
      }
      setIsLoading(false);
      setLocations([]);
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
        logger.debug('Fetching locations with filters:', {
          tenantId: filters.tenantId,
          page: filters.page,
          size: filters.size,
          status: filters.status,
          zone: filters.zone,
          search: filters.search,
        });
      }

      const response = await locationService.listLocations(filters);

      // Don't check for abort here - the request already completed successfully
      // Even if cleanup aborted the signal, we should process the response
      // The abort signal is only for canceling in-flight requests, not for ignoring completed ones

      if (import.meta.env.DEV) {
        logger.debug('Raw location list response:', {
          hasResponse: !!response,
          hasError: !!response?.error,
          hasData: !!response?.data,
          responseKeys: response ? Object.keys(response) : [],
          dataKeys: response?.data ? Object.keys(response.data) : [],
          dataType: response?.data ? typeof response.data : 'undefined',
          responseString: JSON.stringify(response, null, 2).substring(0, 1000), // Log first 1000 chars for debugging
        });
      }

      // Check for error response
      if (response.error) {
        const errorMessage = response.error.message || 'Failed to fetch locations';
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
          responseType: typeof response,
          responseKeys: response ? Object.keys(response) : [],
          responseString: JSON.stringify(response, null, 2),
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

      // Extract locations array from the nested response structure
      // Backend returns ApiResponse<LocationListQueryResult> where LocationListQueryResult has a 'locations' property
      // Handle both direct array and nested structure
      let locationsArray: Location[] = [];

      if (response.data) {
        if (Array.isArray(response.data.locations)) {
          locationsArray = response.data.locations;
        } else if (Array.isArray(response.data)) {
          // Handle case where data is directly an array (shouldn't happen but be defensive)
          locationsArray = response.data;
        } else {
          logger.warn('Locations is not an array in response', {
            locationsType: typeof response.data.locations,
            locationsValue: response.data.locations,
            responseData: response.data,
            responseDataKeys: response.data ? Object.keys(response.data) : [],
          });
        }
      }

      if (import.meta.env.DEV) {
        logger.debug('Parsed locations from response', {
          locationsCount: locationsArray.length,
          totalCount: response.data?.totalCount ?? 'N/A',
          page: response.data?.page ?? 'N/A',
          size: response.data?.size ?? 'N/A',
          locationsIsArray: Array.isArray(response.data?.locations),
          locationsType: typeof response.data?.locations,
          firstLocation: locationsArray.length > 0 ? locationsArray[0] : null,
          responseDataStructure: response.data ? Object.keys(response.data) : [],
        });
      }

      // Validate that we have locations or log a warning
      if (
        locationsArray.length === 0 &&
        response.data?.totalCount &&
        response.data.totalCount > 0
      ) {
        logger.warn('Total count indicates locations exist but array is empty', {
          totalCount: response.data.totalCount,
          responseData: response.data,
        });
      }

      // Always set locations even if aborted - the request completed successfully
      // The abort signal is just for cleanup, not for preventing state updates
      setLocations(locationsArray);
    } catch (err) {
      // Ignore abort errors
      if (err instanceof Error && err.name === 'AbortError') {
        return;
      }

      const error = err instanceof Error ? err : new Error('Failed to fetch locations');
      logger.error('Error fetching locations:', {
        error,
        errorMessage: error.message,
        errorStack: error.stack,
        errorName: error.name,
        filters,
        errorDetails:
          err instanceof Error
            ? {
                name: err.name,
                message: err.message,
                stack: err.stack,
              }
            : String(err),
      });
      setError(error);
      setLocations([]); // Reset locations on error
    } finally {
      isFetchingRef.current = false;
      setIsLoading(false);
    }
  }, [filters]);

  useEffect(() => {
    fetchLocations();

    // Cleanup: abort request on unmount or when filters change
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, [fetchLocations]);

  return { locations, isLoading, error, refetch: fetchLocations };
};
