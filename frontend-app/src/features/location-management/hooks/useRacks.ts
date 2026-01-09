import { useQuery } from '@tanstack/react-query';
import { locationService } from '../services/locationService';
import { LocationHierarchyQueryResult } from '../types/location';

export const useRacks = (
  aisleId: string | null | undefined,
  tenantId: string | null | undefined
) => {
  return useQuery<LocationHierarchyQueryResult, Error>({
    queryKey: ['racks', aisleId, tenantId],
    queryFn: async () => {
      if (!aisleId || !tenantId) {
        throw new Error('Aisle ID and Tenant ID are required');
      }
      const response = await locationService.listRacks(aisleId, tenantId);
      if (response.error) {
        throw new Error(response.error.message || 'Failed to load racks');
      }
      return response.data;
    },
    enabled: !!aisleId && !!tenantId,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
};
