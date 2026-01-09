import { useQuery } from '@tanstack/react-query';
import { locationService } from '../services/locationService';
import { LocationHierarchyQueryResult } from '../types/location';

export const useBins = (rackId: string | null | undefined, tenantId: string | null | undefined) => {
  return useQuery<LocationHierarchyQueryResult, Error>({
    queryKey: ['bins', rackId, tenantId],
    queryFn: async () => {
      if (!rackId || !tenantId) {
        throw new Error('Rack ID and Tenant ID are required');
      }
      const response = await locationService.listBins(rackId, tenantId);
      if (response.error) {
        throw new Error(response.error.message || 'Failed to load bins');
      }
      return response.data;
    },
    enabled: !!rackId && !!tenantId,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
};
