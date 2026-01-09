import { useQuery } from '@tanstack/react-query';
import { locationService } from '../services/locationService';
import { LocationHierarchyQueryResult } from '../types/location';

export const useAisles = (zoneId: string | null | undefined, tenantId: string | null | undefined) => {
  return useQuery<LocationHierarchyQueryResult, Error>({
    queryKey: ['aisles', zoneId, tenantId],
    queryFn: async () => {
      if (!zoneId || !tenantId) {
        throw new Error('Zone ID and Tenant ID are required');
      }
      const response = await locationService.listAisles(zoneId, tenantId);
      if (response.error) {
        throw new Error(response.error.message || 'Failed to load aisles');
      }
      return response.data;
    },
    enabled: !!zoneId && !!tenantId,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
};
