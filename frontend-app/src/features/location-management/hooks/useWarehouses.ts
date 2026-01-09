import { useQuery } from '@tanstack/react-query';
import { locationService } from '../services/locationService';
import { LocationHierarchyQueryResult } from '../types/location';

export const useWarehouses = (tenantId: string | null | undefined) => {
  return useQuery<LocationHierarchyQueryResult, Error>({
    queryKey: ['warehouses', tenantId],
    queryFn: async () => {
      if (!tenantId) {
        throw new Error('Tenant ID is required');
      }
      const response = await locationService.listWarehouses(tenantId);
      if (response.error) {
        throw new Error(response.error.message || 'Failed to load warehouses');
      }
      return response.data;
    },
    enabled: !!tenantId,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
};
