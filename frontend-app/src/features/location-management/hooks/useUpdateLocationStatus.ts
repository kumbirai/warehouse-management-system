import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { locationService } from '../services/locationService';
import { locationStatusService } from '../services/locationStatusService';
import { Location, LocationStatus, UpdateLocationStatusRequest } from '../types/location';
import { logger } from '../../../utils/logger';
import { useToast } from '../../../hooks/useToast';

export interface UseUpdateLocationStatusResult {
  updateStatus: (
    locationId: string,
    status: LocationStatus,
    reason?: string,
    tenantId?: string
  ) => Promise<void>;
  isLoading: boolean;
  error: Error | null;
}

export const useUpdateLocationStatus = (): UseUpdateLocationStatusResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const queryClient = useQueryClient();
  const { success, error: showErrorToast } = useToast();

  const updateStatus = async (
    locationId: string,
    status: LocationStatus,
    reason?: string,
    tenantId?: string
  ) => {
    if (!tenantId) {
      throw new Error('Tenant ID is required');
    }

    setIsLoading(true);
    setError(null);

    // Optimistically update the location in cache
    const queryKey = ['location', locationId, tenantId];
    const previousLocation = queryClient.getQueryData<{ data?: Location }>(queryKey);

    if (previousLocation?.data) {
      queryClient.setQueryData(queryKey, {
        ...previousLocation,
        data: {
          ...previousLocation.data,
          status,
          lastModifiedAt: new Date().toISOString(),
        },
      });
    }

    try {
      // Use dedicated block endpoint when blocking (requires reason)
      if (status === 'BLOCKED') {
        if (!reason || !reason.trim()) {
          throw new Error('Reason is required for blocking a location');
        }
        const response = await locationStatusService.blockLocation(
          locationId,
          reason.trim(),
          tenantId
        );
        if (response.error) {
          throw new Error(response.error.message || 'Failed to block location');
        }
        // Update cache with server response
        if (response.data) {
          queryClient.setQueryData(queryKey, {
            ...previousLocation,
            data: response.data as unknown as Location,
          });
        }
        logger.info('Location blocked successfully', { locationId, reason });
        success('Location blocked successfully');
        return;
      }

      // Use general updateStatus endpoint for other status changes
      const request: UpdateLocationStatusRequest = { status, reason };
      const response = await locationService.updateLocationStatus(locationId, request, tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to update location status');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      // Update cache with server response
      queryClient.setQueryData(queryKey, {
        ...previousLocation,
        data: response.data,
      });

      logger.info('Location status updated successfully', { locationId, status });
      success('Location status updated successfully');
    } catch (err) {
      // Rollback optimistic update on error
      if (previousLocation) {
        queryClient.setQueryData(queryKey, previousLocation);
      }
      // Invalidate queries to ensure consistency
      queryClient.invalidateQueries({ queryKey: ['location', locationId] });
      queryClient.invalidateQueries({ queryKey: ['locations'] });

      const error = err instanceof Error ? err : new Error('Failed to update location status');
      logger.error('Error updating location status:', error);
      setError(error);
      showErrorToast(error.message);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { updateStatus, isLoading, error };
};
