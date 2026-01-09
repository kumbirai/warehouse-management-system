import { useQuery } from '@tanstack/react-query';
import { locationService } from '../services/locationService';
import { LocationHierarchyQueryResult } from '../types/location';

export const useZones = (
  warehouseId: string | null | undefined,
  tenantId: string | null | undefined
) => {
  return useQuery<LocationHierarchyQueryResult, Error>({
    queryKey: ['zones', warehouseId, tenantId],
    queryFn: async () => {
      if (!warehouseId || !tenantId) {
        throw new Error('Warehouse ID and Tenant ID are required');
      }
      const response = await locationService.listZones(warehouseId, tenantId);
      if (response.error) {
        throw new Error(response.error.message || 'Failed to load zones');
      }
      return response.data;
    },
    enabled: !!warehouseId && !!tenantId,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
};
